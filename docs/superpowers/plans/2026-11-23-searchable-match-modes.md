# Searchable Match Modes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let each `@Searchable` field choose `CONTAINS` (default), `PREFIX`, or `EXACT` matching so databases can use indexes.

**Architecture:** A new `MatchMode` enum and a `match` attribute on `@Searchable`. `Searchables` caches each
searchable attribute's name *and* mode per entity class, and builds one comparison per (word, field) according to the
field's mode. Word-by-word semantics are unchanged: AND over words, OR over fields.

**Tech Stack:** Java 25, Jakarta Persistence 3.2 Criteria API, Hibernate 7, Spring Data JPA, JUnit 5 + AssertJ, H2
(`@DataJpaTest`), Testcontainers (`*IT`).

**Spec:** `docs/superpowers/specs/2026-11-23-searchable-match-modes-design.md`

## Global Constraints

- Backward compatible: a bare `@Searchable` behaves exactly as today (`CONTAINS`).
- SQL per mode: `CONTAINS` → `LOWER(col) LIKE '%word%' ESCAPE '\'`; `PREFIX` → `LOWER(col) LIKE 'word%' ESCAPE '\'`;
  `EXACT` → `LOWER(col) = 'word'` (no wildcard escaping).
- Every comparison stays lowercased. No opt-out of lowercasing in this change.
- Enum `org.jwcarman.jpa.search.MatchMode { CONTAINS, PREFIX, EXACT }`; attribute `MatchMode match() default MatchMode.CONTAINS`.
- Project rules: no `@SuppressWarnings`, no star imports, no fully-qualified type names in code.
- Test style: snake_case method names, `@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)`,
  `@Nested` classes per behavior.
- Work on a feature branch (e.g. `feat/searchable-match-modes`), not `main`.

## Review Focus

1. **Wildcards in an `EXACT` word** — `o_` or `o%` must not match `OH`; equality compares them literally. (Task 1, Task 2)
2. **Wildcards in a `PREFIX` word** — `c_` / `c%` must not match `Columbus`; escaping still applies to `PREFIX`. (Task 1)
3. **Mode on a getter (property access)** — the mode must be read from whichever member carries the annotation
   (`Field` *or* `Method`), not only from fields. (Task 1, `Animal.species`)
4. **Mixed modes in one term** — `annex oh` must match name (contains) + state (exact) on the same row, and a word
   matching no field under its mode excludes the row. (Task 1, Task 2)
5. **Case-insensitivity of `EXACT`** — `oh` must find stored `OH`. (Task 1, Task 2)
6. **Hostile browser input** — quotes, SQL fragments, `%`, `_`, `\` must never alter the SQL text or the data; the
   typed words reach the database only as bind parameters. (Task 1, Step 2b)
7. **Index actually used, including after repeated executions** — the reason for the feature. (Task 4)

---

### Task 1: Match modes in `@Searchable` and `Searchables`

**Files:**
- Create: `src/main/java/org/jwcarman/jpa/search/MatchMode.java`
- Modify: `src/main/java/org/jwcarman/jpa/search/Searchable.java`
- Modify: `src/main/java/org/jwcarman/jpa/search/Searchables.java`
- Create: `src/test/java/org/jwcarman/jpa/spring/search/Location.java`
- Create: `src/test/java/org/jwcarman/jpa/spring/search/LocationRepository.java`
- Modify: `src/test/java/org/jwcarman/jpa/spring/search/Animal.java` (add `species`)
- Test: `src/test/java/org/jwcarman/jpa/spring/search/SearchableMatchModeTest.java`

**Interfaces:**
- Produces: `public enum MatchMode { CONTAINS, PREFIX, EXACT }`; `@Searchable(match = MatchMode.X)`.
  `Searchables.createSearchPredicate(String, Root<?>, CriteriaBuilder)` keeps its signature.

- [ ] **Step 1: Create the test entities**

`Location extends BaseEntity` — `@Entity`, `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, no setters,
constructor `Location(String name, String city, String state)`:
- `@Searchable String name` (default mode)
- `@Searchable(match = MatchMode.PREFIX) String city`
- `@Searchable(match = MatchMode.EXACT) String state`

