# Performance, reuse and simplification reviews

A review is done at the end of every phase, in `phase-N.md`. Improvements found are applied in the same phase.

**Checklist:**

- Queries per user action compared with upstream. No extra query on hot paths: queue screen load, episode list binding, playback transitions.
- Indexes: are the new `WHERE` clauses covered?
- No work on the main thread. Database access stays on `dbExec` or Rx background schedulers.
- Reuse existing classes, layouts, dialogs and helpers instead of new ones. Every new class must justify itself.
- Diff size: remove anything that isn't needed.
