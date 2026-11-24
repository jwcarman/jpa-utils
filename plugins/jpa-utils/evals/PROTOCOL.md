# Measuring jpa-utils' agent skill

Each measurement is written here before it runs, and its result appended after. Rules are never
edited once a run has started.

## Baseline: jpa-utils without a skill (written before it runs)

**Purpose:** find where coding agents go wrong using jpa-utils with nothing but the library, its
sources jar and their own knowledge, so the skill is written to fix those failures. There is one
arm and no decision rule: this finds causes, it does not compare.

- **Model:** Haiku (a cheap model leans on documentation most, so it shows what is missing).
- **Cases:** `books-list` (a paged, sorted, searchable list: 9 hidden tests) and `books-audit` (who
  changed a book and when, and stale versions: 5 hidden tests). The prompts pin the HTTP contract
  and say the app depends on jpa-utils; they name no jpa-utils type.
- **Fixture:** a new Spring Boot 4.2 app on H2 (`evals/fixture/`) with jpa-utils installed from
  this repository at `ff9558d` (`0.3.0-SNAPSHOT`, sources jar included), made by
  `scripts/eval-fixture.sh`. `books-audit` starts from the same app with `books-list`'s reference
  solution committed on top.
- **Runs:** 10 per case, in 2 batches of 5, `--ablation none` (the plugin has no skill). Every run
  is reported; none is rerun, dropped or replaced. A run that errors, times out or does nothing is a
  failure.
- **Scoring:** a run succeeds when all the case's hidden tests pass (`evals/scripts/accept.sh`) and
  the app's own build, `./mvnw -o verify`, passes on the run's work as it stands
  (`evals/scripts/build.sh`).
- **Cost:** about $0.05–0.15 a run, $1–3 in all.

Then every failure is classified from its build log, test output and diff (not the agent's
summary): cause, count, an example run, and where its fix belongs (a recipe example, a rule, a check
script, or jpa-utils itself).

### Amendment after batch 1 (written before the remaining runs)

Batch 1 of `books-list` (5 runs, $0.19) passed 5 of 5, and showed the case was wrong, not the
agents: the prompt said `search` finds books whose title or author "contains it" (the whole phrase),
while jpa-utils' search matches every word of the term separately. All five agents read jpa-utils'
sources, saw the difference, and wrote their own search to keep to the prompt (one said so in a
comment). The case measured the prompt's wording, not the library.

The prompt now describes jpa-utils' search (every word, anywhere in the title or author), with a
10th hidden test for it; batch 1 fails that test. Batch 1 is reported here and not counted. The
baseline is 10 new `books-list` runs and 10 `books-audit` runs, otherwise as above (about $1).

Lesson for library-skill: a case's prompt must ask for what the library does. A contract that
differs from it, even slightly, makes the right answer "don't use the library".

## Result: jpa-utils without a skill

| Case | Runs | Successes | Hidden tests all passed | Build passed | Mean turns | Mean time | Mean cost |
|---|---|---|---|---|---|---|---|
| books-list | 10 | 10 | 10 | 10 | 29.9 | 126 s | $0.039 |
| books-audit | 10 | 9 | 9 | 10 | 34.2 | 148 s | $0.032 |

**19 of 20.** Haiku, with no skill and no documentation but jpa-utils' sources jar, used the
library as intended in every counted run: `BaseEntity`, a sort enum with `SortPropertyProvider`,
`Paginator` with `PageParams` and `PageDto`, `@Searchable` with `SearchableRepository`, a 400 for
`UnknownSortByValueException`, and in `books-audit` `AuditableEntity` with `@EnableJpaAuditing` and
an `AuditorAware`. No run broke stable paging.

Why the one run failed:

| Cause | Count | Example | Where the fix belongs |
|---|---|---|---|
| The case: the prompt's timestamp example (`"2026-11-23T14:05:00Z"`) shows whole seconds; the run truncated to seconds, and the hidden test expects `modifiedDate` 50 ms later to be strictly after | 1 | audit b1 run 3 | the case (an example with fractions, or a test that waits a second), not the skill |

Where the turns went (tool calls per run, from the traces): about 5 of 29–33 read jpa-utils'
sources jar (unzip, `jar tf`, reading classes) to learn what a type does: how search splits a term,
what `AuditableEntity` needs enabled, the `Paginator` API. Builds were 3.5–4 calls.

**What this means for the skill.** On these jobs agents do not fail for want of a skill, so a skill
cannot be measured by successes here. What it can change is cost: the ~5 calls spent reverse
engineering the jar per run (about a sixth), and the search semantics every batch-1 run had to dig
out. The skill's measurement should be turns, time and cost to a passing build at equal success,
as roux's was in round 2, or harder cases than these, found first.
