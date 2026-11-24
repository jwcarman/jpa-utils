# Coding Agents

jpa-utils ships an agent skill: a short guide a coding agent loads when it works in an app that
depends on jpa-utils. It covers the types an agent reaches for when it adds an entity, a list
endpoint or auditing:

- `BaseEntity` and `AuditableEntity`, and what auditing needs enabled (`@EnableJpaAuditing`, an
  `AuditorAware<String>`)
- `Paginator`, `PageParams`, `PageDto` and `SortPropertyProvider` for paged, sorted list endpoints
- `@Searchable` and `SearchableRepository`, and exactly what a search term matches

It also carries complete example files for a searchable list and for auditing, which an agent copies
and renames.

## Installing

The skill is a [Claude Code](https://claude.com/claude-code) plugin in this repository:

```bash
claude plugin marketplace add jwcarman/jpa-utils
claude plugin install jpa-utils@jpa-utils
```

Installed from git, it follows `main`, so it describes the latest jpa-utils, including features not
yet released. Lines about such features are marked *since X.Y.Z*; an agent on an older release
skips them.

## Does it help?

It was measured before release. Claude Haiku built two features into a new Spring Boot app, 20 runs
with the skill and 20 without, each checked by hidden acceptance tests and the app's own build:

| | Successes | Mean cost | Mean time | Mean turns |
|---|---|---|---|---|
| Without the skill | 18 of 20 | $0.036 | 122 s | 33.1 |
| With the skill | 20 of 20 | $0.023 | 85 s | 27.9 |

Without the skill, agents already used jpa-utils correctly; they spent their extra turns unpacking
its jar to learn the API. The skill saves those turns: 36% lower cost and 30% less time (one-sided
Mann-Whitney, p < 0.001). The cases, rules and results, written before each run, are in
[PROTOCOL.md](https://github.com/jwcarman/jpa-utils/blob/main/plugins/jpa-utils/evals/PROTOCOL.md).
