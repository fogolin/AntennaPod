# Fork maintenance: staying in sync with upstream

## Remotes

```
origin    https://github.com/fogolin/AntennaPod        (this fork)
upstream  https://github.com/AntennaPod/AntennaPod     (official)
```

## What to track

- `develop` in the fork mirrors `upstream/develop` and is only ever fast-forwarded.
- `multiple-queues` follows **upstream releases**, not every `develop` commit. Releases come every one to two months (3.10.2 through 3.12.2 in the last year) and are tested. That means about 6–10 syncs a year.

## Sync procedure

```sh
git fetch upstream --tags
git checkout develop && git merge --ff-only upstream/develop && git push origin develop
git checkout -b sync/<upstream-tag> multiple-queues
git merge <upstream-tag>              # or upstream/develop for the latest
# resolve conflicts, see the hotspots below
git push origin sync/<upstream-tag>   # open a PR into multiple-queues; CI builds and tests it
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

Most conflicts will be neighbouring-line edits. The ones that need care:

- **Upstream bumps `VERSION` or adds `DBUpgrader` blocks.** No conflict with the fork's migration, because it lives in `onOpen` (ADR-0002). Just keep upstream's changes.
- **Upstream changes `setQueue` or a queue query in `PodDBAdapter`.** Re-apply the `queueId` parameter and the `queue = ?` filter.
- **Upstream ships its own multiple queues.** Stop and plan a migration from the fork schema to theirs. Their migration will see an existing `queue` column only if they chose the same name.

## Going back to the official app

1. In the fork: Settings → Backup & restore → Database export.
2. Uninstall the fork. It uses the same application id as the official app, but a different signature.
3. Install the official app, then Database import.

This works because the fork keeps upstream's database version (ADR-0002). All queues appear merged into one list, and nothing is lost.

## Installing builds

- **PRs:** CI produces `app-play-debug.apk` (application id `de.danoeh.antennapod.debug`) for testing. It installs next to any other install.
- **Everyday use:** every merge into `multiple-queues` publishes a signed release that replaces the official app. See [release-builds.md](release-builds.md). Merging a sync PR therefore also produces an updated app with the new upstream code.
