# Backlog

These items are deferred on purpose. Each one needs its own discussion before it's built.

## Multiple queues: after v1

| Item | Notes |
|---|---|
| Per-podcast or tag-based routing into queues | keunes, Oct 2025. ByteHamster, 2022: prefer the tagging feature over new per-subscription state. The most requested follow-up. Overlaps with upstream PR #8215 (enqueue location per podcast). |
| Smart/automatic queues | Upstream #307. Explicitly after manual queues. |
| "Move to queue…" / choose the queue when adding | Needs a picker. Upstream rejected a picker as the *default* flow, not as an extra action. |
| Per-queue keep-sorted, sort order, lock and enqueue location | keunes expects each queue to keep its own settings. v1 keeps them global (ADR-0006, D4). |
| Show the active queue on the player screen, notification and widget, and switch from there | keunes' indicator list. |
| Android Auto and Wear: browse all queues | v1 exposes the active queue only. Coordinate with upstream PR #8466. |
| Queue rotation (continue with queue B when A is empty) | kjetilk's use case. It would build on continuous playback. |
| Rename the default queue | ADR-0003. Needs a row or a preference. |
| Episode in several queues | ADR-0004. The schema already allows it. |
| Search from the Queue screen scoped to the active queue | ADR-0006, D5. |
| Show the active queue's name in the toolbar title | ADR-0007 keeps "Queue", because the checked chip shows it. Revisit if the chips scroll out of view in practice. |
| Reorder queues (chip order) | Chips follow creation order (`Queues.id`). |
| Friendlier handling of a `NULL` or over-long name from an imported database | Security review phase 2, G2. Cosmetic only. |

## Known limitations of v1

| Limitation | See |
|---|---|
| Playing an episode that sits in a non-active queue stops after it (no continuation). | ADR-0005 |
| Using the official app on the same database merges all queues into the default queue on its first queue edit. | ADR-0002 |

## Fork operations

- **Next: automatic upstream sync.** A scheduled task that notices new upstream releases, merges them into a `sync/<tag>` branch and opens a PR into `multiple-queues`. CI tests it, the user approves, and the merge triggers a new release build. Scheduled GitHub workflows only run from the default branch, so the fork's default branch would move from `develop` to `multiple-queues`.
- ~~A release-signed APK with its own key~~: done in phase 3 ([release-builds.md](../maintenance/release-builds.md)).
