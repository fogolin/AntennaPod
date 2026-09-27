# Performance, reuse and simplification review: phase 6 (queue picker)

- **Date:** 2026-09-27

## Work per trigger

| Trigger | Extra work | Notes |
|---|---|---|
| Opening any episode menu or changing a multi-selection | One read of a `volatile` boolean | Nothing measurable. |
| Opening the picker | Two small queries off the main thread: the `Queues` list, and `SELECT DISTINCT queue … WHERE feeditem IN (…)` | The `feeditem` column of `Queue` is indexed. |
| Adding through the picker | Same as "Add to queue" | It's the phase 5 `addQueueItem(context, queueId, items)`. |
| Moving | One read of the target queue's ids, then the existing remove and add | One DB task. |
| App start | One `COUNT` on `Queues` when the database opens | A few rows. |
| Creating or deleting a queue | Deleting adds one `COUNT` | |

## Reuse

| Need | Reused |
|---|---|
| Where the actions appear | The three shared episode menus, `FeedItemMenuHandler.onPrepareMenu` (visibility) and `EpisodeMultiSelectActionHandler` (the action). The single-item path delegates to the multi-select handler, as "Mark as played" does. So every screen gets the actions without screen-specific code. |
| Adding to a chosen queue | `DBWriter.addQueueItem(context, queueId, items)` from phase 5 |
| Removing from a queue | `DBWriter.removeQueueItemSynchronous` |
| Queue list and names | `DBReader.getQueues()` and the translated `queue_label` |
| Dialog | `MaterialAlertDialogBuilder.setItems`, the same list-dialog style as elsewhere in the app |
| Messages | `showMessage` with plurals, like the other multi-select actions |
| Icons | Built from the existing `ic_playlist_play` shapes (three lines), with a plus or an arrow |

## Simplifications applied during the phase

1. **The flag lives where the queues are written.** An earlier draft kept it in `SharedPreferences`, written from `DBWriter` and `DBReader.getQueues`. That meant three writers, a reader with a side effect, and a stale value after a database import. The in-memory flag in `PodDBAdapter` has one owner, and it's recalculated on every database open.
2. **No second DB task in `moveToQueue`.** `runOnDbThread` runs inline on the DB thread, so the add runs right after the remove, and the returned `Future` covers both. The tests don't need to flush.
3. **One picker class for both actions.** Only the title and the callback differ.
4. **No "New queue…" entry in the picker.** It would have made the action useful with a single queue, but the user chose to keep single-queue menus unchanged.

## Diff size

About 250 lines of production code:

- Java, about 170 lines:
  - `QueuePickerDialog`, 70 (new);
  - `EpisodeMultiSelectActionHandler`, 31;
  - `DBWriter`, 26;
  - `PodDBAdapter`, 20;
  - `DBReader`, 19;
  - `FeedItemMenuHandler`, 7.
- XML, about 70 lines: menu items (34) and two icons (36).

Plus four strings (7 lines) and four new tests (48 lines).
