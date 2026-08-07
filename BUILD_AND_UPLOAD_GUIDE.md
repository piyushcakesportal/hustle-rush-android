# Build and upload Hustle Rush 1.1.0

The ZIP containing this file is the editable source project. Google Play does **not** accept this source ZIP. You must create a signed `.aab` update from it.

## A. Test the update

1. Extract `Hustle-Rush-1.1.0-levels-admob-source.zip`.
2. In Android Studio, choose **Open** and select the extracted `hustle-rush-android` folder.
3. Allow Gradle sync to finish. Install Android API 36 if Android Studio requests it.
4. Connect an Android phone with USB debugging, or start an emulator.
5. Click **Run**. This debug build uses Google's sample ads, so it is safe for testing.
6. Confirm level unlocks remain after closing/reopening the app and that rewarded/interstitial ads say **Test Ad**.

## B. Generate the signed Play bundle

1. Extract your private `Hustle-Rush-cashrunner-CORRECT-upload-key-KEEP-SAFE.zip` on a trusted computer.
2. In Android Studio, choose **Build > Generate Signed Bundle / APK**.
3. Select **Android App Bundle**, then **Next**.
4. Choose the existing `.jks`/keystore from the correct-key backup.
5. Enter the existing alias, store password, and key password from that backup. Do not create a new key.
6. Select the **release** build variant and finish.
7. The expected bundle is under `app/release/` or `app/build/outputs/bundle/release/`.

## C. Upload safely

1. Open the existing Hustle Rush app in Play Console.
2. Create an **Internal testing** release first and upload the new AAB.
3. Confirm Play Console recognizes package `com.hustlerush.cashrunner`, version name `1.1.0`, and version code `3`.
4. Install the internal-test update over the currently installed Play version. Confirm old settings/high score remain and all new features work.
5. Promote the tested release to Production, or create a Production release with the same tested AAB.
6. Use release name `Hustle Rush 1.1.0` and paste `RELEASE_NOTES_1.1.0.txt`.

If Play Console reports a signing-certificate mismatch, stop. That means the wrong upload key was selected; use the correct-key backup whose upload certificate was used for the live app.
