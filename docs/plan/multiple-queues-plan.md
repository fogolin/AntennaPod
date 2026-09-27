# Multiple queues for AntennaPod: implementation plan

- **Issue:** #2648 (Multiple / User Definable Queues)
- **Supersedes:** #8070 (closed Feb 2026) and #8066 (closed)
- **Base:** `develop` at `34de86f` (2026-09-24), app 3.12.1, DB `VERSION = 3110000`
- **Status:** draft for review, 2026-09-26

---

## 0. Summary

**What v1 is.** Version 1 is the minimum that maintainers asked for in November 2025 after their "needs decision" call:

- The user can create, rename and delete manually curated queues.
- Switching queues changes what the existing Queue screen shows. The choice is remembered.
- The selected queue is the **active queue**. Playback, "add to queue" and every automatic enqueue use it.
- There is no picker dialog when adding an episode.
- There is no automation: no per-podcast routing, no smart queues.
- Nothing changes for people who never create a second queue.

**Core technical idea.** Extend the existing `Queue` table instead of adding a parallel system:

- Add a `queue` column (`0` means the existing default queue) and a `position` column.
- Add a small `Queues` table that stores the names of user-created queues.
- Store the active queue id in a preference.
- Scope the list operations in `PodDBAdapter` and `DBReader` to the active queue.

With this approach, around 40 existing call sites in playback, downloads, sync, swipe actions, menus, Android Auto, Wear and the home screen **do not change**.

**Size.** About 400 lines of production code plus tests, spread over about 15 files. Nothing from #8070 needs to be carried over except ideas.

---

## 1. What maintainers asked for

These are the requirements. Every design choice below traces back to one of them.

### 1.1 By source

| When | Where | Who | What they asked |
|---|---|---|---|
| Jun and Sep 2019 | PR #3221 (analysis moved to #2648) | ByteHamster | Multiple queues must not add UI complexity for single-queue users. Queue menu entries should appear only when multiple queues exist. Use fragments, not activities. No color editing. Queue settings belong in the existing Playback/Queue settings section. A feed should belong to only one queue, to avoid inconsistencies. |
| Apr 2022 | #2648 | ByteHamster | First define what "user definable" means. **Do not use the navigation drawer for queue management**, because the drawer may be replaced. Prefer the existing tagging feature over inventing new per-subscription state. |
| May 3, 2022 | #2648 | ByteHamster | A queue implies *manual* management. Automatic refilling is not a queue (that is #307). |
| May 14, 2022 (community call) | #2648 | ByteHamster | Build it incrementally, **"dumb" (manual) queues first**. Open design questions: how to show which queue is used for playback, which manual actions land in which queue, and what the "default" queue is. |
| May 24, 2022 | #2648 | ByteHamster | Behind-the-scenes (database) work can start first. Open a PR early. |
| Oct 3, 2025 | #2648 | ByteHamster | Main problems to solve: show the active queue, switch queues visually, define how "add to queue" behaves (when playing, downloading or adding manually), keep the app as easy as today for people who don't create queues, and **extend the existing queue** (don't rename it "legacy"). Process: open the PR early, no unrelated changes, **no experimental master switch** (merge only an MVP), **nothing automatic, manual queues and infrastructure first**. |
| Oct 3, 2025 | #2648 | keunes | Brainstorm: show the queue on the player screen and notification. Global default queue. Only show queue names once more than one queue exists. Each queue keeps its own sort settings. Auto-download stays a single process. Asked what sync should do. *(Parts of this were superseded by the Nov 2025 decision, see 1.3.)* |
| Oct 27, 2025 | PR #8066 | ByteHamster | Implement only **"the bare minimum to support multiple queues"**: no icon or color pickers. *"Please use our existing DBReader/DBWriter methods"* instead of a new repository and executor layer. Code that ignores the existing code base will not be accepted. |
| Nov 19, 2025 | forum "needs decision" call | team | Liked the UX of #8066, but want it implemented by a human contributor who knows the code, to reduce review overhead. |
| Nov 19, 2025 | PR #8070 | ByteHamster | Switching queues must change **the same screen** (no second fragment on top) and be **remembered**. "Add to queue" goes **straight to the active queue with no dialog**, because routing will be automated later. Changing behavior later causes complaints, adding features later does not. **Edit only the English strings file.** |
| Jan 2026 | PR #8215 | ByteHamster | Unrelated changes block a merge. Features need an approved issue first. |
| Always | `AGENTS.md`, `CONTRIBUTING.md` | project | Minimal diff: no renames, no reordering, no drive-by fixes. New strings only in `ui/i18n/.../values/strings.xml`. Use imports, not fully qualified names. Run `checkstyle lint spotbugs`. Add tests for core features. Follow the PR template, keep the description to 2–8 sentences, and include `Closes: #2648`. |

