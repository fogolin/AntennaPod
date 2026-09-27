# Security review: phase 1 (storage)

- **Scope:** `PodDBAdapter`, `DBUpgrader`, `DBReader`, `DBWriter`, `UserPreferences`, `QueueEvent`, `model.queue.Queue`
- **Date:** 2026-09-26
- **Result:** no open findings. Two items are handed to phase 2 (F4, F5).

| # | Area | Finding | Status |
|---|---|---|---|
| F1 | SQL injection | Every value concatenated into SQL is a Java `long`: `queueId`, and item ids through `getItemIds(long...)` and `item.getId()`. A `long` can't carry SQL. Queue titles only go through `ContentValues` (`insertQueue`, `setQueueTitle`), so they're bound. `PRAGMA table_info(...)` only receives the constant `TABLE_NAME_QUEUE`. The `sqlite_master` lookup binds the table name as an argument. | OK |
| F2 | Migration data safety | `upgradeMultipleQueues` is additive only (`ALTER TABLE ... ADD COLUMN queue INTEGER DEFAULT 0` and `CREATE TABLE Queues`). It never rewrites or deletes rows. Each step is guarded (`PRAGMA table_info` and `sqlite_master`), so it's idempotent and recovers from a crash between the two steps. It's skipped when the helper falls back to a read-only database. Tested in `DBUpgraderMultipleQueuesTest`. | OK |
| F3 | Round trip with the official app | The database version is unchanged, so `importBackup` accepts the database in both directions. The official app ignores `Queue.queue` and `Queues`. Its `setQueue` deletes every row and re-inserts with no `queue` value, so rows fall back to `DEFAULT 0`. Membership collapses into the default queue, and no episode is lost. Covered by `testRowsWrittenWithoutQueueColumnGoToDefaultQueue`. | OK, documented in ADR-0002 |
| F4 | Input validation of queue titles | The storage layer accepts any string, including empty or very long ones, and null (the column allows NULL). An empty title would show as a blank chip. | **Phase 2:** the create/rename dialog must trim, reject empty names and cap the length. Rendering must be plain `setText` (no HTML). |
| F5 | Deleting the default queue | `DBWriter.deleteQueue(0)` is a no-op (tested). `PodDBAdapter.removeQueue(0)` itself isn't guarded and would empty the default queue. Only `DBWriter` calls it. | **Phase 2:** the UI must hide Delete for the default queue (same pattern as `TagMenuHandler`). |
| F6 | Concurrency | The active-queue preference is written only by `switchQueue`, `createQueue` and `deleteQueue`, all on `dbExec`. Queue mutations read the active id once per task on the same thread, so they can't interleave with a switch. UI reads on other threads only ever see the old or the new id, never a torn value (`SharedPreferences` is thread-safe). | OK |
| F7 | Stale or unknown active id | This can happen after a database import or a queue deletion. `DBReader.getActiveQueueId()` falls back to the default queue whenever the id isn't in `Queues`, so writes can never land in a queue that doesn't exist. | OK, tested |
| F8 | Orphan queue rows | `removeFeedItems` now also deletes `Queue` rows of deleted items. Upstream relied on `setQueue` of the single queue for this. | Improved over upstream |
| F9 | Exposure | No new components, permissions, intents or network calls. Android Auto, Wear and sync still see only one queue (the active one), as before. | OK |
| F10 | Backups | Queues live in the database, so they're included in export and import. The active-queue preference isn't part of the database export, and falls back to the default queue (F7). | OK |
| F11 | Size of `IN (...)` lists | Deleting a podcast builds `IN (id,...)` for all its items. That's the same order of size as the list upstream `removeFeedItems` already builds, and far below SQLite's 1 MB statement limit. | Accepted |