`LocationRepository extends SearchableRepository<Location, UUID>`.

`Animal` (property access, getters annotated): add field `species` with `getSpecies()` annotated
`@Searchable(match = MatchMode.EXACT)` and `setSpecies(String)`, following the existing `name` pattern.

- [ ] **Step 2: Write the failing test**

`SearchableMatchModeTest`: `@DataJpaTest(showSql = false)`, `@ContextConfiguration(classes = SpringBootConfig.class)`,
snake_case + `@Nested`. A `@BeforeEach` saves:

| name | city | state |
|------|------|-------|
| HQ | Columbus | OH |
| Annex | Cleveland | OH |
| Lab | Austin | TX |
| Mart | Albany | New York |

(No name contains the letter `o`; the negative cases below rely on that.)

Helper: `List<String> namesFor(String term)` → `locations.search(term, Pageable.unpaged())` mapped to `getName()`.

```java
@Nested class Contains_is_the_default {
    @Test void matches_anywhere_in_the_field()      { assertThat(namesFor("nne")).containsExactly("Annex"); }
}
@Nested class Prefix {
    @Test void matches_the_start_of_the_field()      { assertThat(namesFor("colum")).containsExactly("HQ"); }
    @Test void ignores_case()                        { assertThat(namesFor("COLUM")).containsExactly("HQ"); }
    @Test void does_not_match_the_middle()           { assertThat(namesFor("umbus")).isEmpty(); }
    @Test void treats_percent_literally()            { assertThat(namesFor("c%")).isEmpty(); }
    @Test void treats_underscore_literally()         { assertThat(namesFor("c_")).isEmpty(); }
}
@Nested class Exact {
    @Test void matches_the_whole_value_ignoring_case() { assertThat(namesFor("oh")).containsExactlyInAnyOrder("HQ", "Annex"); }
    @Test void does_not_match_part_of_the_value()      { assertThat(namesFor("o")).isEmpty(); }
    @Test void treats_underscore_literally()           { assertThat(namesFor("o_")).isEmpty(); }
    @Test void treats_percent_literally()              { assertThat(namesFor("o%")).isEmpty(); }
    @Test void cannot_match_a_multi_word_value()       { assertThat(namesFor("new york")).isEmpty(); }
    @Test void reads_the_mode_from_an_annotated_getter() {
        // Animal: name "Rex", species "Dog"
        assertThat(animals.search("dog", Pageable.unpaged()).getTotalElements()).isEqualTo(1);
        assertThat(animals.search("do",  Pageable.unpaged()).getTotalElements()).isZero();
    }
}
@Nested class Mixed_modes {
    @Test void match_each_word_by_its_own_field_mode() { assertThat(namesFor("annex oh")).containsExactly("Annex"); }
    @Test void combine_prefix_and_exact()              { assertThat(namesFor("colum oh")).containsExactly("HQ"); }
    @Test void exclude_a_row_missing_any_word()        { assertThat(namesFor("austin oh")).isEmpty(); }
}
```

- [ ] **Step 2b: Write the untrusted-input tests**

Create `src/test/java/org/jwcarman/jpa/spring/search/CapturingStatementInspector.java`, which implements Hibernate's
`StatementInspector`. It records each SQL string in a static `CopyOnWriteArrayList<String>` and returns the string
unchanged. Give it `static void clear()` and `static List<String> statements()`. Register it on
`SearchableMatchModeTest` with
`@DataJpaTest(showSql = false, properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.jwcarman.jpa.spring.search.CapturingStatementInspector")`.

Add the following to `SearchableMatchModeTest`:

```java
@Nested class Untrusted_input {
    @ParameterizedTest
    @ValueSource(strings = {"' OR '1'='1", "'; DROP TABLE location; --", "oh' --", "%", "_", "\\", "%%", "o\\h"})
    void matches_nothing_and_changes_nothing(String term) {
        assertThat(namesFor(term)).isEmpty();
        assertThat(locations.count()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"zq'x", "colum", "oh"})
    void never_appears_in_the_sql_text(String term) {
        CapturingStatementInspector.clear();
        namesFor(term);
        assertThat(CapturingStatementInspector.statements())
                .isNotEmpty()
                .noneMatch(sql -> sql.toLowerCase(Locale.ROOT).contains(term));
    }
}
```

