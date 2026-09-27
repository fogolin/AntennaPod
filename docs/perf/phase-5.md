# Performance, reuse and simplification review: phase 5 (a queue per podcast)

- **Date:** 2026-09-27

## Work per trigger

| Trigger | Extra work | Notes |
|---|---|---|
| Any add to queue (manual or automatic) | One `queueExists` count by primary key on `Queues` (a handful of rows) | `getActiveQueueId` already does the same lookup for a custom active queue. Not worth a special case. |
| Feed refresh | None | The podcast's queue comes with the preferences `updateFeed` already loads. |
| Enqueue after download | None | The queue id is read from the item's podcast in memory. |
| Adding to a queue that isn't active | **Less** than before: no `QueueEvent` | The Queue screen, drawer badge and home section don't reload for a list they don't show. `FeedItemEvent` still updates the in-queue icons. |
| Any podcast load | One more integer column in `KEYS_FEED` | Negligible. |
| Opening podcast settings | One `SELECT` on `Queues`, off the main thread | Runs once per screen. |
| Deleting a queue | One `UPDATE Feeds … WHERE feed_queue = ?` inside the existing transaction | No index needed. It's a full scan of the podcasts, once per delete. |

## Reuse

| Need | Reused |
|---|---|
| Adding to a queue | The existing `addQueueItem` body: duplicate and single-membership checks, enqueue location, keep-sorted, marking as unplayed, auto-download. The new overload only picks the queue and filters events. |
| Storage of a podcast setting | A `Feeds` column, read by `FeedPreferencesCursor` and written by `setFeedPreferences`, like every other podcast setting |
| Migration | `DBUpgrader.upgradeMultipleQueues` and its `hasColumn` helper from phase 1 |
| Queue list | `DBReader.getQueues()` from phase 1 |
| Existence check | `PodDBAdapter.queueExists` from phase 1 |
| Settings UI | `MaterialListPreference` and the change-listener pattern used by "New episodes action" and "Auto delete" |
| Icon and labels | `ic_playlist_play` (the queue icon) and the translated `queue_label` for the default queue. Two new strings. |

## Simplifications applied during the phase

1. **One code path for manual and automatic adds.** The routed add is an overload with a queue id. The old signature delegates to it, so the five manual callers didn't change. Only the two automatic callers pass a queue id.
2. **No grouping of items by podcast** inside `DBWriter`. Each automatic caller handles one podcast and already has its settings.
3. **No changes to the `FeedPreferences` constructors.** The field has a default and a setter, which keeps parsers, tests and upstream merges untouched.
4. **No separate table.** See ADR-0010.
5. **Event filtering is one `events.clear()`**, instead of wrapping the posting loop.

## Diff size

About 90 lines of production code over 11 files:

- 41 lines in `FeedSettingsPreferenceFragment`;
- 11 in `DBWriter`;
- 7 in `PodDBAdapter`;
- 10 in `FeedPreferences`;
- the rest one to five lines each.

Plus two English strings, one preference entry, and five new tests.
