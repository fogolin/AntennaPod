#!/bin/bash
set -euo pipefail

KEY_ALIAS="antennapod-fork"
KEYSTORE="${1:-$HOME/.antennapod-fork/antennapod-fork.keystore}"
ENV_FILE="$(git rev-parse --show-toplevel)/.env"

if ! command -v keytool > /dev/null; then
    echo "keytool not found. Install a Java JDK, or add Android Studio's jbr/bin folder to PATH." >&2
    exit 1
fi
if [ -e "$KEYSTORE" ]; then
    echo "$KEYSTORE already exists. Refusing to overwrite a signing key." >&2
    exit 1
fi
if [ -e "$ENV_FILE" ]; then
    echo "$ENV_FILE already exists. Move it away first." >&2
    exit 1
fi

umask 077
mkdir -p "$(dirname "$KEYSTORE")"
FORK_KEYSTORE_PASSWORD=$(head -c 24 /dev/urandom | base64 | tr -d '+/=\n')
export FORK_KEYSTORE_PASSWORD

keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=AntennaPod fork" \
    -storepass:env FORK_KEYSTORE_PASSWORD -keypass:env FORK_KEYSTORE_PASSWORD

{
    echo "FORK_KEYSTORE_BASE64=$(base64 < "$KEYSTORE" | tr -d '\n')"
    echo "FORK_KEYSTORE_PASSWORD=$FORK_KEYSTORE_PASSWORD"
    echo "FORK_KEY_ALIAS=$KEY_ALIAS"
    echo "FORK_KEY_PASSWORD=$FORK_KEYSTORE_PASSWORD"
} > "$ENV_FILE"

echo
echo "Signing key: $KEYSTORE"
echo "Secrets:     $ENV_FILE (ignored by git)"
echo
echo "1. Back up both files, for example in your password manager."
echo "2. Upload the secrets:  gh secret set -f .env --repo fogolin/AntennaPod"
echo "   or copy each line into https://github.com/fogolin/AntennaPod/settings/secrets/actions"
