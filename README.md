# Hustle Rush 1.2.0 Internal Test

Native Android cash-survival runner updated with mission progression and AdMob monetization.

## What is included

- 10 distinct hard-start missions with different speed, spawn, cash, and bill rules.
- Safe-lane gates, chained obstacle waves, pressure phases, and a final sprint.
- A temporary 2× income power-up alongside the existing shield mechanic.
- Up to three stars per level based on completion, combo, and remaining cash.
- Saved level unlocks and an Endless City mode after level 10.
- Rewarded Continue: a player may watch one ad per failed run to resume with ₹300 and one shield.
- Interstitial ads only at natural replay/next-level breaks, never during active play.
- Google UMP consent flow and an in-game privacy-options entry point when required.
- Version 7 uses Google's demo ad units for safe Internal Testing. Live units are enabled only after testing.

## Project identity

- Play package: `com.hustlerush.cashrunner`
- Version: `1.2.0` (`versionCode 7`)
- Minimum Android: API 23
- Target Android: API 36
- Language: Java 17
- Google Mobile Ads SDK: `25.4.0`
- Google UMP SDK: `4.0.0`

The Java namespace and Play package/application ID use `com.hustlerush.cashrunner`.

## Build and test

1. Open this folder in a recent Android Studio with Android API 36 installed.
2. Let Gradle sync and run the `debug` build on a device/emulator.
3. Confirm ads are labeled as test ads. Never click live ads during testing.
4. Complete levels 1–10 and verify saved stars/unlocks after restarting.
5. Verify the rewarded Continue resumes the same run only once.
6. Verify an interstitial can appear only at a replay or next-level transition.
7. Test consent with the Google UMP debugging procedure before release.

## Create the Play update

Use the same Google Play upload key as the live app. Set the four signing environment variables described in `GITHUB_SIGNING_SETUP.md`, or use Android Studio's **Build > Generate Signed Bundle / APK**. Upload version 7 only to Internal Testing. After gameplay, consent, rewarded, and interstitial behavior pass testing, create the live-ad production build with a new version code.

Before sending for review, complete every item in `PLAY_CONSOLE_CHECKLIST.md` and publish the updated `privacy-policy.html` at the URL already used in Play Console.
