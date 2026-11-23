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

## Choosing a match mode

Each searchable field matches in one of three ways, set with `match`:

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    @Searchable(match = MatchMode.PREFIX)
    private String lastName;

    @Searchable(match = MatchMode.EXACT)
    private String state;
}
```

| Mode | A word matches when | SQL |
|------|---------------------|-----|
| `CONTAINS` (default) | it appears anywhere in the field | `LOWER(col) LIKE '%word%' ESCAPE '\'` |
| `PREFIX` | the field starts with it | `LOWER(col) LIKE 'word%' ESCAPE '\'` |
| `EXACT` | the field equals it | `LOWER(col) = 'word'` |

All three are case-insensitive, so `oh` finds `OH`. A bare `@Searchable` is `CONTAINS`.

The reason to choose `PREFIX` or `EXACT` is speed: unlike `CONTAINS`, they can use an index on databases that
support it (see [Limits](#limits)). `EXACT` suits codes such as a state or a status. Because the term is split into words, `EXACT`
can't match a value that contains a space: searching `new york` never matches a field holding "New York". Use
`PREFIX` or `CONTAINS` for fields like that.

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
`john brown` finds nobody unless someone matches both words. Each field uses its own match mode, so with `lastName`
as `PREFIX` and `state` as `EXACT`, `smi oh` finds Smiths in Ohio.

- **Case-insensitive**: both sides are lowercased; the words in Java with `Locale.ROOT`, the column by the
  database's `LOWER()`. For some non-English letters (Turkish `İ`) the two can differ, and a word then doesn't
  match.
- **Per field**: each word is matched against each field by that field's mode: anywhere (`CONTAINS`), at the
  start (`PREFIX`), or the whole value (`EXACT`).
- **Literal**: `%`, `_` and `\` in a word are escaped for `CONTAINS` and `PREFIX`, so `50%` matches the text "50%",
  not "50 and anything". `EXACT` compares with `=`, where they have no special meaning.
- **Words**: words are separated by whitespace, Unicode spaces (a non-breaking space pasted from a web page),
  control characters (tab, NUL) and invisible format characters (zero-width space, byte-order mark). None of these
  reach the database, so a stray NUL can't make PostgreSQL reject the query. A term made only of them is blank.
- **Bounded**: duplicate words count once, only the first 10 words are used (`Searchables.MAX_SEARCH_WORDS`), and
  each word is cut to 100 characters (`Searchables.MAX_WORD_LENGTH`), so a pasted paragraph can't build an enormous
  query.
- **Absent terms match everything**: a `null` or blank term adds no condition.
- **No searchable fields matches everything** too, and logs a warning once for that entity type.

The words are bound as parameters, never concatenated into SQL. That relies on Hibernate's default
`hibernate.criteria.value_handling_mode=BIND`; don't set it to `INLINE` in an application that searches browser
input, because Hibernate then writes the words into the SQL as quoted literals.

## Combining search with other conditions

`searchFor(term)` on a `SearchableRepository` is the same search as a `Specification`, so it composes with your
own:

```java
public PageDto<ArticleDto> searchPublished(String term, PageSpec spec) {
    return ARTICLES.paginate(spec,
            pageable -> articles.findAll(articles.searchFor(term).and(isPublished()), pageable),
            ArticleDto::of);
}

private static Specification<Article> isPublished() {
    return (root, query, cb) -> cb.isTrue(root.get("published"));
}
```

Without Spring, `Searchables.createSearchPredicate(term, root, builder)` returns the same condition as a Jakarta
Persistence Criteria `Predicate`, for your own Criteria queries.

## Limits

A `CONTAINS` field's leading-wildcard `LIKE` can't use an ordinary B-tree index, so a search over `CONTAINS` fields
scans the table, with one comparison per word per searchable field. That is fine for modest tables.

`PREFIX` and `EXACT` fields can use an index on the lowercased column:

```sql
-- EXACT: any database with expression indexes
CREATE INDEX customer_state_lower ON customer (LOWER(state));

-- PREFIX on PostgreSQL: the index must use text_pattern_ops (or the C collation),
-- or a prefix LIKE can't use it
CREATE INDEX customer_last_name_lower ON customer (LOWER(last_name) text_pattern_ops);
```

Support differs by database. These results come from `EXPLAIN` on a 20,000-row table:

| Database | `EXACT` | `PREFIX` |
|----------|---------|----------|
| PostgreSQL | index on `LOWER(col)` | index on `LOWER(col) text_pattern_ops` (verified by the test suite) |
| MySQL 8+ | functional index `((LOWER(col)))` | **scans**: MySQL uses functional indexes only for `=`, `<`, `>`, `BETWEEN` and `IN`, not `LIKE` |
| MariaDB 11.6 | **scans** | **scans**: MariaDB has no expression indexes, and it doesn't match `LOWER(col)` to an indexed virtual column |

Oracle and CockroachDB support expression indexes on `LOWER(col)`; check your plans with `EXPLAIN`.

**Indexes help only when every `@Searchable` field on the entity is `PREFIX` or `EXACT`, and indexed.** For each
word the fields are ORed together, and a database can use indexes for an `OR` only when it can use one for every
branch. A single `CONTAINS` field makes the whole search scan the table.

For large tables with free-text needs, consider a database full-text index (PostgreSQL `tsvector`, MySQL
`FULLTEXT`) behind a custom query.

The library caps a search at 10 words of 100 characters, but still bound the length of the term at the
controller, so an oversized request is rejected before any work is done:

```java
@GetMapping
public PageDto<ArticleDto> search(
        @RequestParam(required = false) @Size(max = 200) String query,
        PageParams page) {
    return articles.search(query, page);
}
```

Around 100 to 200 characters covers ordinary searches; never leave it unbounded.

The rest of the cost is the application's to manage: every search over a `CONTAINS` field scans the table (plus a
count query for each page), a one-letter term matches most rows, and a blank term matches all of them. Always page
search results, and consider a minimum term length or rate limiting for public endpoints.
