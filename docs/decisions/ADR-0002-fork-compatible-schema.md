# ADR-0002: Fork-compatible schema (same DB version, guarded migration, order by row id)

- **Status:** accepted, 2026-09-26. Supersedes plan §5 (the `position` column and the `DBUpgrader` version step) **for the fork**.
- **Context:** The fork is used daily, next to or instead of the official app. `DatabaseExporter.importBackup` refuses a database with a higher version than the app's, and Android can't downgrade. If the fork bumped `PodDBAdapter.VERSION`:
  - its database could never be imported back into the official app;
  - its version numbers would collide with upstream's (#8718 already claims `3130000`).

## Decision

1. **The fork keeps upstream's `VERSION`.** The schema additions run in `PodDBHelper.onOpen` through `DBUpgrader.upgradeMultipleQueues(db)`. That method checks `PRAGMA table_info(Queue)` and only adds what's missing, so it's idempotent and runs its DDL at most once per database.
2. **Schema additions:**
   - `Queue.queue INTEGER DEFAULT 0`. `0` is the default queue, and every existing row migrates into it with no data rewrite.
   - A new table `Queues(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT)` for user-created queues.
   - Fresh installs get both through `onCreate`.
3. **There is no `position` column. Order stays `ORDER BY Queue.id`, as upstream does.** `setQueue(queueId, list)` deletes that queue's rows and inserts the list **without** an explicit `id`. SQLite then assigns `max(rowid) + 1` to each row, so ids increase in list order within a queue. This is documented SQLite behavior for `INTEGER PRIMARY KEY` without `AUTOINCREMENT`, and it only changes when the maximum id is 2^63−1.
4. The default queue is implicit (id `0`, no row). See [ADR-0003](ADR-0003-implicit-default-queue.md).

## Consequences

**Round trip with the official app works both ways:**

- The official app ignores the extra column and table.
- It shows the rows of *all* queues in one list, ordered by id, because it never filters on `queue`.
- On its first queue edit it rewrites everything into queue `0` (it runs `DELETE FROM Queue` and re-inserts `id = 0..n-1`).
- Back in the fork, those rows are simply in the default queue, and custom queues still exist but are empty.
- No crash and no lost episodes. Queue membership collapses into the default queue.

**Ids grow with every rewrite.** They are 64-bit, and at 20,000 rows rewritten per day it would take about 10^12 years to reach the limit.

**For an upstream PR,** the same statements move into a normal `if (oldVersion < N)` block in `DBUpgrader.upgrade`, and the `onOpen` hook is dropped. Keeping the migration in one method makes that a small change.

## Rejected alternative: a `position` column (the original plan)

It's more explicit, but it breaks the official-app round trip. Rows written by the official app all get `position = 0`, so the fork loses their order and `getNextInQueue` stops early. It's also a bigger diff: every `ORDER BY` and the `getNextInQueue` subselect would have to change.
