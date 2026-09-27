# Release builds of the fork

Every time `multiple-queues` changes (you merge a PR), GitHub builds a signed release APK and publishes it under **Releases** on https://github.com/fogolin/AntennaPod/releases. The design is in [ADR-0008](../decisions/ADR-0008-fork-release-builds.md).

## One-time setup: the signing key

Do this on your own computer, in your local clone of the fork. The key only leaves your machine as GitHub secrets.

**You need:** `keytool`, which comes with any Java JDK and with Android Studio (`<Android Studio>/jbr/bin`). Run everything below in a bash shell: Linux, macOS, WSL, or Git Bash on Windows.

### 1. Create the key and `.env`

From the root of your clone:

```sh
scripts/createForkSigningKey.sh
```

The script:

- creates the key at `~/.antennapod-fork/antennapod-fork.keystore`. It sits **outside** the repository, so `git clean` can never delete it. To put it somewhere else, pass a path as the first argument.
- generates a random 32-character password. You never have to invent or type one.
- writes `.env` in the repository root with the four values the release workflow needs: `FORK_KEYSTORE_BASE64`, `FORK_KEYSTORE_PASSWORD`, `FORK_KEY_ALIAS` and `FORK_KEY_PASSWORD`. `.env` is listed in `.gitignore`, so it can't be committed by accident.
- makes both files readable only by you.
- refuses to run if either file already exists, so it can never overwrite a key you're using.

### 2. Back it up

**Back up `antennapod-fork.keystore` and `.env`**, for example as attachments in your password manager. `.env` alone is enough to restore everything, because it contains the key (base64) and its password. `git clean -fdx` deletes ignored files such as `.env`, so restore it from the backup if that ever happens. If you lose both, future builds can't update the installed app. You'd have to export your data, uninstall, install again and import.

### 3. Upload the secrets

With the [GitHub CLI](https://cli.github.com/), after `gh auth login`:

```sh
gh secret set -f .env --repo fogolin/AntennaPod
```

Without it, open https://github.com/fogolin/AntennaPod/settings/secrets/actions. For each line of `.env`, choose **New repository secret**: the name is the part before `=`, and the value is everything after it.

Check that the page lists `FORK_KEYSTORE_BASE64`, `FORK_KEYSTORE_PASSWORD`, `FORK_KEY_ALIAS` and `FORK_KEY_PASSWORD`.

### Manual alternative (without the script)

```sh
keytool -genkeypair -keystore antennapod-fork.keystore -storetype PKCS12 -alias antennapod-fork \
    -keyalg RSA -keysize 4096 -validity 10000
base64 < antennapod-fork.keystore | tr -d '\n' > keystore.txt
```

Then add the four secrets by hand:

| Secret | Value |
|---|---|
| `FORK_KEYSTORE_BASE64` | the contents of `keystore.txt` |
| `FORK_KEYSTORE_PASSWORD` | your password |
| `FORK_KEY_ALIAS` | `antennapod-fork` |
| `FORK_KEY_PASSWORD` | the same password |

Delete `keystore.txt` afterwards.

## First install (replacing the official app)

1. **Export your data.** In the app you use now (the debug build from the PRs), go to Settings → Backup & restore → **Database export**.
2. **Note your settings.** Global settings (speed, auto-download, notifications, …) aren't in the export. Podcast-level settings are.
3. **Uninstall the official AntennaPod.** Android can't update it with the fork, because the signatures differ.
4. **Install the fork.** Download `AntennaPod-<version>.apk` from the latest release and install it. You'll need to allow installs from your browser or file manager.
5. **Import your data.** Settings → Backup & restore → **Database import**, then restart the app.
6. **Check it.** Your queues, subscriptions and history should all be there.
   - Downloaded episodes don't come across, because the files belonged to the old app. Download them again.
   - Once you're happy, uninstall the debug build.

## Updates

- **Automatic, with Obtainium (recommended).** Install [Obtainium](https://github.com/ImranR98/Obtainium), add the app with the URL `https://github.com/fogolin/AntennaPod`, and it notifies you or updates when a new release appears. It installs over the existing app, so no data is lost.
- **Manual.** Download the newer APK from Releases and install it over the existing app.

## Checking a download

Each release also has an `AntennaPod-<version>.apk.sha256` file. You can check your download with `sha256sum -c AntennaPod-<version>.apk.sha256`.

## Version names

- A release is named `<upstream versionName>-mq.<build number>`, for example `3.12.1-mq.7`, and tagged `v3.12.1-mq.7`.
- The upstream part changes when an upstream sync brings a new AntennaPod version. The number after `mq.` counts every fork build.
- The version name is shown in Settings → About.

## Known limitations

| Limitation | Why |
|---|---|
| The official Wear OS app on a watch won't connect to the fork | The Wear data layer requires the same signature on phone and watch. |
| The Play Store may list AntennaPod as not installed, or fail to update it | It isn't Play's copy. Don't install the official app again from Play: it would refuse to install over the fork. |
| Global settings must be set once after the switch | The app has no settings export. |

## If a release build fails

| Symptom | Fix |
|---|---|
| "Signing key missing" | Add the secrets above, then open the failed run under **Actions** and choose **Re-run jobs**. |
| Anything else | Open the failed run and send the error to Claude. The build log can't be read from Claude's workspace. |
