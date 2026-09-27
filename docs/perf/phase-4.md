# Performance, reuse and simplification review: phase 4 (upstream sync)

- **Date:** 2026-09-27

## Cost

| Week | What runs | Time |
|---|---|---|
| Nothing new upstream (common) | Checkout, fetch, ancestry check. `publish` is skipped. | about 1 minute |
| Upstream moved, clean merge | Build, unit tests and checkstyle on the merged code, then `publish` | about 15 minutes |
| Conflict | Merge attempt, then `publish` | about 2 minutes |

Actions minutes are free for this public fork. The Gradle cache uses the same key as `checks.yml` and `fork-release.yml`.

## Reuse

| Need | Reused |
|---|---|
| Build and test | The Gradle tasks `checks.yml` runs in its build and unit-test jobs, plus `checkstyle` |
| Workflow steps | JDK, cache, checkout and wrapper validation, with the same pinned SHAs as `checks.yml` |
| Publishing | The `gh` CLI on the runner |
| Releasing | The `fork-release.yml` from phase 3, unchanged: merging the sync PR is the trigger |
| Conflict guidance | The hotspots listed in `fork-sync.md` since phase 0 |

## Simplifications

1. **The PR head is the upstream commit itself** (`sync/upstream`), not a merge commit made by a bot. GitHub's merge button creates the merge, so the PR diff shows exactly upstream's changes, and there's no bot-authored merge in the history.
2. **There's only ever one sync PR.** The branch is force-updated and the open PR is edited in place, so weeks don't pile up as separate PRs.
3. **No issue tracker is needed.** Issues are disabled on the fork, so conflicts surface in the PR, via GitHub's conflict banner and the file list.
4. **No extra secrets.** It uses the workflow token only.

## Checks done locally

- **`actionlint` with `shellcheck`:** clean, after replacing a `sed` with backtick quoting by a plain `while read` loop.
- **A test harness** (`test-sync.sh` in the agent workspace, not committed) pulled the exact `run:` scripts out of the YAML and ran them against local bare repositories standing in for upstream and the fork. Four scenarios passed:

| Scenario | Result |
|---|---|
| Nothing new | `up-to-date` |
| Clean upstream change | Merged. `develop` fast-forwarded, `sync/upstream` created, and the PR body rendered with the commit list in a code block, `@mention` included. |
| Upstream edits a line the fork changed | `conflict`, with the file listed |
| A manual resolution sits on `sync/upstream` | The branch is kept, and `develop` is still mirrored |
