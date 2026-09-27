# Feature: multiple queues

- **Upstream issue:** #2648
- **Status:** v1 is complete across phases 1 (storage) and 2 (UI), and both are awaiting the user's approval.

## What the user gets (v1)

- Create more queues next to the normal one, and rename or delete them.
- Switch between queues on the Queue screen. The app remembers the choice.
- The selected queue is the **active queue**. Playing, "Add to queue", auto-enqueue after download and "add new episodes to queue" all use it.
- Nothing looks different until a second queue exists (apart from a "New queue" menu item).

## Rules

There are two concepts, and the code keeps them apart:

- **Queued** means in *some* queue. Used by the in-queue icon, the "In queue" filter, auto-download of queued episodes and "don't auto-delete queued episodes".
- **The queue** means the ordered list of the *active* queue. Used by the Queue screen, playback order, Android Auto, Wear, the home section and the drawer badge.

| Action | Result | Where it's implemented |
|---|---|---|
| Add to queue (menu, swipe, multi-select) | Added to the active queue. Already queued anywhere means no-op. | `DBWriter.addQueueItem`/`addQueueItemAt` |
| Auto-enqueue (downloaded, new episode, playing an unqueued episode) | Active queue. | same (callers unchanged) |
| Remove from queue, "delete removes from queue", finished playback, played on another device | Removed from the queue that holds it. | `DBWriter.removeQueueItemSynchronous` |
| Delete episode or podcast | Removed from every queue. | `DBWriter.deleteFeedItemsSynchronous` |
| Move, sort, clear, lock, keep sorted | Active queue. Lock and keep-sorted are global preferences. | `DBWriter` |
| Next episode | Next in the active queue. If the finished episode isn't in it, playback stops. | `PodDBAdapter.getNextInQueue` |
| Switch queue | Takes effect immediately. Current playback continues. | `DBWriter.switchQueue` |
| Create a queue | The new queue becomes the active one, so the user lands in it. | `DBWriter.createQueue` |
| Delete a queue | After confirmation, its episodes are unqueued. If it was active, the default queue becomes active. | `DBWriter.deleteQueue` |
| Database import with an unknown active queue | Falls back to the default queue. | `DBReader.getActiveQueueId` |
| Round trip with the official app | Works. The official app shows all queues merged into one list. See ADR-0002. | schema |

## UI (phase 2, ADR-0007)

- **Queue screen:** a chip row under the header, shown only with 2+ queues. The checked chip is the active queue, and tapping another one switches.
- **⋮ menu:** "New queue" is always shown. "Rename queue" and "Delete queue" appear only when a custom queue is active.
- **Names:** trimmed, non-empty, at most 30 characters. The default queue is shown as "Queue" (the translated `queue_label`).
- **Android Auto:** the "Queue" node lists the active queue, and its count matches.

## Data model

- `Queue(id, feeditem, feed, queue)`: `queue` references `Queues.id`, and `0` is the default queue. Order is by `id` within a queue.
- `Queues(id, title)`: user-created queues only.
- Preference `prefActiveQueue` (long, default `0`).
- Model class `de.danoeh.antennapod.model.queue.Queue(id, title)`, where `title` is null for the default queue.

## Phases

| Phase | Branch | Content | Status |
|---|---|---|---|
| 0 | `mq/phase-0-setup` | Docs, branches, research | merged (PR #1) |
| 1 | `mq/phase-1-storage` | Schema, migration, queue-scoped `DBReader`/`DBWriter`, preference, events, tests | PR #2, CI green, awaiting approval |
| 2 | `mq/phase-2-ui` | Chips, new/rename/delete, strings, Android Auto count, swipe undo guard | implemented, awaiting approval |
