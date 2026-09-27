# Phase 3: release builds of the fork

- **Branch:** `mq/phase-3-release`, cut from `multiple-queues` after the upstream sync check (the fork was level with upstream `develop` `6f08045`, so there was nothing to merge). Its PR goes into `multiple-queues`.
- **Date:** 2026-09-27
- **App code changes:** none. The phase adds one fork-only workflow file and documentation.

## Why

The user tested multiple queues on the phone and wants the fork as their everyday AntennaPod. Until now, the only builds were the debug APKs from PRs.

## Decisions (user's choices)

- **Replace the official app.** It keeps the same application id.
- **Play flavor.** It includes Chromecast and Wear OS support.

Details are in [ADR-0008](../decisions/ADR-0008-fork-release-builds.md).

## What changed

| File | Change |
|---|---|
| `.github/workflows/fork-release.yml` | **New.** On every push to `multiple-queues` (and on a manual run):<br>1. validate the Gradle wrapper;<br>2. restore the signing key from secrets;<br>3. set `versionName` to `<upstream>-mq.<run>`;<br>4. build `:app:assemblePlayRelease`;<br>5. publish a GitHub Release with the APK and its SHA-256;<br>6. remove the key. |
| `scripts/createForkSigningKey.sh` | **New** (added at the user's request, to keep the secrets in a local `.env`). It creates a PKCS12 key outside the repo with a random password, and writes `.env` with the four secrets in the format `gh secret set -f` reads. Style follows the existing `scripts/*.sh`. |
| `.gitignore` | `.env` added. `*.keystore` was already ignored. |
| `docs/maintenance/release-builds.md` | **New.** Key creation and backup, GitHub secrets, first install, Obtainium updates, limitations, troubleshooting. |
| `docs/…` | ADR-0008, the security and performance reviews, this log, and the index. The workflow now starts with an upstream sync step (the user's rule). |

## Reuse check

- **Signing** uses the existing `releaseConfig` in `app/build.gradle`, which already reads `releaseStoreFile`, `releaseStorePassword`, `releaseKeyAlias` and `releaseKeyPassword`. No build-script change.
- **The workflow steps** (checkout, JDK 21, Gradle cache, wrapper validation) copy `checks.yml`, including the pinned action SHAs.
- **The release APK path** follows the debug path CI already uploads: `app/build/outputs/apk/play/<type>/app-play-<type>.apk`.
- **No third-party actions.** Publishing uses the `gh` CLI that's on every runner.

## Security and performance

- [security/phase-3.md](../security/phase-3.md): secrets never reach PR code, the token is scoped to `contents: write`, actions are pinned, the wrapper is validated, and releases carry checksums.
- [perf/phase-3.md](../perf/phase-3.md): only the phone app is built, with no repeated tests, in about 10–15 minutes per merge.

## Local checks

| Tool | Result |
|---|---|
| `actionlint` with `shellcheck` | clean |
| `shellcheck` on `createForkSigningKey.sh` | clean |
| Script dry run in a throwaway clone | `.env` ignored by git, files mode `600`, the keystore decodes byte-identical the way the workflow does it, `keytool` reads it, `jarsigner` signs with it, and a second run refuses to overwrite |
| Version `sed` step, tested on a copy of `app/build.gradle` | works |

The workflow can't run before it's merged, because it only triggers on `multiple-queues`. This PR's own CI (`checks.yml`) isn't affected: no app code changed.

## How to test (after merging)

1. **Before merging,** run `scripts/createForkSigningKey.sh` from your clone, back up the two files, and upload the secrets with `gh secret set -f .env --repo fogolin/AntennaPod`. See [release-builds.md](../maintenance/release-builds.md). If you merge first, the release run fails with "Signing key missing". Add the secrets afterwards and choose **Re-run jobs** on that run.
2. **Merge this PR.** Under **Actions**, the **Fork release** run should go green in about 15 minutes.
3. **Check the release.** Under **Releases** there's `v3.12.1-mq.<n>` with `AntennaPod-3.12.1-mq.<n>.apk` and its `.sha256`.
4. **Install it.** Follow "First install" in release-builds.md: export, uninstall the official app, install, import.
5. **Check the app.** Settings → About shows `3.12.1-mq.<n>`. Your queues and chips are there, and playback works.
6. **Optional:** add the repo to Obtainium. It should see the release as up to date.

## Results

- **CI on the PR:** pending.
- **First release run:** pending the user's secrets and merge.
