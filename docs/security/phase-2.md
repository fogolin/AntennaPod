# Security review: phase 2 (UI)

- **Scope:** `QueueFragment`, `QueueNameDialog`, `menu/queue.xml`, `layout/queue_fragment.xml`, `RemoveFromQueueSwipeAction`, `MediaLibrarySessionCallback`, and the English strings
- **Date:** 2026-09-27
- **Result:** no open findings. F4 and F5 from phase 1 are closed.

| # | Area | Finding | Status |
|---|---|---|---|
| F4 (phase 1) | Queue name validation | `QueueNameDialog` trims the input, rejects empty names with an inline error (the dialog stays open), and caps names at 30 characters with an `InputFilter.LengthFilter`. The input type is single-line text, so no newlines get in. | **Closed** |
| F5 (phase 1) | Deleting the default queue | The Rename and Delete menu items are hidden unless the active queue is a custom one. `DBWriter.deleteQueue(0)` also stays a no-op (tested in phase 1). | **Closed** |
| G1 | Rendering names | Names are set with `Chip.setText(String)` and passed as a `%1$s` argument to `getString`. No HTML parsing (`Html.fromHtml`) and no links, so no markup injection. | OK |
| G2 | Names from an imported database | A crafted database could contain names longer than 30 characters, or `NULL`. A long name only widens its chip, since the row scrolls horizontally. A `NULL` title shows as "Queue", and the delete dialog would print "null". Cosmetic, with no crash path: `activeQueue.title` is only passed to `getString` and `setText`, which accept null. | Accepted, noted in the backlog |
| G3 | Stale selection across a switch | Multi-select actions such as "Move to top" apply to the active queue. If the user switched queues with items still selected, `moveQueueItemsSynchronous` would re-insert them into the new queue, which could put an episode in two queues. `switchQueue` ends select mode before switching. | **Fixed in this phase** |
| G4 | Undo of "Remove from queue" for an item in another queue | Before, undo called `addQueueItemAt(-1)`, which threw an `IndexOutOfBoundsException` on the DB thread (swallowed by the `Future`). The undo snackbar is now offered only when the item was in the active queue (`position >= 0`). | **Fixed in this phase** |
| G5 | Threading | All database reads in the UI (`getQueues`, `getActiveQueueId`, `getQueue`) run in `Observable.fromCallable` on `Schedulers.computation()`. The debug build's `ThreadUtils.assertNotMainThread` would crash on main-thread I/O, and the emulator tests run the debug build. | OK |
| G6 | Exposure | No new activities, intents, permissions or exported components. Android Auto still lists only the active queue, and its item count now matches that list instead of counting all queues. | OK |
| G7 | Leaks | `queuesDisposable` is disposed in `onStop`, the same as the existing `disposable`. The dialog holds its `Activity` through a `WeakReference`, like `RenameFeedDialog`. | OK |
