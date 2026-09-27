# Phase 6: "Add to queue…" and "Move to queue…"

- **Branch:** `mq/phase-6-add-to-queue`, cut from `multiple-queues` (`0ee0c8b`, after PR #7). Its PR goes into `multiple-queues`.
- **Date:** 2026-09-27
- **Decision:** [ADR-0011](../decisions/ADR-0011-queue-picker.md)

## Upstream sync

- Upstream `develop` gained one commit before this phase: `d3c01a5` "Enable bottom navigation for all existing users (#8808)". It adds a step to `PreferenceUpgrader` for `oldVersion < 3130000`, so it takes effect at the 3.13 version bump, for the fork as for the official app.
- The sync workflow hadn't been run yet when phase 6 was ready, so the commit was merged into this branch (workflow step 0). The merge was clean, touching one file.

## Why

Putting an episode into a queue that isn't active meant switching to that queue first. Moving one between queues took three steps. This was the backlog item "Move to queue… / choose the queue when adding", and the user picked it for phase 6.

## Decisions (the user's)

- **Both actions.** "Add to queue…" for unqueued episodes, and "Move to queue…" for queued ones.
- **No change for single-queue users.** The user pointed out that upstream maintainers want the default experience left alone. So the actions only appear once a custom queue exists, and the always-visible option was rejected.

## What changed

| File | Change |
|---|---|
| `storage/database/…/PodDBAdapter.java` | An in-memory `hasCustomQueues` flag, set when the database opens and updated by `insertQueue`, `removeQueue` and `deleteDatabase`. Also `getQueueIdsOfItemsCursor`. |
| `storage/database/…/DBReader.java` | `hasCustomQueues()` (no database access) and `getQueueIdsOfItems(ids)`. |
| `storage/database/…/DBWriter.java` | `moveToQueue(context, queueId, items)`. |
| `app/…/ui/screen/queue/QueuePickerDialog.java` | **New.** Loads the queues off the main thread, leaves out the queue that already holds all the episodes, and shows a list dialog. |
| `app/…/ui/episodeslist/FeedItemMenuHandler.java` | Visibility of the two actions. A single item is delegated to the multi-select handler. |
| `app/…/ui/episodeslist/EpisodeMultiSelectActionHandler.java` | The two actions: picker, then add or move, then the plurals message. |
| `app/src/main/res/menu/feeditemlist_context.xml`, `feeditem_options.xml`, `episodes_apply_action_speeddial.xml` | The two items, hidden by default. |
| `app/src/main/res/drawable/ic_playlist_add.xml`, `ic_playlist_move.xml` | **New.** Speed-dial icons, drawn from the `ic_playlist_play` shapes. |
| `ui/i18n/…/values/strings.xml` | `add_to_other_queue_label`, `move_to_other_queue_label`, `move_to_queue_label`, `moved_to_queue_message`. |
| `DbMultipleQueuesTest` | Four tests: move, move into the active queue, queue ids of episodes, and the flag following create and delete. |
| `docs/…` | ADR-0011, this log, the reviews, the spec, fixtures, the backlog, the index, and phase 5's results. |

## Reuse check

Before writing code, these were checked:

- **Menus:** all ten or so screens with episode actions use one of three shared menus. Visibility goes through `FeedItemMenuHandler.onPrepareMenu`, and actions through `FeedItemMenuHandler.onMenuItemClicked` or `EpisodeMultiSelectActionHandler`. Adding the items there covers every screen, and no screen needed changes.
- **Queue writes:** phase 5's `addQueueItem(context, queueId, items)` and the existing `removeQueueItemSynchronous`.
- **Dialogs:** `MaterialAlertDialogBuilder.setItems`, and the `QueueNameDialog` pattern for a small dialog class next to the Queue screen.
- **Main-thread rule:** `PodDBAdapter.open()` asserts it isn't on the main thread in debug builds, so menu visibility couldn't query the database. That's why the in-memory flag exists (ADR-0011).

## Security and performance

- [security/phase-6.md](../security/phase-6.md): nothing user-typed reaches SQL, no database access on the main thread, single membership is kept, and two review findings were fixed (mixed selections, destroyed activity).
- [perf/phase-6.md](../perf/phase-6.md): one boolean read per menu, two small queries per picker, and the flag design was simplified from three writers to one owner.

## Independent review

A separate agent reviewed the diff for compile errors, runtime bugs and test correctness, since the workspace can't build. It found:

- **"Move to queue…" also added unqueued episodes** from a mixed selection. Fixed.
- **The picker could show on a destroyed activity.** Fixed.
- **`deleteDatabase` didn't reset the flag.** Fixed.
- **The brief was wrong about the DB thread:** `runOnDbThread` runs inline when already on it. The two test "flushes" were removed.

## Local checks

| Check | Result |
|---|---|
| checkstyle 10.12.0 on the changed Java files | clean |
| CI's XML check (layouts only) | not affected (no layout changed). The menus keep upstream's formatting, so the diff stays minimal. |

## How to test (debug APK from the PR, or the release after merging)

1. **Single queue first (optional):** with only the default queue, long-press an episode. The menu is exactly as before, with no "…" items.
2. **Setup:** have at least two queues, for example "Queue" (active) and "Running".
3. **Add:** in a podcast's episode list, long-press an episode that isn't queued. Choose **Add to queue…**. The picker lists "Queue" and "Running". Choose "Running": the episode gets the in-queue icon, and the Queue screen (showing "Queue") doesn't change.
4. **Move from the Queue screen:** long-press an episode in "Queue" and choose **Move to queue…**. The picker offers only "Running" (the current queue is left out). The episode disappears from "Queue", and it's in "Running" when you switch there.
5. **Multi-select:** select several episodes in "Queue" and choose **Move to queue…** from the speed dial. The message says "N episodes moved to another queue."
6. **Episode screen:** open an episode that's in "Running". The ⋮ menu has **Move to queue…**, and choosing "Queue" moves it into the list you're looking at.
7. **Back to one queue:** delete "Running". The "…" items disappear from the menus.

## Results

- **CI on the PR:** pending.

## Phase 5 results (recorded here, since phase 5's PR was already merged)

- **CI on PR #7:** all 12 checks green, including the unit tests and emulator tests on API 23, 30 and 36.
- **Merged** by the user (PR #7, merge commit `0ee0c8b`).
- **Release:** `Fork release` published [v3.12.1-mq.4](https://github.com/fogolin/AntennaPod/releases/tag/v3.12.1-mq.4), a 9.5 MB APK, in about 3 minutes.
- **Also merged:** PR #6 (label the sync PRs), released as `v3.12.1-mq.3`.
