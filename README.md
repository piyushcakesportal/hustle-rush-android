# Hustle Rush 1.1.0

Native Android cash-survival runner updated with mission progression and AdMob monetization.

## What is included

- 10 progressively harder levels with distance goals.
- Up to three stars per level based on completion, combo, and remaining cash.
- Saved level unlocks and an Endless City mode after level 10.
- Rewarded Continue: a player may watch one ad per failed run to resume with ₹300 and one shield.
- Interstitial ads only at a natural break after every third failed-run exit/replay.
- Google UMP consent flow and an in-game privacy-options entry point when required.
- Debug builds use Google's sample ad IDs; release builds use the supplied Hustle Rush AdMob IDs.

## Project identity

- Play package: `com.hustlerush.cashrunner`
- Version: `1.1.0` (`versionCode 4`)
- Minimum Android: API 23
- Target Android: API 36
- Language: Java 17
- Google Mobile Ads SDK: `25.4.0`
- Google UMP SDK: `4.0.0`

The Java namespace intentionally remains `com.cakesportal.hustlerush`; this does not change the Play package/application ID.

## Build and test

1. Open this folder in a recent Android Studio with Android API 36 installed.
2. Let Gradle sync and run the `debug` build on a device/emulator.
3. Confirm ads are labeled as test ads. Never click live ads during testing.
4. Complete levels 1–10 and verify saved stars/unlocks after restarting.
5. Verify the rewarded Continue resumes the same run only once.
6. Verify an interstitial can appear only after every third failed-run replay/home action.
7. Test consent with the Google UMP debugging procedure before release.

## Create the Play update

Use the same Google Play upload key as the live app. Set the four signing environment variables described in `GITHUB_SIGNING_SETUP.md`, or use Android Studio's **Build > Generate Signed Bundle / APK**. Upload the resulting release `.aab` to the Production track as an update—do not create a new Play Console app.

Before sending for review, complete every item in `PLAY_CONSOLE_CHECKLIST.md` and publish the updated `privacy-policy.html` at the URL already used in Play Console.
