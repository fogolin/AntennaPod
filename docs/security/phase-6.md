# Security review: phase 6 (queue picker)

- **Scope:** `DBWriter.moveToQueue`, `DBReader.getQueueIdsOfItems` and `hasCustomQueues`, `PodDBAdapter` (the flag and `getQueueIdsOfItemsCursor`), `QueuePickerDialog`, `EpisodeMultiSelectActionHandler`, `FeedItemMenuHandler`, three menus, two icons and the strings
- **Date:** 2026-09-27
- **Result:** no open findings. Two issues found by the independent review were fixed in this phase.

| # | Area | Finding | Status |
|---|---|---|---|
| J1 | SQL | `getQueueIdsOfItemsCursor` builds its `IN (…)` list with the existing `getItemIds`, which joins `long` values, so no text reaches SQL. `getQueueIDCursor` uses `?` arguments. | OK |
| J2 | Main thread | `onPrepareMenu` only reads a `volatile` boolean, with no database access. The picker's two queries run on `Schedulers.computation()`. All writes go through `DBWriter`'s single DB thread. | OK |
| J3 | Single membership | Moving removes the episode's row before adding it, and the add still skips anything queued elsewhere. Both steps run back to back on the DB thread (`runOnDbThread` runs inline there), so no other write can slip in between. An episode can't end up in two queues. | OK, tested (`testMoveToQueue`, `testMoveToActiveQueue`) |
| J4 | Mixed selection | "Move to queue…" used to pass unqueued episodes too, and they were then added to the chosen queue. It now keeps only queued episodes, like "Add to queue…" keeps only unqueued ones. | **Fixed in this phase** (found by the independent review) |
| J5 | Destroyed activity | The picker shows after an async load. It now returns if the activity is finishing **or destroyed** (rotation, "Don't keep activities"), which avoids a `BadTokenException`. The load takes milliseconds, and the `Activity` is held only until then. | **Fixed in this phase** (found by the independent review) |
| J6 | Single-queue users | The new items default to `visible="false"` in every menu, and `onPrepareMenu` shows them only when `hasCustomQueues` is true. Every screen that inflates these menus calls `onPrepareMenu`. `deleteDatabase` resets the flag, so the emulator tests can't leak it from one test to the next. | OK |
| J7 | Stale flag | The flag is computed at database open and updated by the only writers of `Queues`. A database import force-restarts the app. A read-only open sets it to `false`, which hides the actions, the safe side. | OK |
| J8 | Rendering names | Queue names are plain list entries, with no HTML. A `NULL` title shows as "Queue". | OK |
| J9 | Exposure | No new activities, intents, permissions, exported components or network calls. | OK |
