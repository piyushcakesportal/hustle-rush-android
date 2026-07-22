# Hustle Rush

A polished offline Android cash-survival runner built with the native Android Canvas API.

## Game rules

- Start with ₹300.
- Collect cash bundles to increase the wallet.
- Consecutive pickups create a combo multiplier up to x5.
- Tax, rent, EMI, fuel, and fines deduct the exact value shown.
- A rare shield blocks one bill.
- The run ends only when the wallet reaches ₹0.
- The score is the highest cash balance reached during the run.

## Project details

- Package: `com.cakesportal.hustlerush`
- Version: `1.0.0` (`versionCode 1`)
- Minimum Android: API 23 / Android 6.0
- Target Android: API 36 / Android 16
- Language: Java 17
- Third-party libraries: none
- Internet permission: none
- Data collection: none

## Build locally

1. Open the project in a recent Android Studio version with Android 16 SDK installed.
2. Let Gradle sync.
3. Run the `app` configuration on a phone or emulator.
4. For an APK: **Build > Build APK(s)**.
5. For Play Console: **Build > Generate Signed Bundle / APK > Android App Bundle**.

## Build on GitHub

1. Create an empty GitHub repository.
2. Upload every file and folder from this project.
3. Push to the `main` branch.
4. Open **Actions > Build Android > Run workflow**.
5. Download the debug APK artifact for testing.
6. For a Play Console bundle, follow `GITHUB_SIGNING_SETUP.md` and run the `Build Signed Play Bundle` workflow.

## Play Store preparation

Read `PLAY_CONSOLE_CHECKLIST.md` and `GITHUB_SIGNING_SETUP.md`. Host `privacy-policy.html` at a public HTTPS URL before production submission. Ready-made store graphics are inside `store-assets/`.

## Important signing note

Do not upload a debug APK to production. Google Play requires a signed Android App Bundle for a new app. Keep your upload keystore private and backed up.
