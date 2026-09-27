# Release builds of the fork

Every time `multiple-queues` changes (you merge a PR), GitHub builds a signed release APK and publishes it under **Releases** on https://github.com/fogolin/AntennaPod/releases. The design is in [ADR-0008](../decisions/ADR-0008-fork-release-builds.md).

## One-time setup: the signing key

Do this on your own computer. The key never needs to leave it except as GitHub secrets.

### 1. Create the key

This needs `keytool`, which comes with any Java JDK and with Android Studio (`<Android Studio>/jbr/bin/keytool`).

```sh
keytool -genkeypair -v -keystore antennapod-fork.jks -alias antennapod-fork \
    -keyalg RSA -keysize 4096 -validity 10000
```

- Pick a strong password.
- The name and organisation questions can be anything.
- Modern `keytool` uses the store password for the key as well.

If you have OpenSSL but no Java:

```sh
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 10000 -nodes -subj "/CN=AntennaPod fork"
openssl pkcs12 -export -inkey key.pem -in cert.pem -name antennapod-fork -out antennapod-fork.jks
rm key.pem cert.pem
```

### 2. Back it up

**Back up `antennapod-fork.jks` and its password**, for example in your password manager. If you lose it, future builds can't update the installed app. You'd have to export your data, uninstall, install again and import.

### 3. Encode it for GitHub

| System | Command |
|---|---|
| Linux | `base64 -w0 antennapod-fork.jks > keystore.txt` |
| macOS | `base64 -i antennapod-fork.jks -o keystore.txt` |
| Windows (PowerShell) | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("antennapod-fork.jks")) \| Set-Content keystore.txt` |

### 4. Add four repository secrets

Go to https://github.com/fogolin/AntennaPod/settings/secrets/actions and choose **New repository secret**:

| Secret | Value |
|---|---|
| `FORK_KEYSTORE_BASE64` | the contents of `keystore.txt` |
| `FORK_KEYSTORE_PASSWORD` | the keystore password |
| `FORK_KEY_ALIAS` | `antennapod-fork` |
| `FORK_KEY_PASSWORD` | the same password, unless you set a separate key password |

If you use the GitHub CLI, `gh secret set FORK_KEYSTORE_BASE64 < keystore.txt` does the first one.

Then delete `keystore.txt`.

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
