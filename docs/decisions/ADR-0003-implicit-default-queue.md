# ADR-0003: The default queue is implicit (id 0, no row)

- **Status:** accepted, 2026-09-26

## Decision

- The queue that exists today is **queue id `0`**. It has no row in `Queues`.
- Its name is the existing translated `queue_label` ("Queue").
- It can't be deleted, and in v1 it can't be renamed.
- `Queue.DEFAULT_QUEUE_ID = 0`.

## Why

- **The migration inserts nothing.** Existing rows get `queue = 0` from the column default.
- **An unset `prefActiveQueue` already means the default queue.**
- **The fallback needs no special cases.** If the active id points to a queue that no longer exists (after a queue deletion or a database import), `DBReader.getActiveQueueId()` falls back to `0`, which always exists.
- **Test setup keeps working.** Upstream tests call `PodDBAdapter.deleteDatabase()`, which empties every table in `ALL_TABLES`. An implicit default queue survives that without any re-seeding.
- **It fits upstream's requests:** keunes asked to show queue names only once there are several, and ByteHamster asked to extend the existing queue rather than rename it.

## Consequence

Renaming the default queue (keunes' forum wish) would need either a row or a preference. It's deferred, see the [backlog](../future/backlog.md).
