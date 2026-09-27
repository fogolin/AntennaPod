# Phase 1: storage layer

- **Branch:** `mq/phase-1-storage`, as PR #2 into `multiple-queues`. It was opened on top of phase 0 and retargeted after PR #1 was merged.
- **Date:** 2026-09-26
- **User-visible changes:** none. The app behaves exactly like upstream while only the default queue exists. The UI to create and switch queues is phase 2.

## What changed

| File | Change |
|---|---|
| `model/.../model/queue/Queue.java` | **New.** `id`, `title` (null for the default queue), and `DEFAULT_QUEUE_ID = 0`. |
| `storage/database/.../PodDBAdapter.java` | `KEY_QUEUE`, `TABLE_NAME_QUEUES` and `CREATE_TABLE_QUEUES`. The `queue` column is added to `CREATE_TABLE_QUEUE`, and `Queues` is added to `ALL_TABLES` and `onCreate`. The `onOpen` hook runs the fork-safe migration. These queue methods take a `queueId`: `setQueue`, `clearQueue`, `getQueueCursor`, `getQueueIDCursor`, `getNextInQueue`, `getPausedQueueCursor`, `getQueueSize`. New methods: `removeQueueItems`, `insertQueue`, `setQueueTitle`, `removeQueue`, `getQueuesCursor`, `getQueuedItemIdsCursor`, `queueExists`, and a `getItemIds(long...)` overload. `removeFeedItems` also deletes `Queue` rows. |
| `storage/database/.../DBUpgrader.java` | `upgradeMultipleQueues(db)`: guarded, idempotent schema additions (ADR-0002). |
| `storage/database/.../DBReader.java` | The existing queue readers keep their signatures and use `getActiveQueueId()`. New: `getQueue(long)`, `getActiveQueueId()`, `getQueues()`, `getQueuedItemIds(long...)`. |
| `storage/database/.../DBWriter.java` | Active-queue operations read the id once per task. Adds skip items that are queued anywhere (ADR-0004). Removal and episode deletion delete rows directly, across all queues. New: `createQueue` (which also activates the new queue), `renameQueue`, `deleteQueue` (ignored for the default queue, and falls back to it if the deleted queue was active), `switchQueue`. |
| `storage/preferences/.../UserPreferences.java` | `prefActiveQueue` with `getActiveQueueId()` and `setActiveQueueId(long)`. |
| `event/.../QueueEvent.java` | `QUEUES_CHANGED` and `queuesChanged()`. |
| Tests | 12 existing calls now pass `Queue.DEFAULT_QUEUE_ID`. New `DbMultipleQueuesTest` (15 tests) and `DBUpgraderMultipleQueuesTest` (3 tests). |

## Reuse check

Existing code was searched before anything new was written. See [findings/codebase-map.md](../findings/codebase-map.md).

- No new layer or executor: everything goes through `PodDBAdapter`, `DBReader` and `DBWriter` (ADR-0001).
- The model class follows `model.download.ProxyConfig`.
- The id helpers reuse `LongList` and the existing `getItemIds` pattern.
- The migration follows the `CREATE_*` constants that `DBUpgrader` already shares.
- The event is a new action on `QueueEvent`, not a new class.

## Behavior changes compared with upstream, all intentional

1. **Remove from queue** deletes the rows directly instead of rewriting the queue. The order of the remaining items is unchanged, because order is by row id.
2. **An item that can't be loaded** is still removed from the queue. Upstream left the row behind.
3. **`setQueue` no longer writes an explicit `id`.** Row ids grow, and order within a queue is insertion order (ADR-0002).
4. **Deleting episodes** also clears their `Queue` rows inside `removeFeedItems`.

## Security and performance

- [security/phase-1.md](../security/phase-1.md): no open findings. Title validation and hiding "delete" for the default queue are handed to phase 2.
- [perf/phase-1.md](../perf/phase-1.md): removing from the queue and deleting episodes no longer load the whole queue.

## How to test

**Automated** (runs in CI on the PR):
- `DbMultipleQueuesTest` and `DBUpgraderMultipleQueuesTest`
- all existing `DbWriterTest`, `DbReaderTest` and cleanup tests
- checkstyle and lint

**Manual** (with the APK from the PR's CI run: Actions, then the run, then Artifacts, then `app-play-debug.apk`):

1. Install it. It installs as a separate app (application id `de.danoeh.antennapod.debug`) with the same name and icon, and doesn't touch the official app. Tell them apart in Android's app info screen.
2. In the official app: Settings → Backup & restore → Database export. Then in the debug app: Database import, and restart.
3. Check the Queue screen: same episodes, same order.
4. Drag to reorder. Move to top and bottom. Sort.
5. Add an episode from the episode list, and remove one by swipe and by menu.
6. Play an episode and let it finish, or skip to the end. The next queue item should start (continuous playback).
7. Check the drawer or bottom-navigation queue count, and "Continue listening" on the Home screen.
8. Delete a podcast that has episodes in the queue. They should disappear from the queue.
9. Optional round trip: export the database from the debug app and import it into the **debug** app again, or into the official app if you've kept a backup. The queue should be intact.

There's nothing multi-queue to click yet. Multiple queues are only reachable through the tests until phase 2.

## Results

**CI run [36288029928](https://github.com/fogolin/AntennaPod/actions/runs/36288029928)** on commit `a3a7980`: **all green.**

| Check | Result |
|---|---|
| Gradle wrapper validation | pass |
| Static analysis (XML format, checkstyle, lint) | pass |
| Unit tests PlayDebug (all modules, including the 18 new tests) | pass |
| Unit tests PlayRelease | pass |
| FreeRelease build | pass |
| Emulator tests, API 23/30/36, debug and release | pass (5/5) |

The artifact `app-play-debug.apk` (about 21 MB) is attached to that run.

- **Manual test:** pending the user.
