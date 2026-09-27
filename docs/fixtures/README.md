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
| `DbMultipleQueuesTest` | `net/download/service` (test) | One feed with 6 items that have media (`saveFeedlist(1, 6, true)`). Queues are created with `adapter.insertQueue`, filled with `adapter.setQueue(queueId, ...)`, and checked by reading `getQueueIDCursor(queueId)` in order. |
| `DBUpgraderMultipleQueuesTest` | `storage/database` (test) | An in-memory SQLite database with the **upstream** `Queue` schema (`id, feeditem, feed`) and two rows, then runs `DBUpgrader.upgradeMultipleQueues`. It also simulates the official app writing rows without the `queue` column. |

## Manual scenarios

Each phase log has its own checklist. The shared setup:

| Scenario | How to build it |
|---|---|
| Real data | Export the database from the official app and import it into the debug build (see [maintenance/fork-sync.md](../maintenance/fork-sync.md)). |
| Long queue | Subscribe to a podcast with many episodes and multi-select "Add to queue" on 20 or more. |
| Round trip | Export from the debug build, import into the official app, check the queue, export again, import back into the debug build. |
