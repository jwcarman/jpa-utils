# jpa-utils agent skill, step 1: the baseline

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Find, by measurement, where coding agents go wrong using jpa-utils without a skill, so the
skill (the next plan) is written to fix those failures and nothing else.

**Architecture:** library-skill's step 1 (`references/baseline.md`), laid out like roux's plugin: a
Claude Code plugin in jpa-utils (`plugins/jpa-utils/`, no skill yet) holding a public fixture app,
two eval cases with hidden acceptance tests, the scorers, and library-skill's runner and scorer
adapted. Haiku runs each case 10 times with no skill; every failure is classified by cause and
recorded in `plugins/jpa-utils/evals/PROTOCOL.md`. This plan stops there: the skill is written by
the next plan, after James has seen the failures.

**Tech Stack:** Java 25, Spring Boot 4.x, Spring Data JPA, H2, Maven wrapper, bash, Python 3,
`claude plugin eval`, library-skill's scripts.

**Spec:** `~/IdeaProjects/library-skill/docs/specs/2026-11-23-library-skill-design.md` ("Proving it:
jpa-utils"); method: library-skill `references/baseline.md`.

## Global Constraints

- Everything added is public and lives in jpa-utils; no change to jpa-utils' library code or its
  build (`pom.xml`, `src/`). The plugin folder is outside the Maven build.
- The fixture uses jpa-utils built from this checkout at a pinned commit (the skill will describe
  `main`, not 0.2.0), installed into the fixture's own Maven repository, so runs build offline.
- The fixture's database is H2, in the app and its tests: agents can then run their tests without
  Docker. jpa-utils is tested on H2 in its unit tests.
- Prompts pin the HTTP contract the hidden tests check (paths, parameters, JSON fields, status
  codes) and say the app already depends on jpa-utils; they never name a jpa-utils type.
- Success = all hidden tests pass and the fixture's build (`./mvnw -o verify`) passes on the run's
  work. Every run counts; errors and timeouts are failures.
- Haiku. Cost stated before running: ~$0.05–0.15 a run, 20 runs, about $1–3.
- No star imports, no warning suppression; commit only what passes.

## Review Focus

1. The fixture must build offline under the eval's sandbox (no network, `$HOME` unreadable): proven
   by a preflight like roux's, not assumed.
2. Hidden tests must pass on a known-good solution and fail on the untouched fixture, or a baseline
   failure means the test, not the agent.
3. A contract the prompt leaves unpinned (a parameter name, an error status) shows up as agent
   failures that are really guessing: each hidden test checks only what the prompt pins.
4. Scorers must report a run that does not compile as FAIL for every hidden test, never as "no tests".
5. Classification reads the build log and diff, not the agent's summary of what it did.

---

### Task 1: The plugin and the fixture app

**Files:**
- Create: `.claude-plugin/marketplace.json` (repo root: one plugin, `./plugins/jpa-utils`)
- Create: `plugins/jpa-utils/.claude-plugin/plugin.json`
- Create: `plugins/jpa-utils/evals/fixture/` (the app's source)
- Create: `plugins/jpa-utils/scripts/eval-fixture.sh`, `eval-preflight.sh`

- [ ] **Step 1:** The fixture source: a new Spring Boot 4 app, package `app`, Maven wrapper, Spring
  Web, Spring Data JPA, Bean Validation, H2, jpa-utils `${jpa-utils.version}`; `App.java`,
  `application.yml` (H2 in memory, `ddl-auto: create-drop`), `AppTest` (context loads). Nothing else:
  a new app, so agents have no feature to copy.
- [ ] **Step 2:** `eval-fixture.sh <commit> <dir>`: `mvn install -DskipTests` of jpa-utils at
  `<commit>` (a worktree) into `<dir>/.m2repo`; copy the fixture; set `jpa-utils.version`;
  `.mvn/maven.config` with `-Dmaven.repo.local=<dir>/.m2repo`; one online `./mvnw verify` to fill the
  repository, then `./mvnw -o verify` must pass; `git init` and one commit. Prints `<dir>`.
- [ ] **Step 3:** `eval-preflight.sh <dir>` (ported from roux's): under `sandbox-exec` with `$HOME`
  unreadable and a refusing proxy, `./mvnw -B -o -q verify` on a copy passes.
- [ ] **Step 4:** Run both on `HEAD`. Expected: preflight `ok`. `claude plugin validate plugins/jpa-utils`
  passes. Commit `test(plugin): a fixture app for measuring agents using jpa-utils`.

### Task 2: Two cases and their hidden tests

**Files:** Create `plugins/jpa-utils/evals/{books-list,books-audit}/{case.yaml,prompt.md,place-app.sh}`,
`acceptance/java/app/acceptance/*AcceptanceTest.java`, and `reference/` (a known-good solution, never
copied into a run).

- [ ] **Step 1: `books-list`.** Prompt: a `Book` (title ≤ 100 chars, required; author, required;
  published `LocalDate`, required) and `GET /api/books`, paged and sorted, searchable by title and
  author. Pinned: query parameters `pageIndex`, `pageSize`, `sortBy` (`TITLE`, `AUTHOR`, `PUBLISHED`),
  `sortDirection` (`ASC`/`DESC`, any case), `search`; answer `{"data": [...], "pagination":
  {"pageIndex", "pageSize", "totalElementCount", "totalPageCount", "hasNext", "hasPrevious"}}`; a book
  as `{"id", "title", "author", "published"}`; an unknown `sortBy` is a `400`; `POST /api/books`
  adds one (`201`). Hidden tests (MockMvc on H2): default page; page 2 of 3; each sort both ways;
  search matches title or author, case-insensitive, and a `%` or `_` in the search is matched
  literally; unknown `sortBy` → 400; **paging through ties** (12 books, one author, `sortBy=AUTHOR`,
  `pageSize=5`: three pages return all 12 ids, none twice).
- [ ] **Step 2: `books-audit`.** Prompt: the same app with a `Book` already listed (the case's
  `place-app.sh` lays `books-list`'s reference solution over the fixture); add who created and last
  changed each book and when. Pinned: the request's `X-User` header names the user (absent →
  `"system"`); `PUT /api/books/{id}` with `{"title", "author", "published", "version"}`; a book's
  JSON gains `createdBy`, `createdDate`, `modifiedBy`, `modifiedDate`, `version`; a stale `version`
  is a `409`. Hidden tests: created/modified set on create; only modified* change on update; header
  absent → `system`; stale version → 409.
- [ ] **Step 3:** Each case's hidden tests pass on its `reference/` solution and fail on the fixture
  as placed: run them with `accept.sh` (Task 3) both ways. Commit
  `test(plugin): two eval cases with hidden acceptance tests`.

### Task 3: Scorers and runner

**Files:** Create `plugins/jpa-utils/evals/scripts/{accept.sh,build.sh}`; copy library-skill's
`run-evals.sh`, `score.py`, `PROTOCOL-template.md` into `plugins/jpa-utils/scripts/` and adapt.

- [ ] **Step 1:** `accept.sh <case dir> <work dir>`: on a copy, the run's own tests are reset to the
  fixture's, the case's `acceptance/java` copied into `src/test/java`, `./mvnw -B -o test
  -Dtest='app.acceptance.*'`; one `PASS accept:` / `FAIL accept:` line per hidden test from the
  Surefire reports; if the code does not compile, every hidden test is a `FAIL` with the compile error.
- [ ] **Step 2:** `build.sh <work dir>`: `./mvnw -B -o verify` → `PASS build` / `FAIL build` with the
  first error lines.
- [ ] **Step 3:** `run-evals.sh`'s `ADAPT:` lines point at these, and it gains `--ablation none` (one
  arm, the plugin as it is): a baseline needs no second arm. Carry that change back to
  library-skill's `run-evals.sh` with its dry-run test. Prove on the reference solutions:
  `accept.sh` all PASS, `build.sh` PASS; on the fixture: all FAIL, build PASS. Commit
  `test(plugin): scoring a run's work`.

### Task 4: The baseline

- [ ] **Step 1:** `PROTOCOL.md`: "Baseline: jpa-utils without a skill (written before it runs)":
  cases, fixture commit, Haiku, 10 runs per case, no skill (the plugin has none yet), scoring as
  above, cost ~$1–3. This is for finding failures, not a comparison: no decision rule. Commit.
- [ ] **Step 2:** Run: `run-evals.sh --a plugins/jpa-utils --fixture <dir> --runs 5 --batches 2
  --model haiku --ablation none` per case. Check the first batch: hidden tests ran, the
  count matches the log.
- [ ] **Step 3:** Score with `score.py`; classify every failure from its build log and diff (cause,
  count, example run); for each cause, where its fix belongs (recipe example, rule, check script,
  or jpa-utils itself).
- [ ] **Step 4:** Append the result and the failure table to `PROTOCOL.md`; commit
  `test(plugin): what agents get wrong using jpa-utils without a skill`. Report to James with the
  table; the skill's plan comes next.
