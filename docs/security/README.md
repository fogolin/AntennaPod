# Security reviews

A review is done at the end of every phase, in `phase-N.md`. The checklist, adapted to this app (a local SQLite database, no server of its own):

- **SQL injection:** values from users (queue names, anything typed) must go through `ContentValues` or bind arguments, never string concatenation. Ids that are Java `long`s can't carry SQL.
- **Data loss:** every migration and delete path must be idempotent and must not drop episodes, history or downloads. The official-app round trip must be checked (ADR-0002).
- **Input validation:** length and emptiness of user-provided names, and how they're rendered (no HTML).
- **Concurrency:** writes on `dbExec` only. No check-then-act across threads.
- **Exposure:** no new exported components, intents, permissions or network calls. Nothing new sent through Android Auto, Wear or sync beyond what upstream already sends.
- **Backups:** new data must travel with the database export and import, and must stay private (no new files outside the app sandbox).
