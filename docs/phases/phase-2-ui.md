# Phase 2: queue UI

- **Branch:** `mq/phase-2-ui`, as PR #3 into `multiple-queues`. It was opened on top of phase 1 and retargeted after PR #2 was merged.
- **Date:** 2026-09-27
- **User-visible changes:** yes. This is the first phase you can see and use.

## What the user gets

**One queue (the default):**
- The app looks exactly as before, except for one new item at the bottom of the Queue screen's ⋮ menu: **New queue**.

**Creating a queue:**
- ⋮ → New queue asks for a name. Empty names are rejected, and names are capped at 30 characters.
- The new queue becomes active immediately, so you land in an empty queue.

**Two or more queues:**
- A row of chips appears under the Queue screen's header, one per queue. The first is "Queue", the default.
- The checked chip is the active queue. Tap another chip to switch; the list scrolls to the top of that queue.
- While a custom queue is active, the ⋮ menu also shows **Rename queue** and **Delete queue**. They're hidden for the default queue.
- **Delete queue** asks for confirmation. Its episodes are removed from the queue but not deleted, and the default queue becomes active. When only the default queue is left, the chips disappear.

**Where the active queue applies** (built in phase 1): Add to queue, auto-enqueue, playback order, the drawer or bottom-navigation count, Home "Continue listening", Android Auto and Wear.

## What changed

| File | Change |
|---|---|
| `app/.../ui/screen/queue/QueueFragment.java` | Chip row (`loadQueues`, `switchQueue`), handling for the `QUEUES_CHANGED` event, and the menu items New, Rename and Delete. Rename and Delete are only shown for custom queues. Select mode ends when switching. |
| `app/.../ui/screen/queue/QueueNameDialog.java` | **New.** Create and rename dialog with validation. |
| `app/src/main/res/layout/queue_fragment.xml` | `queueChipsContainer` (a `HorizontalScrollView`) holding `queueChipGroup`, hidden by default. |
| `app/src/main/res/menu/queue.xml` | `new_queue_item`, `rename_queue_item` and `delete_queue_item`. The last two are hidden by default. |
| `app/.../ui/swipeactions/RemoveFromQueueSwipeAction.java` | Undo is only offered when the item was in the active queue (security review G4). |
| `playback/.../internal/MediaLibrarySessionCallback.java` | The Android Auto "Queue" item count uses the active queue. |
| `ui/i18n/.../values/strings.xml` | 6 English strings: `new_queue_label`, `rename_queue_label`, `delete_queue_label`, `delete_queue_confirmation`, `queue_name_label`, `queue_name_empty`. |
| `app/src/androidTest/.../ui/QueueFragmentTest.java` | `testCreateRenameAndDeleteQueue`: checks empty-name validation, then create, rename and delete. |

## Reuse check

- The chips reuse `item_tag_chip` and the `ChipGroup` pattern from `SearchFragment`.
- The dialog reuses `EditTextDialogBinding`, `MaterialAlertDialogBuilder`, `Keyboard` and `ConfirmationDialog`.
- The default queue's name reuses the translated `queue_label`.
- The plan's long-press chip menu was replaced by overflow items, following how Clear, Sort and Lock already work. See [ADR-0007](../decisions/ADR-0007-queue-ui.md).

## Local checks before pushing

This workspace can't build Android (see [findings/environment.md](../findings/environment.md)), but it can run two of CI's style tools:

| Tool | Result |
|---|---|
| Checkstyle 10.12.0 with `config/checkstyle/checkstyle.xml` on every changed production file | 0 errors. One `VariableDeclarationUsageDistance` finding was fixed first. |
| CI's `android-xml-formatter` 1.1.0 on `queue_fragment.xml` | No changes |

## Security and performance

- [security/phase-2.md](../security/phase-2.md): closes F4 and F5 from phase 1, and fixes G3 (stale selection across a switch) and G4 (swipe undo).
- [perf/phase-2.md](../perf/phase-2.md): the chips reload only on screen start and on queue changes, not on every player-status event.

## How to test

Install the APK from this PR's CI run (Actions → the run → Artifacts → `app-play-debug.apk`). Use the debug build with your imported database (see phase 1).

| # | Scenario | Expected |
|---|---|---|
| 1 | Open Queue with only the default queue | Same as before. The ⋮ menu has **New queue**, and no Rename or Delete. No chips. |
| 2 | ⋮ → New queue, leave the name empty, tap Confirm | The dialog stays open with "Name cannot be empty". |
| 3 | Type "Running" and confirm | Chips appear: **Queue** and **Running** (checked). The list is empty. |
| 4 | Add 2 episodes from Episodes or a podcast page | They land in **Running**. The episode-list icons show them as queued, and the badge counts 2. |
| 5 | Try adding an episode that's already in **Queue** | Nothing happens. It stays in **Queue** (one queue per episode, ADR-0004). |
| 6 | Tap the **Queue** chip | Your original queue is back, scrolled to the top. The badge shows its count. |
| 7 | With **Queue** active, play the last-but-one episode to its end | The next episode of **Queue** plays. |
| 8 | Switch to **Running** and play its first episode to its end | The second **Running** episode plays. |
| 9 | Play an episode that's in **Queue** while **Running** is active, then let it finish | Playback stops after it (ADR-0005). |
| 10 | With **Running** active: ⋮ → Rename queue → "Gym" | The chip now reads **Gym**. |
| 11 | Swipe-remove an episode in **Gym**, then tap Undo | It comes back in the same place. |
| 12 | Long-press an episode → multi-select → tap the **Queue** chip | Select mode ends, then the queue switches. |
| 13 | With **Gym** active: ⋮ → Delete queue → Confirm | **Queue** becomes active, the chips disappear, and Gym's episodes are no longer marked as queued. They aren't deleted. |
| 14 | Android Auto (if available) | "Queue" lists the active queue, and the count matches the list. |
| 15 | Rotate the screen with 2+ queues | The chips and the checked state survive. |

## Results

**CI run 1** ([36327820878](https://github.com/fogolin/AntennaPod/actions/runs/36327820878), commit `63151d3`):

| Check | Result |
|---|---|
| Static analysis | pass |
| Unit tests PlayDebug, PlayRelease, FreeRelease | pass |
| Emulator tests API 30 debug, API 36 debug, API 23 release, API 36 release | pass |
| Emulator tests **API 23 debug** | **fail**, after the script's 3 retries |

- **Can't read the log:** the job log and the `test-report` artifact sit in Azure blob storage, which the workspace can't reach (see [environment](../findings/environment.md)).
- **Reasoning:** the new test passed on API 23 **release** and on API 30 and 36 **debug**. Only `app` has instrumented tests, so the debug and release runs execute the same suite. What's specific to API 23 debug:
  - it's the slowest configuration (unoptimised build on a `default` image with swiftshader);
  - the `default` image has a real soft keyboard (the API 30 and 36 `aosp_atd` images don't), and `QueueNameDialog` opens it.
- **Fix:** harden `testCreateRenameAndDeleteQueue` the way `AddFeedFragmentTest` handles the same kind of dialog:
  - call `Espresso.closeSoftKeyboard()` before each Confirm;
  - click Confirm with `scrollTo(), click()`;
  - wait up to 10 s instead of 3 s. The helpers poll every 50 ms, so this costs nothing when things are fast.

**CI run 2:** see the PR checks.

- **Manual test:** pending the user.
