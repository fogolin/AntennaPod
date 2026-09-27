# ADR-0006: Other v1 product decisions

- **Status:** accepted for v1, 2026-09-26. These are plan decisions D3–D7 and are still open for maintainer feedback.

| # | Decision | Reason |
|---|---|---|
| D3 | Deleting a queue asks for confirmation. Its episodes become unqueued (like "Clear queue"). If it was active, the default queue becomes active. | Mirrors "Clear queue" and doesn't silently grow the default queue. |
| D4 | Keep-sorted, sort order, lock and enqueue location stay **global** preferences. They apply to whichever queue is active. | Bare-minimum MVP. Per-queue settings are on the backlog (keunes expects them eventually). |
| D5 | Search opened from the Queue screen (the `QUEUED` filter) matches episodes in any queue. | Scoping it would need the active id inside `FeedItemFilterQuery`. Low value for v1. |
| D6 | The switcher is a chip row on the Queue screen (the Subscriptions tag pattern), shown only when there are 2+ queues. | It's an existing pattern and it doubles as the "which queue am I in" indicator. |
| D7 | The default queue can't be renamed in v1. | See ADR-0003. |
