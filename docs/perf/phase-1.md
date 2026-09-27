# Performance, reuse and simplification review: phase 1 (storage)

- **Date:** 2026-09-26

## Queries per operation, compared with upstream

| Operation | Upstream | Fork | Change |
|---|---|---|---|
| Remove from queue (menu, swipe, finished playback, sync) | Loads the **whole queue** with a join on items and media, plus feed data. Rewrites every row. | One indexed `SELECT feeditem ... IN (...)`, then one `DELETE ... IN (...)` | **Faster.** It no longer scales with queue length. This path runs on every finished episode. |
| Delete episodes or podcast | Loads the whole queue and rewrites it | One indexed `SELECT`. The row delete joins the existing `removeFeedItems` transaction. | **Faster** |
| Add to queue | Load queue, rewrite | Same, plus one indexed `SELECT` for the any-queue check | +1 small query |
| Move, sort, clear | Load queue, rewrite | Same | Same |
| Any active-queue read (`getQueue`, `getQueueIDList`, `getNextInQueue`, `getPausedQueue`, drawer badge) | — | `getActiveQueueId()`: a preference read only for the default queue, plus one primary-key `COUNT` on the tiny `Queues` table for a custom queue | Negligible |
| `setQueue` | Deletes **all** rows | Deletes the rows of one queue (`WHERE queue = ?`) | Same order of cost |

## Indexes

- `Queue(feeditem)` already exists and now also covers the membership checks.
- **No index on `Queue.queue`.** Queue reads filter on `queue` and order by `id`. SQLite walks the table by row id and filters, which costs O(total queued rows across all queues). That's typically tens to a few hundred rows. An index would add a write cost on every `setQueue` and another schema object to keep compatible, for no measurable gain. Revisit if someone keeps thousands of queued episodes.

## Threading

- There are no new main-thread paths. All writes go through `runOnDbThread`.
- `DBReader.getActiveQueueId()` touches the database for custom queues. Phase 2 must call it off the main thread, like every other `DBReader` method. The debug build crashes on main-thread I/O (`ThreadUtils.assertNotMainThread`).

## Reuse

| Need | Reused |
|---|---|
| Id lists | `LongList` (`contains`) |
| Existence check | `DatabaseUtils.queryNumEntries` |
| Id join | The `getItemIds` pattern, as a `long...` overload |
| Change notifications | `QueueEvent` (one new action instead of a new event class) and `FeedItemEvent` for tag changes |
| Model | The `ProxyConfig`-style model class (public final fields) |

**New code with no existing equivalent:** `model.queue.Queue` (2 fields) and `DBUpgrader.upgradeMultipleQueues` with `hasColumn`.

## Simplifications applied during the phase

1. **The first draft** looked up which queues held the items (`getQueueIdsOfItems`), then loaded and rewrote each of those queues. Order is by row id and deleting rows can't reorder the others, so removal became a plain `DELETE ... WHERE feeditem IN (...)`. One DB helper was dropped, and both removal paths no longer read any queue.
2. **`deleteFeedItemsSynchronous`** no longer calls `setQueue`. The rows go inside the existing `removeFeedItems` transaction.
3. **No `position` column** (ADR-0002). All the `ORDER BY` clauses stay as upstream wrote them.

## Diff size

About 290 production lines added and about 40 removed, over 7 files. Most of it is in `DBWriter` (4 new public methods) and `PodDBAdapter` (6 new small methods). No file outside the storage, preferences, event and model modules is touched.
