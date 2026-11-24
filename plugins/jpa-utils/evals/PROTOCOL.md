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
