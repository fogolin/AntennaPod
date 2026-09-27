# Phase 0: setup

- **Branch:** `mq/phase-0-setup`, merged into `multiple-queues`
- **Date:** 2026-09-26
- **Code changes:** none (documentation only)

## Done

- Forked `AntennaPod/AntennaPod` to `fogolin/AntennaPod`. The fork's `develop` is at upstream `34de86f` (2026-09-24).
- Added the `upstream` remote.
- Created the integration branch `multiple-queues` and the per-phase branch convention (see [docs/README.md](../README.md)).
- Researched the maintainer requirements from issue #2648, PRs #8070, #8066, #3221, #8215 and #8718, and the forum. Results are in [findings/upstream-research.md](../findings/upstream-research.md) and the [plan](../plan/multiple-queues-plan.md).
- Mapped the queue architecture and the reusable UI patterns: [findings/codebase-map.md](../findings/codebase-map.md).
- Found that the workspace can't build Android, and moved build and test to GitHub Actions: [findings/environment.md](../findings/environment.md).
- Recorded decisions ADR-0001 to ADR-0006. ADR-0002 changes the plan's schema for fork compatibility.

## Findings during setup

- `docs/` is fork-only. It would have to be excluded from any upstream PR.
- Actions must be enabled once on the fork (a GitHub setting, not code).

## Security and performance

Documentation only, so there is nothing to review.
