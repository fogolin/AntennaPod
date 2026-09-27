# Security review: phase 5 (a queue per podcast)

- **Scope:** `Feeds.feed_queue` and its migration, `FeedPreferences`/`FeedPreferencesCursor`, `PodDBAdapter.setFeedPreferences` and `removeQueue`, `DBWriter.addQueueItem(context, queueId, items)`, the two automatic callers, and the podcast settings preference
- **Date:** 2026-09-27
- **Result:** no open findings.

| # | Area | Finding | Status |
|---|---|---|---|
| H1 | SQL | The migration and the schema only concatenate constants (`KEY_FEED_QUEUE`, `Queue.ACTIVE_QUEUE_ID`). The reset on queue delete and `queueExists` use `?` arguments. Nothing user-typed reaches SQL. | OK |
| H2 | Orphaned queue rows | A podcast could point to a queue that no longer exists (a queue deleted by an older build, or an edited or imported database). Writing rows with that id would hide episodes that are still marked as queued, and single membership would then block adding them anywhere else. `addQueueItem` checks `queueExists` on the DB thread and falls back to the active queue. `removeQueue` also resets the podcasts in the same transaction, and `AUTOINCREMENT` ids are never reused. | OK, tested (`testAddQueueItemToMissingQueueUsesActiveQueue`, `testDeleteQueueResetsFeedQueue`) |
| H3 | Wrong list on screen | Adding to a queue that isn't active would otherwise post `ADDED` (or `SORTED`, with keep-sorted on), and the Queue screen would insert the episode into, or replace, the list it shows. `addQueueItem` drops those events when the target isn't the active queue. | Handled in the design, tested (`testAddQueueItemToOtherQueue`) |
| H4 | Settings value | The list values are ids that the fragment built itself, so `Long.parseLong` can't see foreign input. A value that isn't in the list (a stale id) shows as "Active queue", which is also how routing treats it. | OK |
| H5 | Rendering names | Queue names are list entries (plain `CharSequence`), with no HTML parsing. A `NULL` title shows as "Queue", like the chips. | OK |
| H6 | Threading | The routing decision (`getActiveQueueId`, `queueExists`, `getQueue`) runs on the single DB thread, so it can't interleave with a queue switch or delete. The settings screen loads queues with `Observable.fromCallable` on `Schedulers.computation()`, and the disposable is released in `onDestroy`, like the existing one. | OK |
| H7 | Null safety in the download path | `DownloadServiceInterfaceImpl` can get an item whose podcast wasn't loaded. The queue id is only read when both `getFeed()` and `getPreferences()` are non-null. Otherwise it uses the active queue, as before. | OK |
| H8 | Official app on the same database | It ignores `feed_queue`, keeps it on its own `Feeds` updates (it names only its own columns), and new podcasts get the column default. Nothing it does can point a podcast at a missing queue that H2 doesn't already handle. | OK |
| H9 | Exposure | No new activities, intents, permissions, exported components or network calls. | OK |
