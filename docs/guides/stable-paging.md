# Stable Paging

## The problem

Each page of a listing is its own query:

```sql
SELECT ... ORDER BY role LIMIT 20 OFFSET 0    -- page 1
SELECT ... ORDER BY role LIMIT 20 OFFSET 20   -- page 2
```

SQL orders rows only by the columns in `ORDER BY`. Rows with the same `role` (say 200 members who are all
`MEMBER`) can come back in any order, and the database may choose a different order for each query, depending on
the plan, the `LIMIT`, parallel workers or recent updates. When page 1 and page 2 disagree about the order of the
tied rows, a row can appear on both pages, or on neither.

Nothing fails. The listing is just quietly wrong, usually only once there is enough data, in production. This
isn't hypothetical: jpa-utils' integration tests page through 100 rows that tie on the sort column, five at a
time, and without a tiebreaker PostgreSQL, MySQL and CockroachDB each returned duplicate rows across pages.

## The fix: a unique last sort key

Adding a unique column as the final sort key makes the order total. No two rows tie, every query agrees on the
order, and each row lands on exactly one page:

```sql
ORDER BY role ASC, id ASC
```

`Paginator` does this for every `Pageable` it builds: the requested sort, then the tiebreaker, ascending.

| Request | ORDER BY |
|---------|----------|
| `sortBy=ROLE&sortDirection=DESC` | `role DESC, id ASC` |
| `sortBy=NAME` | `name ASC, id ASC` |
| no `sortBy`, no default sort | `id ASC` |
| no `sortBy`, default sort `NAME` | `name ASC, id ASC` |
| `sortBy=ID&sortDirection=DESC` (a constant whose property is `id`) | `id DESC` |

Requests without a sort get the tiebreaker too: an unsorted page is just as unstable. A tiebreaker the request
already sorts by isn't added again.

The tiebreaker's direction makes no difference to stability, so it is always ascending. For `BaseEntity`'s
UUID v7 ids that puts tied rows in creation order.

## Choosing the tiebreaker

The default is `id`, the identifier of [`BaseEntity`](entities.md). Configure another when the entity's unique
property is named differently:

```java
Paginator.of(MemberDirectoryEntry.class, MemberSort.class, cfg -> cfg.tiebreaker("identityId"));
```

For a composite key, list every part; together they must be unique:

```java
Paginator.of(LineItem.class, LineItemSort.class, cfg -> cfg.tiebreaker("orderId", "lineNo"));
```

To turn it off (for a listing whose every sort option is already unique, or that is never paged):

```java
Paginator.of(Event.class, EventSort.class, Paginator.Config::noTiebreaker);
```

The tiebreaker must be unique, or ties remain. A non-unique tiebreaker reduces the problem but doesn't fix it,
and [property validation](property-validation.md) can't tell unique from non-unique.

## Cost

The extra sort key is the primary key (or another unique, usually indexed column), so the database orders by it
cheaply, and only among rows that already tie. A query that sorts by an indexed column can still use that index;
for large tables, an index on `(sort_column, id)` serves both keys at once.
