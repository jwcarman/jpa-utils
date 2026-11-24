---
max_turns: 80
timeout_seconds: 2400
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

This app keeps a library's books. Add books to it: each book has a title (required, at most 100 characters), an author (required, at most 100 characters) and the date it was published (required). Let clients add books and list them a page at a time, sorted and searchable. The app already depends on jpa-utils (see `pom.xml`); use it for the paging, sorting and search.

Other code and tests rely on this contract, so keep to it exactly:

- `POST /api/books` with `{"title", "author", "published"}` (`published` as `"2024-05-01"`) adds a book and answers `201` with it. A missing or blank title or author, or one over 100 characters, or a missing date, is a `400`.
- A book is answered as `{"id", "title", "author", "published"}`.
- `GET /api/books` answers `{"data": [books], "pagination": {"pageIndex", "pageSize", "totalElementCount", "totalPageCount", "hasNext", "hasPrevious"}}`.
- Its query parameters, all optional: `pageIndex` (from 0, default 0), `pageSize` (default 20), `sortBy` (`TITLE`, `AUTHOR` or `PUBLISHED`; default `TITLE`), `sortDirection` (`ASC` or `DESC` in any case; default `ASC`) and `search`.
- `search` is split into words, and finds the books where every word appears somewhere in the title or the author, ignoring case: `tower dark` finds "The Dark Tower" and "Tower of Darkness", and `king tower` finds "The Dark Tower" by Stephen King. Characters such as `%` and `_` in it match themselves.
- A `sortBy` that is not one of the three is a `400`.
- Paging through books that share a sort value (say, one author's books sorted by `AUTHOR`) shows every book exactly once.
