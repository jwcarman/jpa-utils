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

For the term `spring boot`, the query matches rows where **any** searchable field contains the term:

```sql
WHERE LOWER(title) LIKE '%spring boot%' ESCAPE '\'
   OR LOWER(summary) LIKE '%spring boot%' ESCAPE '\'
```

- **Case-insensitive**: both sides are lowercased.
- **Substring**: the term may appear anywhere in the field.
- **Literal**: `%`, `_` and `\` in the term are escaped, so `50%` matches the text "50%", not "50 and anything".
- **Absent terms match everything**: a `null` or blank term adds no condition.
- **No searchable fields matches everything** too, and logs a warning once for that entity type.

The term is bound as a parameter, never concatenated into SQL.

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

A leading-wildcard `LIKE` can't use an ordinary B-tree index, so every search scans the searchable columns. That is
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
