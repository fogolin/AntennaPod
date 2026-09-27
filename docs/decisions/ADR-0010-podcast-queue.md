# ADR-0010: Each podcast can send its automatic adds to a chosen queue

- **Status:** accepted, 2026-09-27. Amends [ADR-0005](ADR-0005-active-queue.md) for automatic adds only. Manual adds still go to the active queue.

## Context

- Routing podcasts to queues is the most requested follow-up (see the [backlog](../future/backlog.md)).
- keunes (Oct 2025) described a default queue per podcast. ByteHamster (2022) preferred reusing tags over new per-subscription state.
- Upstream PR #8215 (enqueue location per podcast) also puts a per-podcast enqueue setting in the podcast settings.

## Decision (the user's choices)

1. **Per podcast, not per tag.** Podcast settings → Automation gets **"Queue for new episodes"**, right below "New episodes action".
   - The choices are **"Active queue"** (the default, meaning today's behavior) and every queue, including the default "Queue".
   - The setting is hidden while only one queue exists, like the queue chips.
2. **Automatic adds only.** Two paths follow the podcast's queue:
   - the "Add to queue" new-episodes action;
   - "Enqueue downloaded", which also covers episodes that automatic download fetched.

   Manual adds (the menu, swipe, multi-select, and playing an unqueued episode) keep going to the active queue, so the user can still hand-mix a queue.

## Technical rules

| Rule | Where |
|---|---|
| Stored as `Feeds.feed_queue INTEGER DEFAULT -1`. `-1` is `Queue.ACTIVE_QUEUE_ID`, and `0` is the default queue. | `PodDBAdapter.CREATE_TABLE_FEEDS`, `KEYS_FEED`, `setFeedPreferences` |
| Existing databases get the column from the guarded, idempotent migration. The DB version doesn't change (ADR-0002). | `DBUpgrader.upgradeMultipleQueues` |
| Loaded into `FeedPreferences.queueId` with a setter, so the upstream constructors stay untouched. | `FeedPreferencesCursor`, `FeedPreferences` |
| The column is named `feed_queue`, not `queue`, because `queue` is already a column of the `Queue` table and a join of both tables would make it ambiguous. | `PodDBAdapter.KEY_FEED_QUEUE` |
| `DBWriter.addQueueItem(context, queueId, items)` resolves the target on the DB thread. `-1`, or an id that no longer exists, means the active queue. The old `addQueueItem(context, items)` delegates with `-1`. | `DBWriter` |
| Adding to a queue that isn't active posts **no** `QueueEvent`. The Queue screen shows the active queue, so an `ADDED` or `SORTED` event would insert into, or replace, the wrong list. `FeedItemEvent` is still posted, so in-queue icons update. | `DBWriter.addQueueItem` |
| Deleting a queue resets the podcasts that pointed to it to `-1`, in the same transaction. Queue ids are `AUTOINCREMENT`, so a stale id can never point to a newer queue. | `PodDBAdapter.removeQueue` |
| Enqueue location and keep-sorted apply to the target queue. "After currently playing" puts the episode at the front when the target queue doesn't hold the current episode. | existing `ItemEnqueuePositionCalculator` |
| Single membership (ADR-0004) is unchanged. An episode already in any queue isn't moved. | existing check in `addQueueItem` |

## Rejected alternatives

- **Tag → queue.** One setting would cover many podcasts, but it needs tags set up, and a podcast with two linked tags needs a tie-break rule. The user chose per podcast.
- **Every add follows the podcast.** The user couldn't hand-pick an episode into another queue.
- **A separate `FeedQueues` table.** That's one more table and join for a one-to-one value. Every other podcast setting is a `Feeds` column.
- **`DBWriter` grouping items by podcast.** Each automatic path already handles one podcast at a time and knows its settings. An explicit queue id keeps a single code path, and a future "Add to queue…" picker can reuse it.

## Consequences

- **Official app on the same database:** it ignores the column. Its `Feeds` updates name only its own columns, so the setting survives, and podcasts it adds get the default `-1`.
- **Episodes routed to a queue that isn't active** show the in-queue icon. "Automatically download queued episodes" covers them, because "queued" spans all queues. They play in order once that queue is active.
- **One podcast, one queue.** Several queues per podcast, and tag-based routing, stay in the backlog.
