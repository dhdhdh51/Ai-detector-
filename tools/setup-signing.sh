#!/usr/bin/env bash
#
# FitBudget signing setup.
#
# Creates (or imports) the release signing material for Android and iOS and stores it as GitHub
# Actions secrets, so the release workflows can sign automatically.
#
#   ./tools/setup-signing.sh android      # generate an Android keystore + upload secrets
#   ./tools/setup-signing.sh ios          # upload iOS signing material you already exported
#   ./tools/setup-signing.sh status       # show which secrets are configured
#
# SECURITY
#   * Private keys are NEVER written into the repository. The Android keystore is created in
#     ./signing/ which is git-ignored, and you are told to back it up somewhere safe.
#   * Secrets are uploaded straight to GitHub with `gh secret set`; nothing is printed to stdout.
#   * If you lose the Android keystore you can never update the same app listing on Google Play
#     again, so keep an offline copy.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
SIGNING_DIR="$REPO_ROOT/signing"

info()  { printf '\033[0;36m==>\033[0m %s\n' "$*"; }
warn()  { printf '\033[0;33m[!]\033[0m %s\n' "$*"; }
die()   { printf '\033[0;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }

require() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not installed."
}

base64_flat() {
  # -w0 on GNU coreutils, no wrapping flag needed on macOS/BSD.
  if base64 --help 2>&1 | grep -q '\-w'; then base64 -w0 "$1"; else base64 "$1" | tr -d '\n'; fi
}

set_secret() {
  local name="$1" value="$2"
  printf '%s' "$value" | gh secret set "$name" --body - >/dev/null
  info "stored secret $name"
}

set_secret_from_file() {
  local name="$1" file="$2"
  gh secret set "$name" < "$file" >/dev/null
  info "stored secret $name"
}

# ---------------------------------------------------------------- Android

