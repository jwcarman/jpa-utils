# Searchable match modes — design

**Status:** approved 2026-11-23

## Why

Every `@Searchable` field is matched with `LOWER(col) LIKE '%word%'`. A leading wildcard defeats ordinary B-tree
indexes on most databases, so every search scans the table. Letting a field opt into prefix or exact matching gives
the database a chance to use an index.

## Goals

- A per-field match mode on `@Searchable`: `CONTAINS`, `PREFIX`, `EXACT`.
- Full backward compatibility: `CONTAINS` is the default, so existing entities behave exactly as today.

## Non-goals (this round)

- Opting a field out of lowercasing (planned as a later, separate change).
- Searchable types other than `String`.
- Matching the whole search term instead of word by word.
- Choosing the mode per query.

## Approaches considered

- **A. `match` attribute on `@Searchable`** — chosen. One annotation, backward compatible, and leaves room for a
  later lowercasing attribute.
- B. One annotation per mode (`@SearchablePrefix`, `@SearchableExact`) — more surface; a future lowercasing option
  would have to be repeated on each.
- C. Mode chosen per query — doesn't fit: indexability is a property of the field, not the call.

## Design

- New enum `org.jwcarman.jpa.search.MatchMode { CONTAINS, PREFIX, EXACT }`.
- `@Searchable` gains `MatchMode match() default MatchMode.CONTAINS`.
- Word-by-word semantics are unchanged: every word of the term must match at least one searchable field, each field
  using its own mode. `"smith oh"` matches `lastName` (PREFIX) plus `state` (EXACT).
- SQL per mode:
  - `CONTAINS` — `LOWER(col) LIKE '%word%' ESCAPE '\'` (unchanged)
  - `PREFIX` — `LOWER(col) LIKE 'word%' ESCAPE '\'`
  - `EXACT` — `LOWER(col) = 'word'` (equality, so no wildcard escaping; `%` and `_` compare literally)
- The per-entity-class cache holds attribute name **and** mode, not just the name.

## Known limitation

`EXACT` matches a single word. Because the term is split on whitespace, `"new york"` never equals the value
`"New York"`. `EXACT` suits codes (`OH`); multi-word values should use `PREFIX` or `CONTAINS`.

## Tests

- Unit tests per mode in `SearchablesTest`.
- Repository tests with an entity mixing modes.
- `DatabaseIT` coverage so every supported database exercises each mode.

## Docs

- `docs/guides/search.md`: match modes section, plus indexing guidance — a `LOWER(col)` functional index; on Postgres,
  `PREFIX` additionally needs `text_pattern_ops` (or the C collation) to use the index.
- Fix the stale "OR logic" text and the mislabelled getter example in the `@Searchable` javadoc.
- CHANGELOG entry.

## Builder's calls (made during design)

- Enum named `MatchMode`; attribute named `match`.
- `EXACT` uses `=` rather than a wildcard-free `LIKE`.
