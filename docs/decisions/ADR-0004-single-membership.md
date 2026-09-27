# ADR-0004: An episode is in at most one queue (v1)

- **Status:** accepted for v1, 2026-09-26. This is plan decision D1, still to be confirmed with the maintainers.

## Decision

- `DBWriter.addQueueItem` and `addQueueItemAt` skip items that are already in **any** queue, the same way they skip items already in the queue today.
- "Queued" keeps meaning "in some queue": `is_in_queue`, `FeedItem.TAG_QUEUE`, `FeedItemFilter.QUEUED`, auto-download of queued episodes and the cleanup algorithms that keep queued episodes.
- "Remove from queue", deleting an episode and deleting a podcast remove the episode from whichever queue holds it.

## Why

- None of the "queued" consumers need changes, and none become ambiguous.
- The in-queue icon, "Remove from queue" and swipe actions keep one clear meaning.
- keunes deferred episodes-in-several-queues in the forum design thread.
- The schema (one row per `queue` + `feeditem`) allows multiple membership later without a migration.

## Consequence

- To move an episode to another queue, the user removes it, switches queue and adds it again. A "Move to queue…" action is on the backlog.
- Data written by the official app (see ADR-0002) can't create duplicates, because that app always writes a single list.
