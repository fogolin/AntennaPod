# Decision records

| ADR | Title | Status |
|---|---|---|
| [0001](ADR-0001-extend-existing-queue.md) | Extend the existing `Queue` table and API | accepted |
| [0002](ADR-0002-fork-compatible-schema.md) | Fork-compatible schema: same DB version, guarded migration, order by row id | accepted, supersedes plan §5 |
| [0003](ADR-0003-implicit-default-queue.md) | The default queue is implicit (id 0) | accepted |
| [0004](ADR-0004-single-membership.md) | An episode is in at most one queue (v1) | accepted for v1 |
| [0005](ADR-0005-active-queue.md) | The active queue drives playback and every enqueue | accepted |
| [0006](ADR-0006-v1-product-decisions.md) | Other v1 product decisions (D3–D7) | accepted for v1 |
| [0007](ADR-0007-queue-ui.md) | Queue UI: chips switch, the overflow menu manages | accepted, supersedes plan §7 |
| [0008](ADR-0008-fork-release-builds.md) | Fork release builds: replace the official app, play flavor, signed GitHub Releases | accepted |

**Format.** Each record has a status, a context, a decision and its consequences. Records are never rewritten. To change a decision, add a new ADR that supersedes the old one, and update this table.
