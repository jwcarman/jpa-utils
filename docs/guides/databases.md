# Databases and Performance

## Tested databases

Every build runs the integration suite against real databases in containers
([Testcontainers](https://testcontainers.com/)):

| Database | Version tested |
|----------|----------------|
| PostgreSQL | 17.6 |
| MySQL | 9.2 |
| MariaDB | 11.6 |
| CockroachDB | 25.3 |
| Oracle Database Free | 23 |

The suite covers paging, sorting, search with escaped wildcards, stable paging over tied sort values, a
configured tiebreaker, and lazy-proxy equality. The unit tests use H2.

## Query plan caching

Search terms are bound as parameters, so every search against an entity has the same SQL text and reuses one
cached plan in Hibernate and in the database:

```sql
WHERE LOWER(first_name) LIKE ? ESCAPE '\' OR LOWER(last_name) LIKE ? ESCAPE '\'
```

The set of searchable fields is found by reflection once per entity type and cached.

Hibernate's default literal handling binds strings and inlines numbers. If your own Criteria queries add numeric
or date conditions, binding every literal keeps their plans shared too:

```properties
spring.jpa.properties.hibernate.criteria.literal_handling_mode=BIND
```

## Indexes for listings

A listing sorted by `x` with the `id` tiebreaker orders by `(x, id)`. For large tables an index on
`(x, id)` serves the whole `ORDER BY`, and lets the database stop after the requested page instead of sorting
every row.

Deep `OFFSET`s still cost the rows they skip. For very large result sets that users page deep into, keyset
("seek") pagination is faster; the tiebreaker already gives the total order it needs.
