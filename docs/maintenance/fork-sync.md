# Fork maintenance: staying in sync with upstream

## Remotes

```
origin    https://github.com/fogolin/AntennaPod        (this fork)
upstream  https://github.com/AntennaPod/AntennaPod     (official)
```

## What to track

- `develop` in the fork mirrors upstream `develop` and is only ever fast-forwarded.
- `multiple-queues` follows upstream **`develop`**, weekly. That's the user's choice (ADR-0009), and it's also what the fork was built on.

## Automatic sync (phase 4)

`.github/workflows/fork-sync.yml` runs every Monday at 06:23 São Paulo time, and from **Actions → Fork upstream sync → Run workflow**:

1. It merges upstream `develop` into a copy of `multiple-queues`, then builds and runs the unit tests and checkstyle. It uses a read-only token.
2. It fast-forwards the fork's `develop` and points `sync/upstream` at the tested upstream commit.
3. It opens or updates **one** PR, `sync/upstream` → `multiple-queues`. The PR says whether the checks passed and lists the upstream commits.
4. **The user reviews and merges.** The merge triggers `fork-release.yml`, so a new app release follows.

**Prerequisites** (one time):
- The fork's default branch is `multiple-queues`.
- "Allow GitHub Actions to create and approve pull requests" is on.

**Full checks:** PRs opened by the workflow don't start `checks.yml` (lint and emulator tests) on their own. Close and reopen the PR to run them.

**Don't use GitHub's "Sync fork" button** on `multiple-queues`. It merges upstream straight into the branch, with no tests and no PR.

### When the sync PR reports conflicts

The workflow can't resolve conflicts. The PR shows GitHub's conflict banner and lists the files. Ask Claude to resolve them. Claude will:

```sh
git fetch origin
git checkout -B sync/upstream origin/sync/upstream
git merge origin/multiple-queues      # resolve using the hotspots below
git push origin sync/upstream         # the PR updates, and checks.yml runs, since this push isn't the workflow's
```

The next scheduled run sees commits on `sync/upstream` that aren't upstream's and leaves the branch alone, so the resolution survives until the PR is merged.

## Manual sync (fallback)

```sh
git fetch upstream
git checkout develop && git merge --ff-only upstream/develop && git push origin develop
git checkout -b sync/manual origin/multiple-queues
git merge upstream/develop            # resolve conflicts, see the hotspots below
git push origin sync/manual           # open a PR into multiple-queues; CI builds and tests it
```

A **merge** is used rather than a rebase, so `multiple-queues` keeps a readable history and the PRs of earlier phases stay valid.

## Conflict hotspots

These are the files this feature changes, with upstream commits over the last 12 months:

| File | Commits |
|---|---|
| `MediaLibrarySessionCallback` | 24 |
| `PodDBAdapter` | 16 |
| `DBReader` | 10 |
| `DBWriter` | 8 |
| `ui/screen/queue` | 8 |
| `UserPreferences` | 5 |
| Phase 5: `FeedPreferences`, `FeedPreferencesCursor`, `FeedDatabaseWriter`, `DownloadServiceInterfaceImpl`, `FeedSettingsPreferenceFragment`, `feed_settings.xml` | 1 each |

Most conflicts will be neighbouring-line edits. The ones that need care:

- **Upstream bumps `VERSION` or adds `DBUpgrader` blocks.** No conflict with the fork's migration, because it lives in `onOpen` (ADR-0002). Just keep upstream's changes.
- **Upstream changes `setQueue` or a queue query in `PodDBAdapter`.** Re-apply the `queueId` parameter and the `queue = ?` filter.
- **Upstream adds a podcast setting (a new `Feeds` column).** Keep both. The fork's `feed_queue` lines sit in the middle of the column lists, not at the end where upstream appends, so these usually merge cleanly. If upstream ships a per-podcast enqueue setting (PR #8215), check that the two settings still make sense together.
- **Upstream ships its own multiple queues.** Stop and plan a migration from the fork schema to theirs. Their migration will see an existing `queue` column only if they chose the same name.

## Going back to the official app

1. In the fork: Settings → Backup & restore → Database export.
2. Uninstall the fork. It uses the same application id as the official app, but a different signature.
3. Install the official app, then Database import.

This works because the fork keeps upstream's database version (ADR-0002). All queues appear merged into one list, and nothing is lost.

## Installing builds

- **PRs:** CI produces `app-play-debug.apk` (application id `de.danoeh.antennapod.debug`) for testing. It installs next to any other install.
- **Everyday use:** every merge into `multiple-queues` publishes a signed release that replaces the official app. See [release-builds.md](release-builds.md). Merging a sync PR therefore also produces an updated app with the new upstream code.
