# GitHub signed AAB setup for the existing Play app

The live package `com.hustlerush.cashrunner` must be updated with the **same Play upload key** used for version code 2. Do not generate a new key.

Your uploaded `Hustle-Rush-cashrunner-CORRECT-Play-Package` archive contains the correct upload-key backup. Keep that archive private and never commit the key or its passwords to GitHub.

## 1. Prepare the existing keystore

Extract the upload-key backup on a trusted computer. Use the keystore file, alias, store password, and key password recorded with that backup.

## 2. Convert only the keystore file to Base64

### Windows PowerShell

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("existing-upload-key.jks")) | Set-Clipboard
```

### macOS

```bash
base64 -i existing-upload-key.jks | pbcopy
```

### Linux

```bash
base64 -w 0 existing-upload-key.jks
```

## 3. Add private GitHub Actions secrets

Under **Repository > Settings > Secrets and variables > Actions**, create:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never paste these values into source files or commit them.

## 4. Build and verify

1. Open **Actions > Build Signed Play Bundle > Run workflow**.
2. Download `hustle-rush-play-console-aab`.
3. Upload it to an Internal testing release first. Play Console will reject it immediately if the package or signing key is wrong.
4. After device testing, use the same AAB or promote the tested release to Production.
