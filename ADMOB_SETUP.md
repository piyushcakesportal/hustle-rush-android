# AdMob configuration used in this update

## Production identifiers

- App ID: `ca-app-pub-8512097229727629~4157317521`
- Rewarded Continue: `ca-app-pub-8512097229727629/6574376119`
- Game Over interstitial: `ca-app-pub-8512097229727629/3171538913`

Release builds use these production identifiers. Debug builds use Google's official sample identifiers, controlled through `BuildConfig.DEBUG` and a debug-only manifest placeholder.

## Placement rules

- Rewarded ad: optional, one Continue per failed run, reward granted only by the earned-reward callback.
- Interstitial: evaluated only when leaving/replaying the game-over screen and only every third such transition.
- No banner or app-open ad is included.

## Required AdMob console work

1. Complete the payment profile and any identity/address verification requested by Google.
2. Link the AdMob app to the live Google Play listing when the listing becomes searchable.
3. Create and publish the applicable consent messages under **Privacy & messaging**.
4. Keep the existing AdMob frequency cap as an additional safeguard.
5. Never test by clicking production ads. Use the debug build for test ads.
