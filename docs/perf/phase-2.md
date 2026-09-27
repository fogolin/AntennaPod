# Performance, reuse and simplification review: phase 2 (UI)

- **Date:** 2026-09-27

## Work per user action

| Trigger | Queries | Notes |
|---|---|---|
| Queue screen start | Same as upstream (`getQueue`, inbox count) plus `loadQueues`: one `SELECT` on `Queues` and one `getActiveQueueId` | Runs once per `onStart` |
| Player status change (frequent) | Unchanged: only `loadItems` | `loadQueues` is **not** tied to `loadItems`. Chips are rebuilt only on `onStart` and `QUEUES_CHANGED`. |
| Switch, create, rename, delete | One DB task, plus one `QUEUES_CHANGED` that reloads items and chips | The drawer badge and Home section reload through their existing `QueueEvent` subscriptions |
| Android Auto queue node | `getQueueIDList()` (ids of the active queue) instead of a filtered `COUNT` over all items | Same order of cost. It now matches the list shown. |

**Chip rebuilding:** `removeAllViews()` plus one inflate per queue. With a handful of queues that's negligible. A `RecyclerView` would add an adapter class for no measurable gain.

## Reuse

| Need | Reused |
|---|---|
| Chip look | `R.layout.item_tag_chip` (the Subscriptions tags and Search filters) |
| Chip container | `ChipGroup`, as in `search_fragment.xml` |
| Text input dialog | `EditTextDialogBinding` with `MaterialAlertDialogBuilder`, as in `RenameFeedDialog` and `AddFeedFragment` |
| Validation | Overriding the positive button with `setError`, as in `ParentalControlPreferencesFragment` |
| Keyboard | `ui.common.Keyboard.show` |
| Delete confirmation | `ui.common.ConfirmationDialog`, as in "Clear queue" |
| Menu | The existing `menu/queue.xml` and `refreshToolbarState()` |
| Default queue label | The existing translated `queue_label`, so it needs no new translation |

## Simplifications applied during the phase

1. **Dropped the planned chip adapter and long-press context menu** (plan §7) in favour of a `ChipGroup` and three overflow items. See [ADR-0007](../decisions/ADR-0007-queue-ui.md).
2. **One dialog class** handles both create and rename. `queue == null` means create.
3. **Scroll-to-top on switch lives in one place.** `loadQueues` compares the previous and new active queue. That covers chip taps, create and delete, and avoids a scroll call in each handler.
4. **The toolbar title is unchanged,** so there's no title logic to keep in sync.

## Diff size

About 180 lines added over 7 files: 145 of Java (75 in `QueueFragment`, 68 in the new `QueueNameDialog`) and 32 of XML (layout and menu). There are 6 new English strings, a one-line change each in `RemoveFromQueueSwipeAction` and `MediaLibrarySessionCallback`, and one Espresso test.
