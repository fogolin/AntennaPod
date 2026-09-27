# Performance, reuse and simplification review: phase 3 (release builds)

- **Date:** 2026-09-27

## Build cost

- **Only `:app:assemblePlayRelease` is built.** That leaves out the Wear OS module, the free flavor and the tests, since the tests already ran on the PR being merged. The expected run time is about 10–15 minutes, versus about 25 for the full `checks.yml`.
- **The Gradle cache reuses the exact key `checks.yml` uses,** so dependency downloads are shared.
- **One run per merge.** Merges are rare, so the cost is negligible. Actions minutes are free for public repositories.

## Reuse

| Need | Reused |
|---|---|
| Signing | The existing `releaseConfig` in `app/build.gradle` and its Gradle properties. No build-script change. |
| Workflow steps | The JDK, cache and checkout steps are copied from `checks.yml`, with the same pinned SHAs. |
| Wrapper check | The same `gradle/actions/wrapper-validation` pin as `checks.yml` |
| Publishing | The preinstalled `gh` CLI, instead of a release action |
| Updates on the phone | Obtainium reads GitHub Releases directly, so there's no update server or F-Droid repo to run |

## Simplifications

1. **Replace mode means no `applicationId` or `provider_authority` changes.** The phase touches no upstream file, so upstream syncs can't conflict with it.
2. **The version suffix is applied in the CI checkout only, with `sed`, and never committed.** `versionCode` stays upstream's, so there's no counter to maintain.
3. **One workflow file** (`fork-release.yml`, about 75 lines).

## Checks done locally

| Tool | Result |
|---|---|
| `actionlint` 1.7.7 with `shellcheck` 0.10.0 (downloaded from their GitHub releases) | 0 findings |
| The version-name `sed` step, run against a copy of `app/build.gradle` | `3.12.1` becomes `3.12.1-mq.7`, and nothing else changes |