### 1.2 Distilled requirements

| # | Requirement | Source |
|---|---|---|
| R1 | Extend the existing queue (same table, same `DBReader`/`DBWriter` API). No parallel queue system. | #2648 (Oct 2025), #8066 |
| R2 | Manual queues only. No automation, no per-podcast routing, no smart queues. | #2648 (2022, 2025) |
| R3 | One active queue. It is the target for playback, manual adds and automatic adds. No picker dialog. | #8070 |
| R4 | Switching changes the existing Queue screen in place and is remembered. | #8070 |
| R5 | No added UI complexity with a single queue. Queue names and switching UI only appear once there are two or more queues. | #3221, #2648 (keunes) |
| R6 | No navigation drawer changes for management. | #2648 (2022) |
| R7 | No master switch. Ship as a complete MVP. | #2648 (Oct 2025) |
| R8 | Bare minimum: no colors, icons or per-queue settings in v1. | #8066 |
| R9 | English strings only. No unrelated changes. Minimal diff. | #8070, #8215, `AGENTS.md` |
| R10 | Automated tests, because this is a core feature. | PR template, `CONTRIBUTING.md` |

### 1.3 Where maintainers disagreed or changed their minds

The **most recent decision wins**: ByteHamster's #8070 comment of Nov 19, 2025, made right after the team call. Earlier ideas it overrides:

- **Default queue with a picker for manual actions** (keunes, Oct 2025). Replaced by "active queue, no dialog".
- **A screen that lists all queues** (keunes, forum 2023). Replaced by switching in place on the Queue screen.
- **Per-queue sort and lock settings** (keunes, Oct 2025). Deferred by R8. v1 keeps sort and lock global. This is a candidate follow-up, see §10.
- **Show the active queue on the player screen and notification** (keunes). Deferred. v1 shows it on the Queue screen only.

---

## 2. Why the existing PRs can't be finished as-is

**#8070**

- It builds a parallel `Queues`/`QueueItems` system with 21 temporary `df_*` methods (55 references), next to the old `Queue` table. This contradicts R1.
- It has no database upgrade path, so existing users would hit missing tables and lose their queue.
- Its delete-queue SQL is broken (`"queue_id=?" + queueId` combined with a bind argument).
- Its UX is a picker dialog, which contradicts R3 and R4.
- It modified 22 translation files, which contradicts R9.
- It is 338 commits behind `develop`.

**#8066**

- It was rejected over its scope and because the code was AI-written without regard for the existing code (a custom repository and executor instead of `DBReader`/`DBWriter`).

**What to take from them.** Only the UX lessons. No code needs to be carried over.

---

## 3. Scope

**In v1**

- Create, rename and delete queues. The default queue cannot be deleted.
- A queue switcher on the Queue screen, visible only when there are two or more queues. The active queue is remembered.
- All existing queue actions (add, remove, move, sort, clear, lock) operate on the active queue.
- Playback continues within the active queue.
- A database migration that keeps the current queue and its order.
- Tests.

