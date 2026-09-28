# Backlog

These items are deferred on purpose. Each one needs its own discussion before it's built.

## Multiple queues: after v1

| Item | Notes |
|---|---|
| ~~Per-podcast routing into queues~~ | Done in phase 5 for automatic adds ([ADR-0010](../decisions/ADR-0010-podcast-queue.md)). |
| Tag-based routing into queues | ByteHamster, 2022: prefer the tagging feature over new per-subscription state. Would sit on top of the phase 5 setting, and needs a tie-break for podcasts with two linked tags. |
| Several queues per podcast | keunes, Oct 2025. Needs a rule for which one an episode goes to. |
| Manual adds that follow the podcast's queue (optional setting) | The user chose automatic-only routing in phase 5. |
| Smart/automatic queues | Upstream #307. Explicitly after manual queues. |
| ~~"Move to queue…" / choose the queue when adding~~ | Done in phase 6 as extra actions ([ADR-0011](../decisions/ADR-0011-queue-picker.md)). |
| "New queue…" inside the queue picker | Left out in phase 6. Creating a queue also activates it today, which a picker flow shouldn't do. |
| Per-queue keep-sorted, sort order, lock and enqueue location | keunes expects each queue to keep its own settings. v1 keeps them global (ADR-0006, D4). |
| Show the active queue on the player screen, notification and widget, and switch from there | keunes' indicator list. Gap review A1: the top priority, since it's ByteHamster's first question. |
| Name the queue in "Added to …" messages and on the episode screen | Gap review A2. |
| A short UX proposal and a think-aloud study with the fork's APK | Gap review A3, ByteHamster 2026-09-28. |
| Moving a podcast's queued episodes when its queue setting changes | Gap review B4, ByteHamster 2019 (#3221). |
| Delete a queue and move its episodes to another queue | Gap review B5, Igor 2023. |
| Playlists that keep played episodes | Gap review B6. A different concept from a queue. |
| Translations of the fork's texts | Gap review B11. Would need fork-only files so the upstream translation files stay untouched. |
| Android Auto and Wear: browse all queues | v1 exposes the active queue only. Coordinate with upstream PR #8466. |
| Queue rotation (continue with queue B when A is empty) | kjetilk's use case. It would build on continuous playback. |
| Rename the default queue | ADR-0003. Needs a row or a preference. |
| Episode in several queues | ADR-0004. The schema already allows it. |
| Search from the Queue screen scoped to the active queue | ADR-0006, D5. |
| Show the active queue's name in the toolbar title | ADR-0007 keeps "Queue", because the checked chip shows it. Revisit if the chips scroll out of view in practice. |
| Reorder queues (chip order) | Chips follow creation order (`Queues.id`). |
| Friendlier handling of a `NULL` or over-long name from an imported database | Security review phase 2, G2. Cosmetic only. |
| Survive a read-only first open after updating | Security review phase 5, H10. The fork's columns would be missing until a launch that can write. |

## Known limitations of v1

| Limitation | See |
|---|---|
| Playing an episode that sits in a non-active queue stops after it (no continuation). | ADR-0005 |
| Using the official app on the same database merges all queues into the default queue on its first queue edit. | ADR-0002 |

## Fork operations

- ~~Automatic upstream sync~~: done in phase 4 (weekly, GitHub Actions, one sync PR; [fork-sync.md](../maintenance/fork-sync.md)).
- ~~A release-signed APK with its own key~~: done in phase 3 ([release-builds.md](../maintenance/release-builds.md)).