The second test pins the guarantee that matters for browser input. Hibernate binds Criteria values as parameters
because `hibernate.criteria.value_handling_mode` defaults to `BIND`. If the generated SQL ever contains the typed text,
this test fails. Add one sentence to the `Searchables` class javadoc stating this reliance. If an application sets the
mode to `INLINE`, Hibernate renders the values as escaped literals instead.

- [ ] **Step 3: Run it to verify it fails**

Run: `mvn test -Dtest=SearchableMatchModeTest`
Expected: compilation FAILURE, `cannot find symbol: class MatchMode`.

- [ ] **Step 4: Add `MatchMode` and the `match` attribute**

`MatchMode`: javadoc on each constant stating its SQL (from Global Constraints) and its index note: `CONTAINS` cannot
use a B-tree index; `PREFIX` can use an index on `LOWER(col)` (on PostgreSQL only with `text_pattern_ops` or the C
collation); `EXACT` can use a plain index on `LOWER(col)` and suits codes; it matches one word, so multi-word values
never match.

`Searchable`: add `MatchMode match() default MatchMode.CONTAINS;` with javadoc. In the same file, fix the class
javadoc: replace "OR Logic: Matches if the search term appears in ANY searchable field" with the word-by-word rule
(every word must match at least one field, using that field's mode); retitle "Using on Getter Methods (with Lombok)"
and make its example annotate getters on a property-access entity (`@Id` on a getter), noting that on a field-access
entity a getter annotation is not seen; add a short match-mode example.

- [ ] **Step 5: Build predicates per mode in `Searchables`**

- Replace the cache value type `List<String>` with `List<SearchableAttribute>`, where
  `private record SearchableAttribute(String name, MatchMode match)`.
- `isSearchable(SingularAttribute)` becomes `Optional<Searchable> searchableOf(SingularAttribute<?, ?>)`, reading the
  annotation from the `Method` or `Field` member (same switch as today). Build `SearchableAttribute(attribute.getName(), searchable.match())`.
- Replace `like(...)` with `private static Predicate matches(Root<?> root, CriteriaBuilder builder, SearchableAttribute attribute, String word)`:
  an exhaustive `switch` on `attribute.match()` producing the three SQL forms from Global Constraints. `CONTAINS` and
  `PREFIX` use `sanitizeSearchTerm(word)` with `'\\'` as the escape char; `EXACT` uses `builder.equal(lower, word)` with
  the raw word.
- Update the class and method javadoc: per-mode SQL example and the "Query Size" line (one comparison per word per field).

- [ ] **Step 6: Run the search tests to verify they pass**

Run: `mvn test -Dtest='SearchableMatchModeTest,SearchableRepositoryTest,SearchablesTest'`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/jwcarman/jpa/search src/test/java/org/jwcarman/jpa/spring/search
git commit -m "feat: per-field match modes for @Searchable (CONTAINS, PREFIX, EXACT)"
```

---

### Task 2: Cover match modes on every supported database

**Files:**
- Modify: `src/test/java/org/jwcarman/jpa/integration/TestPerson.java`
- Modify: `src/test/java/org/jwcarman/jpa/integration/DatabaseIT.java`

**Interfaces:**
- Consumes: `MatchMode`, `@Searchable(match = ...)` from Task 1.

- [ ] **Step 1: Add mode fields to `TestPerson`**

Add `@Searchable(match = MatchMode.PREFIX) String city` and `@Searchable(match = MatchMode.EXACT) String state`.
Keep the existing 3-arg constructor (city/state stay null); add
`TestPerson(String firstName, String lastName, String email, String city, String state)`.

- [ ] **Step 2: Seed and write the failing tests**

In `setUp()`, give three existing rows city/state via the 5-arg constructor: Alice → `Columbus`, `OH`;
Bob → `Cleveland`, `OH`; Charlie → `Austin`, `TX`. Every other row is unchanged. Add a `@Nested class Match_modes`:

```java
@Test void finds_by_city_prefix()                     { assertThat(lastNamesFor("clev")).containsExactly("Brown"); }
@Test void does_not_match_the_middle_of_a_prefix_field() { assertThat(lastNamesFor("eveland")).isEmpty(); }
@Test void finds_by_exact_state_ignoring_case()       { assertThat(lastNamesFor("oh")).containsExactlyInAnyOrder("Anderson", "Brown"); }
@Test void treats_wildcards_in_an_exact_word_literally() { assertThat(lastNamesFor("o_")).isEmpty(); }
@Test void matches_each_word_by_its_field_mode()      { assertThat(lastNamesFor("bob oh")).containsExactly("Brown"); }
@Test void excludes_a_row_missing_any_word()          { assertThat(lastNamesFor("charlie oh")).isEmpty(); }
```

Helper `lastNamesFor(term)` → `personRepository.search(term, Pageable.unpaged())` mapped to `getLastName()`.
JUnit runs `@Nested` classes inside the abstract base for each concrete `*IT` subclass, so every database is covered.

- [ ] **Step 3: Run the integration tests (requires Docker)**

Run: `mvn verify`
Expected: BUILD SUCCESS. The existing `DatabaseIT` tests still pass unchanged on every database, and so do the new
`Match_modes` tests. If a nested class is not picked up for the subclasses, move the six tests to top level in
`DatabaseIT` with a `match_mode_` name prefix.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/org/jwcarman/jpa/integration
git commit -m "test: match modes on every supported database"
```

---

### Task 3: Guide and changelog

**Files:**
- Modify: `docs/guides/search.md`
- Modify: `CHANGELOG.md`
- Check: `docs/index.md`, `README.md`, `AGENTS.md`. Update any sentence that says search is always "contains"/substring.

- [ ] **Step 1: Update `docs/guides/search.md`**

- New section **"Choosing a match mode"** after "Marking fields": the three modes, a table of mode → SQL (from Global
  Constraints), an `Article`/`Person` example with a `PREFIX` and an `EXACT` field, and the single-word limitation of
  `EXACT` with the `"new york"` example.
- In "What a search matches", change the **Substring** bullet so it describes the field's mode, and change the
  **Literal** bullet to say that `PREFIX` and `CONTAINS` escape wildcards and that `EXACT` compares with `=`.
- In "Limits", add indexing guidance:
  ```sql
  -- EXACT, any database with expression indexes
  CREATE INDEX person_state_lower ON person (LOWER(state));
  -- PREFIX on PostgreSQL: the index must use text_pattern_ops (or the C collation)
  CREATE INDEX person_city_lower ON person (LOWER(city) text_pattern_ops);
  ```
  Mention that MySQL 8 needs the doubled parentheses `((LOWER(city)))` and that MariaDB needs an indexed virtual
  column instead. Keep the existing note that `CONTAINS` always scans the table.
- State plainly that the fields are ORed together for each word. The database can use indexes only when **every**
  `@Searchable` field on the entity is `PREFIX` or `EXACT` and indexed. A single `CONTAINS` field makes the whole
  search scan the table. Task 4 proves this.

- [ ] **Step 2: Update `CHANGELOG.md`**

Under `## [Unreleased]`:
```markdown
### Added

- **Match modes for `@Searchable`.** `@Searchable(match = MatchMode.PREFIX)` matches the start of a field and
  `MatchMode.EXACT` matches the whole value (case-insensitive), so databases can use an index on `LOWER(column)`.
  `CONTAINS` stays the default, so existing entities are unchanged.
```

- [ ] **Step 3: Verify the build**

Run: `mvn -q test`
Expected: BUILD SUCCESS. Javadoc changes compile, and the docs contain no broken code fences.

- [ ] **Step 4: Commit**

```bash
git add docs/guides/search.md CHANGELOG.md docs/index.md README.md AGENTS.md
git commit -m "docs: searchable match modes and indexing guidance"
```

---

### Task 4: Prove PostgreSQL uses the indexes

**Files:**
- Create: `src/test/java/org/jwcarman/jpa/integration/IndexedPlace.java`
- Create: `src/test/java/org/jwcarman/jpa/integration/IndexedPlaceRepository.java`
- Test: `src/test/java/org/jwcarman/jpa/integration/PostgreSQLIndexIT.java`

**Interfaces:**
- Consumes: `MatchMode` from Task 1, and `TestPerson` with `city` (PREFIX) and `state` (EXACT) from Task 2.

**Why this approach:** the test searches through the real repository, so the query goes through Hibernate's real
binding and pgjdbc's server-side prepared statements, exactly as in production. It then reads PostgreSQL's own
index-usage counters (`pg_stat_user_indexes.idx_scan`). The test does not rebuild the SQL by hand for `EXPLAIN`, and
it does not set `enable_seqscan = off`. That setting would hide the risk this test is looking for: a generic plan
that cannot use an index for `LIKE ?`.

- [ ] **Step 1: Create the entity and repository**

`IndexedPlace extends BaseEntity` follows the usual entity style, with
`@Searchable(match = MatchMode.PREFIX) String city` and `@Searchable(match = MatchMode.EXACT) String state`, and no
`CONTAINS` field. Create `IndexedPlaceRepository extends SearchableRepository<IndexedPlace, UUID>`.

- [ ] **Step 2: Write the test**

`PostgreSQLIndexIT` uses `@Testcontainers`, `@Tag("integration")`, `@SpringBootTest(classes = TestConfig.class)`, and
the same `postgres:17.6-alpine` `@ServiceConnection` container as `PostgreSQLIT`. It is not transactional, so each
search commits and its statistics get flushed. Properties:
`spring.jpa.hibernate.ddl-auto=create-drop` and `spring.datasource.hikari.maximum-pool-size=1`. A single connection
means the searches and the statistics reads run on the same backend.

`@BeforeAll` (or a once-guarded `@BeforeEach`) uses `JdbcTemplate` to:
1. Insert 20,000 rows into `indexed_place` and into `test_person` with
   `INSERT ... SELECT ... FROM generate_series(1, 20000) n`, using `gen_random_uuid()` for ids and `0` for version.
   Cities are `'City' || n`. States cycle through 50 two-letter codes, and row `n % 50 = 0` gets `'OH'`.
2. Create `LOWER(city) text_pattern_ops` and `LOWER(state)` indexes on both tables, named
   `<table>_city_lower` and `<table>_state_lower`.
3. Run `ANALYZE` on both tables.

Helper `long idxScans(String indexName)`: run `SELECT pg_stat_force_next_flush()` in its own statement, then
`SELECT idx_scan FROM pg_stat_user_indexes WHERE indexrelname = ?`. If a counter lags, poll for up to 5 s with a
short sleep, and say so in a comment.

```java
@ParameterizedTest
@ValueSource(strings = {"city1234", "oh", "city1234 oh"})
void uses_both_indexes_when_every_searchable_field_is_indexable(String term) {
    long city = idxScans("indexed_place_city_lower"), state = idxScans("indexed_place_state_lower");
    for (int i = 0; i < 10; i++) {          // past pgjdbc's prepareThreshold (5) and PostgreSQL's 5 custom plans
        places.search(term, Pageable.unpaged());
    }
    assertThat(idxScans("indexed_place_city_lower") - city).isGreaterThanOrEqualTo(10);
    assertThat(idxScans("indexed_place_state_lower") - state).isGreaterThanOrEqualTo(10);
}

@Test
void scans_the_table_when_any_searchable_field_is_contains() {
    long city = idxScans("test_person_city_lower"), state = idxScans("test_person_state_lower");
    for (int i = 0; i < 10; i++) {
        people.search("city1234", Pageable.unpaged());
    }
    assertThat(idxScans("test_person_city_lower")).isEqualTo(city);
    assertThat(idxScans("test_person_state_lower")).isEqualTo(state);
}
```

The second test is the control. It shows the counters really do separate "used an index" from "scanned the table",
and it pins the documented rule that one `CONTAINS` field forces a full scan.

- [ ] **Step 3: Run it**

Run: `mvn verify -Dit.test=PostgreSQLIndexIT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: BUILD SUCCESS. If the first test fails only after several repetitions, PostgreSQL switched to a generic
plan. **Stop and report it**, don't weaken the assertion. That finding changes the docs, and possibly the design.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/org/jwcarman/jpa/integration
git commit -m "test: prove PostgreSQL uses indexes for PREFIX and EXACT search"
```
