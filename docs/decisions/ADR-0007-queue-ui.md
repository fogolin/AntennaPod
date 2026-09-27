# ADR-0007: Queue UI: chips switch, the overflow menu manages

- **Status:** accepted, 2026-09-27. Supersedes plan §7 (long-press context menu on chips, a new chip adapter).
- **Context:** The plan copied the Subscriptions tag pattern: `SubscriptionTagAdapter`, a long-press context menu, and `TagMenuHandler`. Reviewing it for phase 2 showed two things:
  - Queue counts are tiny, so a `RecyclerView` adapter is overkill. `SearchFragment` already builds `item_tag_chip` chips inside a `ChipGroup`.
  - A long-press context menu needs the "long-pressed item" plumbing (`longPressedItem`, `registerForContextMenu`, `MenuItemUtils`), and it's hard to discover.

## Decision

**Switching**

- A `ChipGroup` inside a `HorizontalScrollView`, in the Queue screen's app bar under the info bar.
- One `item_tag_chip` per queue, built in `QueueFragment.loadQueues()`.
- `singleSelection` and `selectionRequired` are set, and it's shown only with 2+ queues.
- Tapping a chip calls `DBWriter.switchQueue`.

**Managing**

- Three overflow items in the existing `menu/queue.xml`: **New queue** (always visible), plus **Rename queue** and **Delete queue**. Rename and Delete are visible only when the active queue isn't the default.
- They act on the active queue, the same way the existing Clear, Sort and Lock items do.

**Naming dialog**

- `QueueNameDialog` follows `RenameFeedDialog`: `EditTextDialogBinding` with `MaterialAlertDialogBuilder`.
- Validation follows the parental-control dialog: override the positive button, trim the input, and `setError` when it's empty.
- A 30-character `LengthFilter` caps the length.

**Deleting** uses `ConfirmationDialog`, as "Clear queue" does.

**The toolbar title stays "Queue".** The checked chip already shows which queue is active.

## Consequences

- No new adapter class and no context-menu code. Phase 2 adds one small class (`QueueNameDialog`, 68 lines) and about 75 lines in `QueueFragment`.
- The default queue can't be renamed or deleted, because its menu items are hidden. This fixes F5 from the phase-1 security review.
- A single-queue user sees exactly one new overflow item ("New queue"). R5 is kept as far as it can be, since the feature needs an entry point.
