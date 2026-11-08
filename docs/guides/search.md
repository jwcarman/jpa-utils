# Search

## Marking fields

Annotate the `String` fields that free-text search should match (or their getters, for an entity that uses
property access):

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseEntity {

    @Searchable
    private String title;

    @Searchable
    private String summary;

    private String body;   // not searched
}
```

Only `String` attributes are searched; `@Searchable` on any other type is ignored.

## Searching from a repository

`SearchableRepository` adds `search(String term, Pageable pageable)` to a Spring Data repository:

```java
public interface ArticleRepository extends SearchableRepository<Article, UUID> {
}
```

```java
private static final Paginator ARTICLES = Paginator.of(Article.class, ArticleSort.class);

public PageDto<ArticleDto> search(String term, PageSpec spec) {
    return ARTICLES.paginate(spec, pageable -> articles.search(term, pageable), ArticleDto::of);
}
```

## What a search matches

The term is split into words on whitespace, and a row matches when **every word** appears in **at least one**
searchable field, in any order. For `john smith` against `firstName` and `lastName`:

```sql
WHERE (LOWER(first_name) LIKE '%john%' ESCAPE '\' OR LOWER(last_name) LIKE '%john%' ESCAPE '\')
  AND (LOWER(first_name) LIKE '%smith%' ESCAPE '\' OR LOWER(last_name) LIKE '%smith%' ESCAPE '\')
```

So `john smith` and `smith john` both find John Smith, even though the words are in different columns, and
`john brown` finds nobody unless someone matches both words.

- **Case-insensitive**: both sides are lowercased.
- **Substring**: each word may appear anywhere in a field.
- **Literal**: `%`, `_` and `\` in a word are escaped, so `50%` matches the text "50%", not "50 and anything".
- **Bounded**: duplicate words count once, and only the first 10 words are used (`Searchables.MAX_SEARCH_WORDS`),
  so a pasted paragraph can't build an enormous query.
- **Absent terms match everything**: a `null` or blank term adds no condition.
- **No searchable fields matches everything** too, and logs a warning once for that entity type.

The words are bound as parameters, never concatenated into SQL.

## Combining search with other conditions

`Searchables.createSearchPredicate(term, root, builder)` returns the same predicate for use in your own
Specifications:

```java
public Page<Article> searchPublished(String term, Pageable pageable) {
    Specification<Article> published = (root, query, cb) -> cb.isTrue(root.get("published"));
    Specification<Article> matches = (root, query, cb) -> Searchables.createSearchPredicate(term, root, cb);
    return articles.findAll(published.and(matches), pageable);
}
```

The predicate works with plain Jakarta Persistence Criteria queries as well; it needs no Spring.

## Limits

A leading-wildcard `LIKE` can't use an ordinary B-tree index, so every search scans the searchable columns, with one
`LIKE` per word per searchable field. That is
fine for modest tables. For large ones, consider a database full-text index (PostgreSQL `tsvector`, MySQL
`FULLTEXT`) behind a custom query.

Bound the length of search terms at the controller, since a very long pattern is wasted work for the database:

```java
@GetMapping
public PageDto<ArticleDto> search(
        @RequestParam(required = false) @Size(max = 200) String query,
        PageParams page) {
    return articles.search(query, page);
}
```

Around 100 to 200 characters covers ordinary searches; never leave it unbounded.
