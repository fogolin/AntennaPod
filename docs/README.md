# Fork documentation: fogolin/AntennaPod

This folder documents everything done in this fork on top of upstream [AntennaPod](https://github.com/AntennaPod/AntennaPod). The first feature is **multiple queues** (upstream issue #2648).

> This `docs/` folder exists only in the fork. If any of this work is ever proposed upstream, `docs/` must be left out: upstream rejects unrelated changes (see `findings/upstream-research.md`).

## Layout

| Folder | What goes in it |
|---|---|
| `plan/` | The implementation plan as reviewed, kept as a snapshot. Later changes are recorded in `decisions/`, not edited into the plan. |
| `features/` | Behavior specs of what the fork adds, written from the user's point of view with the technical rules behind them. |
| `decisions/` | Architecture decision records (ADRs). One file per decision, numbered, never rewritten. A new ADR supersedes an old one. |
| `phases/` | One log per implementation phase: scope, branch, what changed, how to test, results. |
| `findings/` | Research: maintainer requirements, the codebase map, environment constraints, anything learned along the way. |
| `fixtures/` | Test fixtures and manual test scenarios: which data each test builds and how to reproduce it by hand. |
| `security/` | The security review done at the end of each phase. |
| `perf/` | The performance, reuse and simplification review done at the end of each phase. |
| `future/` | Backlog: deferred features, known limitations, ideas. |
| `maintenance/` | How to keep the fork in sync with upstream, how to install and test builds, and how to go back to the official app. |

## Branches

| Branch | Purpose |
|---|---|
| `develop` | Mirror of upstream `develop`. Never committed to directly. |
| `multiple-queues` | Integration branch: the fork's "product" branch. Phases are merged here after they have been tested. |
| `mq/phase-N-<name>` | One branch per phase, cut from `multiple-queues`, or from the previous phase branch while that one is awaiting approval. It is opened as a PR so CI builds an APK for testing. |

## Workflow per phase

1. Create the branch `mq/phase-N-<name>` from `multiple-queues`.
2. Look for existing code to reuse first. The findings go into the phase log.
3. Implement, with tests, following upstream `AGENTS.md`: minimal diff, no comments in code, English strings only.
4. Security review, written to `security/phase-N.md`.
5. Performance, reuse and simplification review, written to `perf/phase-N.md`. Improvements are applied in the same phase.
6. Push and open a PR into `multiple-queues`. CI (the upstream `checks.yml`) runs checkstyle, lint, the unit tests and emulator tests, and uploads `app-play-debug.apk`.
7. The user tests the APK against the checklist in the phase log.
8. **Only the user approves and merges PRs.** Nothing is merged into `multiple-queues` without their approval. The next phase branch is stacked on the previous phase branch until that one is merged.

## Index

- Plan: [plan/multiple-queues-plan.md](plan/multiple-queues-plan.md)
- Feature spec: [features/multiple-queues.md](features/multiple-queues.md)
- Decisions: [decisions/](decisions/)
- Phases:
  - [phase 0, setup](phases/phase-0-setup.md)
  - [phase 1, storage](phases/phase-1-storage.md)
- Findings:
  - [upstream research](findings/upstream-research.md)
  - [codebase map](findings/codebase-map.md)
  - [environment](findings/environment.md)
- Maintenance: [maintenance/fork-sync.md](maintenance/fork-sync.md)
- Backlog: [future/backlog.md](future/backlog.md)
