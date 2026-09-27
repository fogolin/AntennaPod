# ADR-0001: Extend the existing `Queue` table and API

- **Status:** accepted, 2026-09-26
- **Context:** Upstream asked to "extend the existing system instead of adding a completely independent system" (ByteHamster, #2648, Oct 2025) and to use the existing `DBReader`/`DBWriter` (review of #8066). #8070 built a parallel `Queues`/`QueueItems` system and never got past that.

## Decision

- Multiple queues are rows of the **same** `Queue` table, told apart by a new `queue` column.
- A small `Queues` table holds the names of user-created queues.
- `PodDBAdapter` queue methods take a `queueId`.
- The public `DBReader` and `DBWriter` methods keep their signatures and act on the **active queue** (`DBReader.getActiveQueueId()`).
- Only the operations that must span queues loop over them: removing items, and deleting episodes or podcasts.

## Consequences

- About 40 call sites (playback, downloads, sync, menus, swipe actions, Android Auto, Wear, home) get multi-queue behavior without being edited.
- The diff stays concentrated in the database module, which matters both for upstream review and for fork merges.
- Every `DBWriter` queue task reads the active id **once** and uses it for both its read and its write. All of these run on the single `dbExec` thread, and switching also runs there, so a switch can't land between the read and the write.
