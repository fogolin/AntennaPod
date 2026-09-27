# Security review: phase 4 (upstream sync)

- **Scope:** `.github/workflows/fork-sync.yml`
- **Date:** 2026-09-27
- **Result:** no open findings

| # | Area | Finding | Status |
|---|---|---|---|
| S1 | Running upstream code with a write token | The Gradle build runs code that comes from upstream (build scripts, tests). That happens only in the `check` job, whose token is `contents: read`, and the checkout uses `persist-credentials: false`, so the token isn't stored in `.git/config`. The `publish` job holds the write token and never runs Gradle or any upstream script: it only uses `git`, and `gh` with a body file. | OK |
| S2 | Signing key | The sync workflow references no secrets. The key only exists in `fork-release.yml`, which runs after the user merges. | OK |
| S3 | What reaches `multiple-queues` | Nothing, unless the user merges the sync PR. The workflow writes only `develop` (fast-forward only, a plain `git push` without force, so a diverged `develop` is left alone with a warning) and `sync/upstream`, a bot branch. | OK |
| S4 | Tested code matches the proposed code | `publish` pushes the exact upstream commit `check` tested (a job output), not whatever `develop` points to by then. Merging the PR produces the same tree the tests ran on. | OK |
| S5 | Untrusted text in the PR body | Upstream commit subjects, and file names in conflicts, are written to a file with `echo` and `git log` and passed with `--body-file`. They're never evaluated by the shell. Commit subjects sit inside a fenced code block, so `@mentions` and `#123` references don't notify upstream people or link to the fork's numbers. | OK, tested with a subject containing `@someone (#1)` |
| S6 | Manual work overwritten | Before force-pushing `sync/upstream`, the job checks for commits on it that aren't upstream (a conflict resolution) and leaves the branch and PR alone if there are any. | OK, tested |
| S7 | Supply chain | Same pinned action SHAs as `checks.yml`. The wrapper is validated on the merged code before Gradle runs. `gh` and `git` come from the runner image. | OK |
| S8 | Token scopes | The top-level default is `contents: read`. Only `publish` gets `contents: write` and `pull-requests: write`. | OK |
| S9 | Repository settings the user changes | "Allow GitHub Actions to create and approve pull requests" lets any workflow with `pull-requests: write` open PRs. Besides `fork-sync.yml`, two upstream workflows have that scope, and neither opens PRs:<br>• `assign-milestone.yml` only reacts to PRs into `develop`.<br>• `close-if-no-reply.yml` runs daily from the default branch once it's `multiple-queues`, but its job is guarded with `if: github.repository == 'AntennaPod/AntennaPod'`, so it's skipped in the fork.<br>Making `multiple-queues` the default branch doesn't change who can push. | Accepted |
