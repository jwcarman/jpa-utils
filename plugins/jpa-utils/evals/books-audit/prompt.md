---
max_turns: 80
timeout_seconds: 2400
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

This app keeps a library's books, built with jpa-utils. Record who added each book and who last changed it, and when, and let clients change a book.

Other code and tests rely on this contract, so keep to it exactly:

- The user making a request is named by its `X-User` header. A request without one is made by `system`.
- A book is now answered as `{"id", "title", "author", "published", "createdBy", "createdDate", "modifiedBy", "modifiedDate", "version"}`. The dates are timestamps such as `"2026-11-23T14:05:00Z"`.
- Adding a book (`POST /api/books`, unchanged otherwise) sets both `createdBy` and `modifiedBy` to the user, and both dates.
- `PUT /api/books/{id}` with `{"title", "author", "published", "version"}` changes the book and answers `200` with it. Only `modifiedBy` and `modifiedDate` change; `createdBy` and `createdDate` keep their values. The same rules as adding apply to title, author and date (`400`).
- `version` is the one the client last read. If the book has changed since then, the change is refused with `409` and the book is left as it was.
- Everything else about the app stays as it is.
