# ADR-0009: Weekly upstream sync with GitHub Actions

- **Status:** accepted, 2026-09-27. The runner (GitHub Actions) and the cadence (weekly) are the user's choices.
- **Context:** The user's rule is to have the latest upstream changes merged before any new phase. Phase 3 made every merge into `multiple-queues` produce a release, so syncing upstream also updates the app on the phone.

## Decision

A workflow, `.github/workflows/fork-sync.yml`, runs every Monday at 09:23 UTC (06:23 in São Paulo) and on demand. It has two jobs.

**`check`** (token: `contents: read`, and no credentials left in the checkout):

1. Check out `multiple-queues` and fetch upstream `develop`.
2. If upstream is already contained in `multiple-queues`, stop. That's the usual week.
3. Merge. If the merge is clean, validate the Gradle wrapper and run `assemblePlayDebug testPlayDebugUnitTest testDebugUnitTest checkstyle` on the merged code, the same tasks `checks.yml` runs in its build and unit-test jobs. If it conflicts, record the conflicting files.

**`publish`** (token: `contents: write, pull-requests: write`, and it never builds anything):

1. Fast-forward the fork's `develop` to upstream (a mirror, so this never force-pushes).
2. Point `sync/upstream` at the upstream commit that was tested.
3. Open or update **one** pull request `sync/upstream` → `multiple-queues`. Its body carries the result: "passed", "failed, don't merge", or the list of conflicting files. Upstream commit subjects go in a code block, so `@mentions` and `#refs` from upstream don't notify anyone or link to the fork's own numbers.
4. If `sync/upstream` holds commits that aren't upstream (a manual conflict resolution), leave it alone.
5. If there were conflicts or failures, the run goes red.

**Merging the PR is the user's action.** Its push to `multiple-queues` triggers `fork-release.yml`, so an approved sync becomes a new app release.

## Consequences and limits

**Two repository settings are required,** both done by the user once:

- The **default branch must be `multiple-queues`**, because GitHub runs scheduled workflows and shows the "Run workflow" button only from the default branch.
- **Settings → Actions → General → "Allow GitHub Actions to create and approve pull requests"** must be on, so the workflow token can open the PR.

**PRs opened by the workflow token don't start other workflows.** That's a GitHub rule to prevent loops, so `checks.yml` (lint and emulator tests) doesn't run on the sync PR by itself. The `check` job covers build, unit tests and checkstyle. For the full checks, the user closes and reopens the PR: the reopen is their event, so `checks.yml` runs.

**Conflicts can't be resolved by a workflow.** The PR then shows GitHub's conflict banner and lists the files. The user asks Claude, who merges `multiple-queues` into `sync/upstream`, resolves using [fork-sync.md](../maintenance/fork-sync.md), and pushes. The next Monday run keeps that work (step 4).

**Scheduled workflows in public repositories are disabled after 60 days without repository activity.** Weekly pushes to `develop` count as activity whenever upstream moves.

## Rejected alternative: a scheduled Claude session

It could resolve conflicts itself, and its PRs would get the full `checks.yml`. But every run uses Claude, and GitHub Actions is free for this public fork. This was the user's choice.