**Not in v1** (follow-up PRs, each needs its own discussion)

- Per-podcast or tag-based routing into queues, and smart queues (#307).
- Choosing a queue in "Add to queue", or moving an episode between queues.
- Per-queue keep-sorted, lock and enqueue location.
- Showing the queue on the player, notification or widget, or switching from there.
- Android Auto or Wear browsing of queues other than the active one.
- Queue ordering or rotation ("when A is empty, continue with B").
- Renaming the default queue. It shows the translated "Queue" label.
- An episode in more than one queue at the same time.

---

## 4. Behavior specification

There are two distinct concepts:

- **"Queued"** means the episode is in *some* queue. The existing `is_in_queue` column, `FeedItem.TAG_QUEUE` and `FeedItemFilter.QUEUED` keep this meaning, unchanged.
- **"The queue"** means the ordered list of the **active** queue. `DBReader.getQueue()`, `getQueueIDList()`, `getNextInQueue()`, `getPausedQueue()` and the queue size return this.

**An episode is in at most one queue in v1** (decision D1). This keeps "queued", "remove from queue", the in-queue icon, auto-download and the cleanup algorithms unambiguous without touching them. The schema still allows relaxing this later.

| Area | v1 behavior | Code change? |
|---|---|---|
| Queue screen | Shows the active queue. The switcher chips and the queue name as title appear only with 2+ queues. | `QueueFragment` |
| Add to queue: menu, swipe, multi-select | Adds to the active queue. Items already queued anywhere are skipped (same no-op as today's "already in queue"). | `DBWriter` only |
| Auto-enqueue: downloaded episodes, "add new episodes to queue", starting playback of an unqueued episode | Active queue. | none (goes through `DBWriter.addQueueItem`) |
| Remove from queue: menu, swipe, multi-select, "delete removes from queue", finished playback, played on another device via sync | Removes from whichever queue holds the episode. | `DBWriter` only |
| Delete episode or podcast | Removed from every queue. | `DBWriter` only |
| Move, sort, clear, lock, keep sorted | Act on the active queue. Lock and keep-sorted stay global preferences. | `DBWriter` only |
| Enqueue location (front, back, after current, random) | Computed within the active queue. | none |
| Continuous playback | Next episode = next item in the **active** queue. If the finished episode is not in the active queue, playback stops (D2). | `PodDBAdapter` query |
| Switching queues while something plays | Current episode keeps playing. What comes next follows the rule above. | none |
| Nav drawer badge, Home "continue listening", Wear, Android Auto queue list | Active queue. | none |
| Android Auto queue item count | Currently counts all queued episodes. Change it to count the active queue, to match the list. | 1 line |
| Auto-download "queued episodes", cleanup "keep queued", episode filters "In queue" / "Not in queue" | Any queue. | none |
| Search from the Queue screen (the `QUEUED` filter chip) | Matches episodes in any queue (D5). | none |
| Sync (gpodder.net, Nextcloud) | No queue sync exists today (episode actions only), so there is nothing to decide in v1. This answers keunes' sync question. | none |
| Database export and import | Queues are stored in the database, so they are included. A stale active-queue preference falls back to the default queue. | `DBReader` |
| Delete a queue | Confirmation dialog. The queue's episodes become unqueued (like "Clear queue"). If it was active, the default queue becomes active (D3). | new |

---

## 5. Data model and migration

### 5.1 Schema

**`Queue`** (existing table, extended):

```
id INTEGER PRIMARY KEY, feeditem INTEGER, feed INTEGER,
queue INTEGER DEFAULT 0,        -- new: 0 = default queue
position INTEGER DEFAULT 0      -- new: order within its queue
```

Today `Queue.ID` doubles as the position: `setQueue()` writes `ID = i`, and every query orders by `Queue.ID`. That cannot work across several queues, because the ids would collide. A separate `position` column fixes this explicitly.

**`Queues`** (new table): `id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT`. It holds only user-created queues.

**The default queue is implicit.** It has id `0` and no row. This means:

- The migration doesn't need to insert anything.
- The default queue can't be deleted by accident.
- An unset preference already means "default".
- `deleteDatabase()` in tests needs no special handling.

The name follows existing key conventions: `KEY_QUEUE = "queue"`, like the existing `feed` and `feeditem` columns, and it reuses `KEY_POSITION` and `KEY_TITLE`.

### 5.2 Upgrade (`DBUpgrader`)

```java
if (oldVersion < 3130000) {
    db.execSQL(PodDBAdapter.CREATE_TABLE_QUEUES);
    db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_QUEUE
            + " ADD COLUMN " + PodDBAdapter.KEY_QUEUE + " INTEGER DEFAULT 0");
    db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_QUEUE
            + " ADD COLUMN " + PodDBAdapter.KEY_POSITION + " INTEGER DEFAULT 0");
    db.execSQL("UPDATE " + PodDBAdapter.TABLE_NAME_QUEUE
            + " SET " + PodDBAdapter.KEY_POSITION + " = " + PodDBAdapter.KEY_ID);
}
```

- `onCreate` creates both tables in their new shape.
- `CREATE_TABLE_QUEUES` is package-private static, the same as `CREATE_TABLE_FAVORITES`, which `DBUpgrader` already uses.
- **Version coordination:** the approved PR #8718 (database indexes) already bumps `VERSION` to `3130000` with its own `< 3130000` block. If 3.13 hasn't shipped yet when this merges, add these statements to that block. Otherwise use the next release code. Rebase onto #8718 before opening the PR.

---

## 6. Code changes by module

The list is complete for production code. Everything not listed stays untouched.

### `storage/database`

**`PodDBAdapter`**

- Add constants (`TABLE_NAME_QUEUES`, `KEY_QUEUE`) and `CREATE_TABLE_QUEUES`. Add the new columns to `CREATE_TABLE_QUEUE`, add the table to `ALL_TABLES` and to `onCreate`.
- Add a `long queueId` parameter to: `setQueue`, `clearQueue`, `getQueueCursor`, `getQueueIDCursor`, `getNextInQueue`, `getPausedQueueCursor`, `getQueueSize`.
- `setQueue` deletes the rows of that queue and inserts `queue` and `position = i` instead of an explicit `ID`.
- Change `ORDER BY Queue.ID` to `ORDER BY Queue.position`.
- `getNextInQueue` filters both the outer query and the subselect on `queue = ?`.
- New methods: `getQueuesCursor()`, `insertQueue(title)`, `setQueueTitle(id, title)`, `removeQueue(id)`. `removeQueue` deletes the row and its `Queue` rows in one transaction.
- `SELECT_KEY_IS_IN_QUEUE`, `getAutoDownloadCandidatesCursor` and `FeedItemFilterQuery` stay unchanged ("any queue").

**`DBReader`**

- `getActiveQueueId()` returns the preference value if that queue exists, otherwise `0`. This is the only place the fallback rule lives.
- `getQueues()` returns the default queue plus the stored ones.
- `getQueue(long queueId)` is new. The existing no-argument `getQueue()`, `getQueueIDList()`, `getNextInQueue(item)`, `getPausedQueue(limit)`, `getRemainingQueueSize()` and the nav drawer `queueSize` keep their signatures and delegate to `getActiveQueueId()`.

**`DBWriter`**

- Active-queue operations read `long queueId = DBReader.getActiveQueueId()` **once** per task and use it for both the read and the write: `addQueueItemAt`, `addQueueItem`, `applySortOrder`, `clearQueue`, `moveQueueItem`, `moveQueueItemsSynchronous`, `reorderQueue`. These already run on the single `dbExec` thread, so this is race-free.
- `addQueueItem` and `addQueueItemAt` skip items that are already in any queue (D1).
- `removeQueueItemSynchronous` and `deleteFeedItemsSynchronous` loop over `DBReader.getQueues()` and remove the items from whichever queue holds them. They keep today's events (`REMOVED`, `IRREVERSIBLE_REMOVED`).
- New methods, all `runOnDbThread`: `createQueue(title)`, `renameQueue(id, title)`, `deleteQueue(id)` and `switchQueue(id)`. `switchQueue` writes the preference inside the DB task, so it can't interleave with a queue write. Each posts `QueueEvent.queuesChanged()`.

### `storage/preferences`

- `UserPreferences`: `PREF_ACTIVE_QUEUE = "prefActiveQueue"` (long, default `0`) with a getter and setter.

### `model`

- A new `Queue` class with `id` and `title`, where `title` is null for the default queue, and a constant `DEFAULT_QUEUE_ID = 0`. Package to be decided by the reviewer, e.g. `model.feed` or a new `model.queue`.

### `event`

- `QueueEvent`: one new action `QUEUES_CHANGED` and a factory `queuesChanged()`.
- Existing subscribers already react correctly:
  - `NavDrawerFragment` reloads on any action except moved, sorted and deleted media.
  - `QueueSection` reloads on every event.
  - `QueueFragment` gets an explicit `case QUEUES_CHANGED: loadItems()`.
- `SET_QUEUE` is **not** reused for switching, because `moveQueueItemsSynchronous` depends on its current in-place semantics.

### `app`

- `QueueFragment`, `queue_fragment.xml`, `menu/queue.xml`, a new `QueueChipAdapter` and a context menu resource. Details in §7.

### `playback/service`

- `MediaLibrarySessionCallback`: the Android Auto queue item count uses `DBReader.getQueueIDList().size()` instead of the `QUEUED` filter count. This is the only playback change. **Both playback services (the old one and Media3) stay untouched**, because both call `DBReader.getNextInQueue(item)`.

### `ui/i18n`

- English only. About 6 new strings: new queue, rename queue, delete queue, delete confirmation, queue name hint, and the default queue label if `queue_label` can't be reused.

**Untouched on purpose:** `FeedItemMenuHandler`, `EpisodeMultiSelectActionHandler`, the swipe actions (except the one-line undo guard in `RemoveFromQueueSwipeAction`, see §11), `DownloadServiceInterfaceImpl`, `FeedDatabaseWriter`, `SyncService`, both playback services, the cleanup and auto-download algorithms, `FeedItemFilterQuery`, `WearListenerService`, `QueueSection`, the widget and the drawer.

---

## 7. UI (Queue screen only)

The pattern already exists in the app: the **tag chips on the Subscriptions screen**. They use `SubscriptionTagAdapter` and `item_tag_chip.xml`, a long-press context menu handled by `TagMenuHandler`, and a remembered selection. Reusing this pattern keeps review cheap and meets R4, R5 and R6.

**One queue (default state).** The screen is identical to today, plus one overflow menu item, **"New queue"**, at the bottom of the menu. Creating the second queue needs *some* entry point, and this is the smallest one.

**Two or more queues:**

- A horizontal chip row sits under the info bar, one chip per queue, with the active queue checked. Tapping a chip calls `DBWriter.switchQueue(id)`.
- The toolbar title shows the active queue's name. The default queue shows the existing translated `queue_label`.
- Long-pressing a chip (except the default queue) opens a context menu with Rename and Delete, like the tag chips.
- **Rename** uses `EditTextDialogBinding` with `MaterialAlertDialogBuilder`, the same as `RenameFeedDialog`. Empty names are rejected.
- **Delete** uses a `ConfirmationDialog` that says the queue's episodes will be removed from the queue (the same pattern as deleting a tag).
- After a switch, reset the saved scroll position. Restoring queue A's scroll offset on queue B would be wrong.
- New resource ids use lowerCamelCase as the code style page asks (e.g. `queuesRecycler`), even though older ids in the same layout use snake_case. Don't rename existing ids.

**Alternative if reviewers want even less code:** instead of chips, add a "Switch queue" overflow item (visible only with 2+ queues) that opens a single-choice dialog. It's smaller, but it gives a weaker "which queue am I in" signal. Recommendation: chips.

---

## 8. Tests

Existing Robolectric database tests live in `net/download/service/src/test/.../autodownload/` (`DbWriterTest`, `DbReaderTest`, `DbCleanupTests`).

**Mechanical updates.** The 12 calls to `adapter.setQueue(list)` in tests and `androidTest` utils become `adapter.setQueue(0, list)`. Every existing queue test must pass with unchanged semantics.

**New tests:**

- `addQueueItem` adds to the active queue. After `switchQueue`, `getQueue()` returns the other queue.
- `addQueueItem` skips an item that is already in another queue.
- `removeQueueItem` removes an item that sits in a non-active queue.
- `deleteFeed` and `deleteFeedItems` remove the items from all queues. No orphan rows remain.
- `getNextInQueue` stays within the active queue and returns `null` for an item not in it.
- `moveQueueItem`, `clearQueue` and `reorderQueue` don't touch other queues.
- `deleteQueue` removes its rows. If it was active, `getActiveQueueId()` becomes `0`.
- `getActiveQueueId()` returns `0` for an unknown id (the stale preference after an import).

**Migration.** There is no `DBUpgrader` test precedent, so test it manually:

1. Install the current release and build a queue of about 20 episodes.
2. Export the database.
3. Install the debug build, or import the database into it.
4. Verify the order, the in-queue icons and continuous playback.

Optional: a Robolectric test that builds the old schema and runs `DBUpgrader.upgrade`.

**Commands** (exactly as in `AGENTS.md` and `CONTRIBUTING.md`):

- `./gradlew :app:assembleDebug`
- `./gradlew --console=plain :net:download:service:test`
- `./gradlew checkstyle lint spotbugsPlayDebug spotbugsDebug`

---

## 9. Delivery

1. **Announce first.** #2648 is locked to collaborators, so comment on the closed #8070. Tag @ByteHamster and @dominikfill, say you're picking this up from `develop`, and list the decisions in §10 in a few lines. This follows the `CONTRIBUTING.md` rule to say you're working on something before starting.
2. Create a branch from `develop`, e.g. `multiple-queues`, and rebase it onto #8718 if that has merged.
3. **Commit 1, storage.** Schema, migration, the queue-scoped `PodDBAdapter`/`DBReader`/`DBWriter`, the preference, the model class, the `QueueEvent` action, and all tests. With no UI yet, the app behaves exactly as before: only queue `0` exists.
4. **Open a draft PR now** (per ByteHamster: "open the PR as early as possible"). Ask whether they want commit 1 merged on its own, since in 2022 they welcomed behind-the-scenes changes first.
5. **Commit 2, UI.** The chips, the menu item, the rename/delete dialogs, the strings, and the Android Auto count.
6. Self-review line by line (a PR checklist item). Run the checks from §8. Fill the PR template: 2–8 sentences, `Closes: #2648`, and mention that it continues the direction agreed in #8070.
7. Respond to review with one summary comment rather than per-thread replies, as `AGENTS.md` asks.

**On AI assistance.** The team closed #8066 largely because the code looked AI-written without regard for existing code. Whatever tooling you use, the diff has to read like it was written by someone who knows the codebase: reuse existing methods, keep the diff minimal, add no comments, rename nothing.

**Effort** (part-time):

| Work | Estimate |
|---|---|
| Commit 1 with tests | 2–3 evenings |
| Commit 2 | 2–3 evenings |
| Review rounds | depends on maintainer availability |

---

## 10. Decisions to confirm with maintainers before coding

| # | Question | Recommendation | Why |
|---|---|---|---|
| D1 | Can an episode be in several queues? | **No, not in v1.** Adding an already-queued episode is a no-op. | Keeps "queued", "remove from queue", the in-queue icon and the cleanup and auto-download logic unchanged. keunes deferred multi-membership in the forum design thread. It can be allowed later without a schema change. |
| D2 | Next episode when the finished one isn't in the active queue? | **Stop**, the same as `getNextInQueue` returning `null` today. | ByteHamster: the active queue is "for playback". It adds no new "jump to the head of the queue" logic. Alternative: continue in the episode's own queue. |
| D3 | Deleting a queue: what happens to its episodes? | **Unqueue them** (like "Clear queue"), after confirmation. | Mirrors existing semantics and doesn't silently grow the default queue. Alternative: move them to the default queue. |
| D4 | Keep-sorted and lock: global or per queue? | **Global in v1.** | Per R8, a minimal MVP. keunes expects per-queue settings, so this is a natural follow-up. |
| D5 | Search from the Queue screen: active queue or all queues? | **All queues in v1** (unchanged filter). | Scoping it needs the active queue id inside `FeedItemFilterQuery`, which is more change than it's worth now. |
| D6 | Switcher: chips or dialog? | **Chips** (the Subscriptions tag pattern). | Also answers "how do we show the active queue". |
| D7 | Can the default queue be renamed? | **No in v1.** It shows the translated "Queue". | keunes wanted names hidden until 2+ queues exist. The implicit default has no row. |

---

## 11. Risks and edge cases

- **Undo of the "remove from queue" swipe in lists filtered by "In queue" or "Not in queue".** Those lists offer an undo snackbar. `RemoveFromQueueSwipeAction` looks up the old index in the *active* queue. For an episode from another queue the index is `-1`, and `addQueueItemAt(-1)` would throw on undo. Fix: offer undo only when `position >= 0`. That's a one-line guard.
- **Conflicts with open PRs.** #8718 changes the version and `PodDBAdapter`. #8466 (queue on car head units) touches the Android Auto queue exposure. #8215 (per-podcast enqueue location, "Needs: Decision") overlaps with future routing. Rebase late and keep the diff small.
- **Episode-count sleep timer.** `getRemainingQueueSize()` counts within the active queue. That's consistent with D2 (0 remaining if the playing episode isn't in it).
- **Keep-sorted with switching.** A queue is re-sorted only on its next add, which matches today's behavior.
- **Performance.** Queue tables are tiny. The existing `Queue(feeditem)` index is enough, so no new index is needed.
- **Stale active id** after a database import or a queue deletion is handled in one place, `DBReader.getActiveQueueId()`.

---

## Sources

- Issue #2648, full thread (PDF provided): https://github.com/AntennaPod/AntennaPod/issues/2648
- PR #8070 (dominikfill), maintainer comments Nov 2025: https://github.com/AntennaPod/AntennaPod/pull/8070
- PR #8066 (seefood), review Oct 2025: https://github.com/AntennaPod/AntennaPod/pull/8066
- PR #3221 (2019 constraints): https://github.com/AntennaPod/AntennaPod/pull/3221
- PR #8215 (Needs: Decision, unrelated changes): https://github.com/AntennaPod/AntennaPod/pull/8215
- PR #8718 (database version bump to 3130000): https://github.com/AntennaPod/AntennaPod/pull/8718
- Forum, "needs decision" call Nov 2025: https://forum.antennapod.org/t/needs-decision-meeting-updates/4169/34
- Forum, multiple queues design thread (2023): https://forum.antennapod.org/t/multiple-queues-impact-and-feature-mapping/2670
- Code style: https://antennapod.org/contribute/develop/app/code-style
- Repository: `AGENTS.md`, `CONTRIBUTING.md` and `.github/pull_request_template.md` at `develop@34de86f`
