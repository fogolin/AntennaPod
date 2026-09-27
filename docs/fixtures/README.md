# Test fixtures and manual scenarios

## Automated (Robolectric)

- **Location:** `net/download/service/src/test/java/de/danoeh/antennapod/net/download/service/episode/autodownload/`
- **Database per test:** `PodDBAdapter.init(context)`, then `PodDBAdapter.deleteDatabase()`, which empties every table in `ALL_TABLES`, including `Queues`. `tearDown` calls `DBWriter.tearDownTests()` and `PodDBAdapter.tearDownTests()`.
- **Builder:** `DbTestUtils.saveFeedlist(numFeeds, numItems, withMedia)` saves feeds with items.
- **Queue fixtures:**
  - The helpers in the upstream tests write queues with `adapter.setQueue(queueId, list)`. Queue `0` is the default queue.
  - Multi-queue tests create queues with `adapter.insertQueue(title)`, which returns the new id, and switch with `UserPreferences.setActiveQueueId(id)`.
- **Preferences:** Robolectric gives each test a fresh `SharedPreferences`, so `prefActiveQueue` starts at `0`.

## Test classes added by this fork

| Class | Module | What it builds |
|---|---|---|
| `DbMultipleQueuesTest` | `net/download/service` (test) | One feed with 6 items that have media (`saveFeedlist(1, 6, true)`). Queues are created with `adapter.insertQueue`, filled with `adapter.setQueue(queueId, ...)`, and checked by reading `getQueueIDCursor(queueId)` in order. The test registers itself on `EventBus` and collects `QueueEvent`s, to check that adds to a queue that isn't active post none. |
| `DbMultipleQueuesTest`, phase 6 tests | same | Move tests fill two queues with `adapter.setQueue` and call `DBWriter.moveToQueue(...).get()`. No flush is needed: `runOnDbThread` runs the add inline on the DB thread, so the `Future` covers both steps. `testHasCustomQueuesFollowsCreateAndDelete` only asserts after its own create and delete, because `hasCustomQueues` is static. |
| `DbMultipleQueuesTest.testNewEpisodesGoToFeedQueue` (phase 5) | same | Sets the podcast to "Add to queue", automatic download off, and a second queue. Then it runs `FeedDatabaseWriter.updateFeed` with a copy of the podcast that has one episode dated 30 days ahead, so it counts as new. `updateFeed` doesn't wait for the enqueue, so the test then calls `DBWriter.addQueueItem(context)` with no items and waits on it: the DB thread is first-in, first-out, so the enqueue has finished by then. |
| `QueueFragmentTest.testCreateRenameAndDeleteQueue` | `app` (androidTest, emulator) | Starts from a cleared database and preferences on the Queue screen, then drives the ⋮ menu: empty name, create "Second", rename to "Renamed", delete. It waits for the chip text to appear and disappear. |
| `DBUpgraderMultipleQueuesTest` | `storage/database` (test) | An in-memory SQLite database with the **upstream** `Queue` schema (`id, feeditem, feed`) and two rows, plus a minimal `Feeds` table with one podcast (phase 5), then runs `DBUpgrader.upgradeMultipleQueues`. It also simulates the official app writing rows without the `queue` and `feed_queue` columns. |

## Manual scenarios

Each phase log has its own checklist. The shared setup:

| Scenario | How to build it |
|---|---|
| Real data | Export the database from the official app and import it into the debug build (see [maintenance/fork-sync.md](../maintenance/fork-sync.md)). |
| Long queue | Subscribe to a podcast with many episodes and multi-select "Add to queue" on 20 or more. |
| Round trip | Export from the debug build, import into the official app, check the queue, export again, import back into the debug build. |
| Queue picker (phase 6) | Two queues with a few episodes each. See the checklist in [phase-6-queue-picker.md](../phases/phase-6-queue-picker.md). |
| Podcast queue (phase 5) | Two queues, one podcast set to "Add to queue" with "Queue for new episodes" on the queue that isn't active. See the checklist in [phase-5-routing.md](../phases/phase-5-routing.md). |
