# ADR-0008: Fork release builds

- **Status:** accepted, 2026-09-27
- **Context:** Until phase 2, the only installable builds were the CI debug APKs from PRs. Those have a separate app id, run unoptimised, and nothing was built when `multiple-queues` changed. The user wants the fork as their everyday app.

## Decision

**Replace the official app.** The release build keeps upstream's application id, `de.danoeh.antennapod`.

- There's no build-script change, so there's nothing to conflict on upstream syncs.
- There's one app on the phone, so no double feed refreshes, downloads or notifications.
- Because the fork is signed with the user's own key, Android won't install it over the official app. The official app is uninstalled once, after a database export.

This was the user's choice; the alternative was side by side.

**Flavor `play`.** It's what was tested, and it includes Chromecast and Wear OS support (the user's choice).

**Workflow `.github/workflows/fork-release.yml`.** This file exists only in the fork.

| Aspect | Setting |
|---|---|
| Triggers | Push to `multiple-queues` (that is, every PR the user merges) and a manual run. The job only runs for `refs/heads/multiple-queues`. |
| Build | `./gradlew :app:assemblePlayRelease`, signed through the existing `releaseConfig` in `app/build.gradle`. It reads the Gradle properties `releaseStoreFile`, `releaseStorePassword`, `releaseKeyAlias` and `releaseKeyPassword`, passed as `ORG_GRADLE_PROJECT_*` environment variables from repository secrets. |
| No tests | They already ran on the PR that was merged. |
| Version | `versionName` gets the suffix `-mq.<run number>` (for example `3.12.1-mq.7`) in the CI checkout only. `versionCode` stays upstream's, so installing a newer fork build over an older one is always allowed (Android only rejects a lower `versionCode`), and each upstream sync raises it. |
| Output | A GitHub Release tagged `v<versionName>` with the APK and its SHA-256. [Obtainium](https://github.com/ImranR98/Obtainium) can follow these releases for updates. |

## Consequences

- **The signing key is the fork's identity.** Losing it means future builds can't update the installed app. The only way back is export, uninstall, install, import. The key must be backed up by the user, and it never leaves the user's machine or GitHub secrets.
- **The official Wear OS watch app won't pair.** The Wear data layer requires the same signature on phone and watch.
- **Settings don't carry over.** Global settings aren't part of the database export (the app has no settings export), so they're redone once. Podcast-level settings live in the database and do carry over.
- **For an upstream PR,** the workflow file would be left out, like `docs/`.

## Rejected alternatives

- **Side by side with a different application id.** It needs `applicationIdSuffix` plus a new `provider_authority` in `app/build.gradle`, a file upstream changes every release. It also leaves two apps doing the same background work.
- **A third-party release action.** `gh release create` is preinstalled on the runner and needs no extra supply-chain trust.
