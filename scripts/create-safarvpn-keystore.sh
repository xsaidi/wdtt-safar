#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="$ROOT_DIR/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/safarvpn.keystore"
ENV_FILE="$KEYSTORE_DIR/safarvpn-signing.env"
ALIAS="safarvpn"

mkdir -p "$KEYSTORE_DIR"
chmod 700 "$KEYSTORE_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
  echo "Keystore already exists: $KEYSTORE_FILE"
  exit 0
fi

password="${SAFARVPN_KEYSTORE_PASSWORD:-}"
if [[ -z "$password" ]]; then
  password="$(openssl rand -hex 24)"
  umask 077
  {
    echo "SAFARVPN_KEYSTORE_PASSWORD=$password"
    echo "SAFARVPN_KEY_PASSWORD=$password"
  } > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi

keytool -genkeypair -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$password" \
  -keypass "${SAFARVPN_KEY_PASSWORD:-$password}" \
  -dname "CN=SafarVPN, OU=Android, O=SafarVPN, L=Dubai, ST=Dubai, C=AE" \
  -noprompt

chmod 600 "$KEYSTORE_FILE"

echo "Keystore created: $KEYSTORE_FILE"
if [[ -f "$ENV_FILE" ]]; then
  echo "Signing env saved: $ENV_FILE"
fi
