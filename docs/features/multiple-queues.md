# Feature: multiple queues

- **Upstream issue:** #2648
- **Status:** v1 is complete and merged (phases 1 and 2), and the user tested it on a phone. Installable releases come from phase 3. Phase 5 adds a queue per podcast for automatic adds, and phase 6 adds "Add to queue…" and "Move to queue…".

## What the user gets (v1)

- Create more queues next to the normal one, and rename or delete them.
- Switch between queues on the Queue screen. The app remembers the choice.
- The selected queue is the **active queue**. Playing, "Add to queue", auto-enqueue after download and "add new episodes to queue" all use it, unless the podcast has its own queue (phase 5).
- Nothing looks different until a second queue exists (apart from a "New queue" menu item).
- **Phase 5:** each podcast can send its automatically added episodes to a chosen queue (podcast settings → "Queue for new episodes"). See [ADR-0010](../decisions/ADR-0010-podcast-queue.md).
- **Phase 6:** "Add to queue…" and "Move to queue…" put episodes into any queue without switching first. They appear only once a custom queue exists. See [ADR-0011](../decisions/ADR-0011-queue-picker.md).

## Rules

There are two concepts, and the code keeps them apart:

- **Queued** means in *some* queue. Used by the in-queue icon, the "In queue" filter, auto-download of queued episodes and "don't auto-delete queued episodes".
- **The queue** means the ordered list of the *active* queue. Used by the Queue screen, playback order, Android Auto, Wear, the home section and the drawer badge.

| Action | Result | Where it's implemented |
|---|---|---|
| Add to queue (menu, swipe, multi-select) | Added to the active queue. Already queued anywhere means no-op. | `DBWriter.addQueueItem`/`addQueueItemAt` |
| Auto-enqueue: "Add to queue" new-episodes action, "Enqueue downloaded" | The podcast's queue, if one is set and still exists. Otherwise the active queue. | `FeedDatabaseWriter.updateFeed`, `DownloadServiceInterfaceImpl`, `DBWriter.addQueueItem(context, queueId, items)` |
| Playing an unqueued episode | Active queue. | callers unchanged |
| "Add to queue…" (phase 6) | The queue picked in the dialog. Already queued means skipped. | `EpisodeMultiSelectActionHandler`, `DBWriter.addQueueItem(context, queueId, items)` |
| "Move to queue…" (phase 6) | Removed from its queue and added to the picked one at the enqueue location. Already in the picked queue means unchanged. | `DBWriter.moveToQueue` |
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
- **Episode menus (phase 6, ADR-0011):** "Add to queue…" sits next to "Add to queue", and "Move to queue…" next to "Remove from queue", in the long-press menu, the episode screen and player ⋮ menu, and the multi-select speed dial. They're hidden until a custom queue exists. The picker leaves out the queue that already holds all the chosen episodes.
- **Podcast settings (phase 5, ADR-0010):** "Queue for new episodes" sits under Automation, below "New episodes action". The choices are "Active queue" (the default) and every queue. It's hidden while only one queue exists.

## Data model

- `Queue(id, feeditem, feed, queue)`: `queue` references `Queues.id`, and `0` is the default queue. Order is by `id` within a queue.
- `Queues(id, title)`: user-created queues only.
- Preference `prefActiveQueue` (long, default `0`).
- `Feeds.feed_queue` (phase 5): the podcast's queue for automatic adds. `-1` (`Queue.ACTIVE_QUEUE_ID`) means the active queue. It's reset to `-1` when that queue is deleted.
- Model class `de.danoeh.antennapod.model.queue.Queue(id, title)`, where `title` is null for the default queue.

## Phases

| Phase | Branch | Content | Status |
|---|---|---|---|
| 0 | `mq/phase-0-setup` | Docs, branches, research | merged (PR #1) |
| 1 | `mq/phase-1-storage` | Schema, migration, queue-scoped `DBReader`/`DBWriter`, preference, events, tests | merged (PR #2) |
| 2 | `mq/phase-2-ui` | Chips, new/rename/delete, strings, Android Auto count, swipe undo guard | merged (PR #3), tested on the user's phone |
| 5 | `mq/phase-5-routing` | A queue per podcast for automatic adds | merged (PR #7) |
| 6 | `mq/phase-6-add-to-queue` | "Add to queue…" and "Move to queue…" with a picker | in review |
