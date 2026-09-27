# Phase 4: automatic upstream sync

- **Branch:** `mq/phase-4-sync`, cut from `multiple-queues` after the upstream sync check (level with upstream `develop` `6f08045`, so nothing to merge). Its PR goes into `multiple-queues`.
- **Date:** 2026-09-27
- **App code changes:** none. The phase adds one fork-only workflow file and documentation.

## Why

The user's rule is to have the latest upstream merged before any new phase, and they want it without having to notice the fork is behind. They chose **GitHub Actions** and a **weekly** cadence. Details are in [ADR-0009](../decisions/ADR-0009-upstream-sync.md).

## What changed

| File | Change |
|---|---|
| `.github/workflows/fork-sync.yml` | **New.** Runs every Monday at 06:23 São Paulo time, and on demand.<br>**`check`** (read-only token): merge upstream `develop` into `multiple-queues`, then build, run the unit tests and checkstyle, or record conflicts.<br>**`publish`** (write token, no build): fast-forward `develop`, point `sync/upstream` at the tested upstream commit, and open or update the single sync PR with the result. Manual conflict resolutions are never overwritten. |
| `docs/…` | ADR-0009, the security and performance reviews, this log, `fork-sync.md` (the automated flow and conflict handling), the index and the backlog. Phase 3's results are recorded. |

## Reuse check

- **Build and tests:** the same Gradle tasks as `checks.yml`, the same pinned action SHAs, and the same Gradle cache key.
- **Releases:** no new code. Merging the sync PR triggers `fork-release.yml` from phase 3.
- **Conflict guidance:** the hotspot list that has been in `fork-sync.md` since phase 0.

## Security and performance

- [security/phase-4.md](../security/phase-4.md): upstream code only runs in the read-only job, the write job never builds, the tested commit is the one proposed, and PR text can't ping upstream people.
- [perf/phase-4.md](../perf/phase-4.md): about 1 minute on quiet weeks, about 15 minutes when upstream moved, one PR at most.

## Local checks

| Check | Result |
|---|---|
| `actionlint` with `shellcheck` | clean |
| The workflow's own `run:` scripts, run against local test repositories | Four scenarios pass: up to date, clean merge (with a PR body preview), conflict, and a manual resolution kept |

## After merging: two settings (one time)

1. **Settings → General → Default branch:** switch to `multiple-queues`. GitHub only runs scheduled workflows from the default branch.
   - It also makes PRs target `multiple-queues` by default, and the repository page shows the fork's docs.
   - Afterwards, don't use GitHub's **Sync fork** button. The workflow does the syncing through a PR.
2. **Settings → Actions → General → Workflow permissions:** tick **Allow GitHub Actions to create and approve pull requests**, then Save.

## How to test (after merging and the two settings)

1. **Actions → Fork upstream sync → Run workflow** (on `multiple-queues`).
2. **Expected today:** the run is green in about a minute and no PR is opened, because the fork is up to date with upstream.
3. **On the first Monday upstream has moved,** a PR "Sync with upstream AntennaPod (…)" appears. It says whether the build and tests passed and lists the upstream commits.
   - Optionally close and reopen it to run the full checks.
   - Then merge it. `Fork release` publishes `v<version>-mq.<n>`, and Obtainium offers the update.

## Phase 3 results (recorded here, since phase 3's PR was already merged)

- **Release run [36343221104](https://github.com/fogolin/AntennaPod/actions/runs/36343221104):** green in about 5 minutes. It published [v3.12.1-mq.1](https://github.com/fogolin/AntennaPod/releases/tag/v3.12.1-mq.1).
- **The APK** is 9.5 MB, shrunk by R8 (the debug builds were about 21 MB).
- **Verified after download:** the SHA-256 matches the published `.sha256`, and `jarsigner` verifies the signature with signer `CN=AntennaPod fork`, the user's key.

## Results

- **CI on the PR:** pending.
- **First sync run:** pending the merge and the two settings.
