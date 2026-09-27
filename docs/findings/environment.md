# Environment constraints

## Where the code is written

The agent's cloud workspace can reach GitHub, but not Google's Maven repository, Maven Central, the Gradle plugin portal or `services.gradle.org`. The egress proxy denies them with a 403 (checked on 2026-09-26). So in the workspace:

- Gradle can't download the Android Gradle Plugin or any dependency.
- Nothing can be compiled, unit-tested or linted.

**Consequence:** every compile, test and style check runs in **GitHub Actions on the fork**. The upstream `.github/workflows/checks.yml` already does this on every `pull_request`:

| Job | What it runs |
|---|---|
| Static analysis | Android XML formatter, `checkstyle lint` |
| Unit tests | `assemblePlayDebug` plus `testPlayDebugUnitTest testDebugUnitTest` (and the same for PlayRelease), and uploads the artifact **`app-play-debug.apk`** |
| Emulator tests | APIs 23, 30 and 36 |

The code is therefore written carefully against the real sources, then verified by CI on the phase PR. CI results are read through the GitHub checks API.

## What can be checked locally (found in phase 2)

GitHub **release downloads** are reachable, so two of CI's style tools run in the workspace with the same versions:

| Tool | Where it comes from | Command |
|---|---|---|
| `android-xml-formatter` 1.1.0 | The same jar `checks.yml` downloads | `java -jar android-xml-formatter.jar <layout.xml>`, then check `git diff` |
| Checkstyle 10.12.0 | `checkstyle-10.12.0-all.jar` from the checkstyle GitHub releases, the version `common.gradle` pins | `java -Dconfig_loc=config/checkstyle -jar checkstyle.jar -c config/checkstyle/checkstyle.xml <files>` |

Compiling, lint and tests still need CI.

## Reading CI failures

Job **logs** and **artifacts** (the `test-report` from failed emulator runs) are served from Azure blob storage, which the workspace proxy blocks. Only check-run statuses and annotations are readable through the API. When a job fails without a readable annotation, either the user opens the job log in the browser and shares the failing test, or the cause is narrowed down by reasoning and another push.

## Requirements on the fork

- GitHub Actions must be enabled on `fogolin/AntennaPod`. Forks start with workflows disabled.
- Actions minutes are free for public repositories.

## Installing test builds

`app-play-debug.apk` has the application id `de.danoeh.antennapod.debug`. It installs **next to** the official app and has its own database. To test with real data:

1. In the official app: Settings → Backup & restore → Database export.
2. In the debug app: Database import.

The fork keeps the same database version as upstream, so this also works in the other direction. See [ADR-0002](../decisions/ADR-0002-fork-compatible-schema.md).
