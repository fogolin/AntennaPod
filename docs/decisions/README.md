# Decision records

| ADR | Title | Status |
|---|---|---|
| [0001](ADR-0001-extend-existing-queue.md) | Extend the existing `Queue` table and API | accepted |
| [0002](ADR-0002-fork-compatible-schema.md) | Fork-compatible schema: same DB version, guarded migration, order by row id | accepted, supersedes plan §5 |
| [0003](ADR-0003-implicit-default-queue.md) | The default queue is implicit (id 0) | accepted |
| [0004](ADR-0004-single-membership.md) | An episode is in at most one queue (v1) | accepted for v1 |
| [0005](ADR-0005-active-queue.md) | The active queue drives playback and every enqueue | accepted |
| [0006](ADR-0006-v1-product-decisions.md) | Other v1 product decisions (D3–D7) | accepted for v1 |

**Format.** Each record has a status, a context, a decision and its consequences. Records are never rewritten. To change a decision, add a new ADR that supersedes the old one, and update this table.
