# Hustle Rush 1.1.0 — Play Console Checklist

## Build identity

- Existing Play app/package: `com.hustlerush.cashrunner`
- Update version: `1.1.0`
- Update version code: `3`
- Required signing key: the same upload key used for the live release
- Upload format: signed Android App Bundle (`.aab`)

## Before uploading

1. Test the debug build; it automatically uses Google's sample ad IDs.
2. Test all 10 mission goals, stars, saved unlocks, Endless City, rewarded Continue, interstitial pacing, pause/resume, sound, and vibration.
3. Configure and publish applicable consent messages in **AdMob > Privacy & messaging**.
4. Replace the currently hosted privacy policy with the included updated `privacy-policy.html`.
5. Confirm AdMob app-store details are linked to the live Play listing when indexing becomes available.
6. Build a signed release AAB with the existing Play upload key.

## Play Console declarations to review

These must describe version 1.1.0, not the old ad-free version:

- **Contains ads:** Yes
- **App access:** All core gameplay is available without login or access restrictions
- **Account creation:** No
- **Target audience:** General/adult audience; do not select children unless the app, ads, content, and Families policies are fully configured for children
- **Permissions:** Internet and network state for ads; vibration for optional haptics
- **Data safety:** Update for Google Mobile Ads SDK data practices. Review Google's current disclosure page and your actual AdMob configuration before submitting; do not leave “No data collected” from version 1.0.
- **Privacy policy:** Use the newly hosted August 7, 2026 policy URL

## Production update flow

1. Open **Play Console > Hustle Rush > Production**.
2. Choose **Create new release**.
3. Upload the signed version-code-3 AAB.
4. Use release name `Hustle Rush 1.1.0`.
5. Paste the release notes from `RELEASE_NOTES_1.1.0.txt`.
6. Resolve blocking errors, save, review the release, and send it for review.
7. If Managed publishing is enabled, publish after Google approves it.

## Updated store-listing draft

**Short description:** Complete cash missions, dodge expenses, unlock levels, and build your fortune.

**Full description:**

Hustle Rush is a fast cash-survival runner where every rupee matters. Switch between three city lanes, collect income, build combo multipliers, and avoid tax, rent, EMI, fuel, and fines. Expenses deduct the amount shown—you lose only when your wallet reaches zero.

Take on 10 increasingly challenging city missions. Reach each distance target, protect your wallet, earn up to three stars, and unlock Endless City. Rare shields block an expense, while smart lane changes and bigger combos help you finish with more cash.

Features:
- 10 progressive cash-survival missions
- Up to three stars per level
- Unlockable Endless City mode
- Three-lane tap and swipe controls
- Cash combo multiplier up to x5
- Protective shields and varied expenses
- Saved progress and all-time best cash
- Optional rewarded Continue after a failed run
- Sound and vibration controls
- No login required

Contains ads.
