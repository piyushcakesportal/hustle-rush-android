# GitHub Signed AAB Setup

The normal `Build Android` workflow creates a debug APK for testing and an unsigned release bundle. Play Console needs the signed bundle produced by `Build Signed Play Bundle`.

## 1. Create an upload keystore

Run this once on a trusted computer with Java installed:

```bash
keytool -genkeypair -v \
  -keystore hustle-rush-upload.jks \
  -alias hustle-rush \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep this file and its passwords backed up privately. Never upload it as a repository file.

## 2. Convert the keystore to Base64

### Windows PowerShell

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("hustle-rush-upload.jks")) | Set-Clipboard
```

### macOS

```bash
base64 -i hustle-rush-upload.jks | pbcopy
```

### Linux

```bash
base64 -w 0 hustle-rush-upload.jks
```

## 3. Add GitHub repository secrets

Go to **Repository > Settings > Secrets and variables > Actions** and create:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## 4. Build the Play bundle

Open **Actions > Build Signed Play Bundle > Run workflow**.

Download the `hustle-rush-play-console-aab` artifact and upload its `.aab` file to the Play Console internal testing track first.