setup_android() {
  require keytool
  require gh

  mkdir -p "$SIGNING_DIR"
  local keystore="$SIGNING_DIR/fitbudget-release.jks"

  if [[ -f "$keystore" ]]; then
    warn "$keystore already exists - reusing it."
    read -r -s -p "Keystore password: " STORE_PASS; echo
    read -r -p "Key alias [fitbudget]: " ALIAS; ALIAS="${ALIAS:-fitbudget}"
    read -r -s -p "Key password (blank = same as keystore): " KEY_PASS; echo
    KEY_PASS="${KEY_PASS:-$STORE_PASS}"
  else
    info "Creating a new 4096-bit RSA keystore valid for 10,000 days."
    read -r -p "Key alias [fitbudget]: " ALIAS; ALIAS="${ALIAS:-fitbudget}"
    read -r -s -p "Choose a keystore password (min 6 chars): " STORE_PASS; echo
    read -r -s -p "Repeat the password: " STORE_PASS2; echo
    [[ "$STORE_PASS" == "$STORE_PASS2" ]] || die "Passwords do not match."
    [[ ${#STORE_PASS} -ge 6 ]] || die "Password must be at least 6 characters."
    KEY_PASS="$STORE_PASS"

    read -r -p "Your name or organisation [FitBudget]: " CN; CN="${CN:-FitBudget}"
    read -r -p "Two-letter country code [IN]: " COUNTRY; COUNTRY="${COUNTRY:-IN}"

    keytool -genkeypair \
      -alias "$ALIAS" \
      -keyalg RSA -keysize 4096 -validity 10000 \
      -keystore "$keystore" \
      -storetype PKCS12 \
      -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
      -dname "CN=$CN, OU=FitBudget, O=FitBudget, C=$COUNTRY"

    info "Keystore created at $keystore"
  fi

  # Local signing config so `./gradlew assembleRelease` signs on this machine too.
  cat > "$REPO_ROOT/keystore.properties" <<EOF
# Local release signing for FitBudget. This file is git-ignored - never commit it.
storeFile=$keystore
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$KEY_PASS
EOF
  info "wrote keystore.properties (git-ignored)"

  info "Uploading GitHub Actions secrets..."
  set_secret "ANDROID_KEYSTORE_BASE64" "$(base64_flat "$keystore")"
  set_secret "ANDROID_KEYSTORE_PASSWORD" "$STORE_PASS"
  set_secret "ANDROID_KEY_ALIAS" "$ALIAS"
  set_secret "ANDROID_KEY_PASSWORD" "$KEY_PASS"

  echo
  info "Android signing is ready. Certificate fingerprint:"
  keytool -list -v -keystore "$keystore" -alias "$ALIAS" -storepass "$STORE_PASS" \
    | grep -E 'SHA1|SHA256' || true
  echo
  warn "BACK UP $keystore AND ITS PASSWORD SOMEWHERE SAFE."
  warn "Losing it means you can never ship an update to the same Play listing."
}

# ---------------------------------------------------------------- iOS

setup_ios() {
  require gh

  cat <<'EOF'

iOS signing needs material that only Apple can issue, exported from your Mac or the
Apple Developer portal:

  1. A "Apple Distribution" certificate exported from Keychain Access as a .p12 file
     (right-click the certificate -> Export -> Personal Information Exchange).
  2. An App Store (or Ad Hoc) provisioning profile (.mobileprovision) for com.fitbudget.app,
     downloaded from developer.apple.com -> Certificates, Identifiers & Profiles.
  3. Your 10-character Team ID (developer.apple.com -> Membership).

Optionally, for fully automatic signing (Xcode manages certificates itself), an App Store
Connect API key instead of 1 and 2:

  4. An API key (.p8) from App Store Connect -> Users and Access -> Integrations -> App Store
     Connect API, plus its Key ID and Issuer ID.

EOF

  read -r -p "Set up with (1) certificate + profile, or (2) App Store Connect API key? [1/2]: " MODE
  read -r -p "Apple Team ID: " TEAM_ID
  [[ -n "$TEAM_ID" ]] || die "Team ID is required."
  set_secret "IOS_TEAM_ID" "$TEAM_ID"

  if [[ "$MODE" == "2" ]]; then
    read -r -p "Path to the .p8 API key: " P8_PATH
    [[ -f "$P8_PATH" ]] || die "No file at $P8_PATH"
    read -r -p "Key ID: " KEY_ID
    read -r -p "Issuer ID: " ISSUER_ID

    set_secret "APPSTORE_API_PRIVATE_KEY" "$(cat "$P8_PATH")"
    set_secret "APPSTORE_API_KEY_ID" "$KEY_ID"
    set_secret "APPSTORE_API_ISSUER_ID" "$ISSUER_ID"
    info "Automatic signing configured (xcodebuild -allowProvisioningUpdates)."
  else
    read -r -p "Path to the distribution .p12: " P12_PATH
    [[ -f "$P12_PATH" ]] || die "No file at $P12_PATH"
    read -r -s -p "Password for the .p12: " P12_PASS; echo
    read -r -p "Path to the .mobileprovision: " PROFILE_PATH
    [[ -f "$PROFILE_PATH" ]] || die "No file at $PROFILE_PATH"

    set_secret "IOS_CERTIFICATE_P12_BASE64" "$(base64_flat "$P12_PATH")"
    set_secret "IOS_CERTIFICATE_PASSWORD" "$P12_PASS"
    set_secret "IOS_PROVISIONING_PROFILE_BASE64" "$(base64_flat "$PROFILE_PATH")"
    # Random keychain password, only ever used inside the CI runner.
    set_secret "IOS_KEYCHAIN_PASSWORD" "$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32)"
    info "Manual signing configured with your certificate and profile."
  fi

  echo
  info "iOS signing is ready. Tag a release (git tag v1.0.0 && git push --tags) to build a signed IPA."
}

# ---------------------------------------------------------------- status

show_status() {
  require gh
  info "Secrets currently configured in this repository:"
  local expected=(
    ANDROID_KEYSTORE_BASE64 ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD
    IOS_TEAM_ID IOS_CERTIFICATE_P12_BASE64 IOS_CERTIFICATE_PASSWORD
    IOS_PROVISIONING_PROFILE_BASE64 IOS_KEYCHAIN_PASSWORD
    APPSTORE_API_PRIVATE_KEY APPSTORE_API_KEY_ID APPSTORE_API_ISSUER_ID
  )
  local present
  present="$(gh secret list --json name --jq '.[].name' 2>/dev/null || gh secret list | awk '{print $1}')"
  for name in "${expected[@]}"; do
    if printf '%s\n' "$present" | grep -qx "$name"; then
      printf '  \033[0;32m✓\033[0m %s\n' "$name"
    else
      printf '  \033[0;90m·\033[0m %s (not set)\n' "$name"
    fi
  done
  echo
  info "Release builds fall back to debug signing for any platform whose secrets are missing."
}

case "${1:-}" in
  android) setup_android ;;
  ios)     setup_ios ;;
  status)  show_status ;;
  *)
    cat <<EOF
FitBudget signing setup

Usage:
  ./tools/setup-signing.sh android   Create an Android release keystore and store it as secrets
  ./tools/setup-signing.sh ios       Store your Apple signing material as secrets
  ./tools/setup-signing.sh status    Show which signing secrets are configured

Requires the GitHub CLI (gh) to be installed and authenticated: gh auth login
EOF
    exit 1
    ;;
esac
