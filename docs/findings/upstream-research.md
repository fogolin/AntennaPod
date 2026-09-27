# Upstream research: what AntennaPod maintainers want

Collected on 2026-09-26. The full requirement table with sources is §1 of the [plan](../plan/multiple-queues-plan.md#1-what-maintainers-asked-for). This file records how it was gathered and the conclusions that shape the fork.

## Sources read

| Source | How it was read | Key content |
|---|---|---|
| Issue #2648 (2018–2025, 44 pages) | PDF export provided by the user. GitHub renders issue comments client-side, so they couldn't be fetched directly. | ByteHamster, 2022: dumb queues first, don't use the drawer for management. ByteHamster, Oct 2025: the open problems, "extend the existing system", no master switch, MVP only. keunes, Oct 2025: indicators, default queue, sync question. |
| PR #8070 (dominikfill) | Fetched from GitHub | ByteHamster, Nov 19, 2025 (after the needs-decision call): switch in the same screen and remember it, add to the active queue with no dialog, English strings only. |
| PR #8066 (seefood) | Fetched from GitHub | ByteHamster: bare minimum, use existing `DBReader`/`DBWriter`, and doubts about AI-written code that ignores the codebase. |
| PR #3221 (2019) | Fetched from GitHub | No extra UI complexity with one queue. Menu entries only with multiple queues. |
| PR #8215 | Fetched from GitHub | Unrelated changes block a merge. Features need an approved issue. |
| PR #8718 | Fetched from GitHub | Approved PR that bumps DB `VERSION` to 3130000. Relevant for migrations. |
| Forum: needs-decision thread (post 34), multiple-queues design thread (2023) | Fetched | The team liked #8066's UX but wants a human implementer. keunes' 2023 feature mapping. |
| Repo `AGENTS.md`, `CONTRIBUTING.md`, PR template, code-style page | Read | Minimal diff, no comments, English strings only, checkstyle/lint/spotbugs, tests for core features. |

## State of the earlier attempts

**#8070**

- Closed Feb 2026 and 338 commits behind `develop`.
- It adds a parallel `Queues`/`QueueItems` system with 21 temporary `df_*` methods (55 references).
- It has no DB upgrade path.
- The delete-queue SQL is broken.
- It modified 22 translation files.
- Its UX (a picker dialog) was rejected.

**#8066**

- Closed, rejected over its scope and AI-written code.

**Fork iamalanturing/AntennaPodwSmartPlaylists**

- Checked by the user: only upstream updates, no feature work.

## Conclusions used by this fork

1. Extend the existing `Queue` table and the existing `DBReader`/`DBWriter` API. See [ADR-0001](../decisions/ADR-0001-extend-existing-queue.md).
2. Build the MVP first: manual queues and one active queue. See the [feature spec](../features/multiple-queues.md).
3. Keep the diff minimal and in upstream style, so the work could be offered upstream later.
4. **Risk:** upstream may not accept code written by an AI for a contributor who doesn't write Java. ByteHamster closed #8066 partly for that reason. The fork is useful on its own either way. See [maintenance/fork-sync.md](../maintenance/fork-sync.md).
