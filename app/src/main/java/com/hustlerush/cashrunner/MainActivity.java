package com.hustlerush.cashrunner;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public final class MainActivity extends Activity {
    private WebView gameWebView;
    private ConsentInformation consentInformation;
    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private boolean adsInitialized;
    private boolean rewardedLoading;
    private boolean interstitialLoading;
    private int completedLevelBreaks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        configureGameWebView();
        requestConsentAndPrepareAds();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.rgb(7, 8, 15));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureGameWebView() {
        gameWebView = new WebView(this);
        gameWebView.setBackgroundColor(Color.rgb(7, 8, 15));
        gameWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        gameWebView.setLongClickable(false);
        gameWebView.setHapticFeedbackEnabled(true);
        WebSettings settings = gameWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        gameWebView.addJavascriptInterface(new GameBridge(), "NativeGame");
        gameWebView.setWebChromeClient(new WebChromeClient());
        gameWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                evaluate("window.setInternalTesting && window.setInternalTesting(" + BuildConfig.USE_TEST_ADS + ")");
            }
        });
        setContentView(gameWebView);
        gameWebView.loadUrl("file:///android_asset/debt_escape.html");
    }

    private void requestConsentAndPrepareAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> initializeAdsIfAllowed()),
                requestConsentError -> initializeAdsIfAllowed());
        initializeAdsIfAllowed();
    }

    private synchronized void initializeAdsIfAllowed() {
        if (adsInitialized || consentInformation == null || !consentInformation.canRequestAds()) return;
        adsInitialized = true;
        MobileAds.initialize(this, initializationStatus -> {
            loadRewarded();
            loadInterstitial();
        });
    }

    private void loadRewarded() {
        if (rewardedLoading || rewardedAd != null || isFinishing()) return;
        rewardedLoading = true;
        RewardedAd.load(this, BuildConfig.REWARDED_UNIT_ID, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                        evaluate("window.setRewardReady && window.setRewardReady(true)");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        rewardedLoading = false;
                        rewardedAd = null;
                        evaluate("window.setRewardReady && window.setRewardReady(false)");
                    }
                });
    }

    private void loadInterstitial() {
        if (interstitialLoading || interstitialAd != null || isFinishing()) return;
        interstitialLoading = true;
        InterstitialAd.load(this, BuildConfig.INTERSTITIAL_UNIT_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialLoading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        interstitialLoading = false;
                        interstitialAd = null;
                    }
                });
    }

    private void showRewardedContinue() {
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            evaluate("window.onRewardUnavailable && window.onRewardUnavailable()");
            loadRewarded();
            return;
        }
        rewardedAd = null;
        evaluate("window.setRewardReady && window.setRewardReady(false)");
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadRewarded(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                evaluate("window.onRewardUnavailable && window.onRewardUnavailable()");
                loadRewarded();
            }
        });
        ad.show(this, rewardItem -> evaluate("window.onRewardedContinue && window.onRewardedContinue()"));
    }

    private void showLevelBreakAd() {
        completedLevelBreaks++;
        InterstitialAd ad = interstitialAd;
        if (ad == null || completedLevelBreaks % 2 != 0) {
            loadInterstitial();
            return;
        }
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadInterstitial(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) { loadInterstitial(); }
        });
        ad.show(this);
    }

    private void showPrivacyOptions() {
        if (consentInformation == null) return;
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> initializeAdsIfAllowed());
    }

    private void evaluate(String script) {
        if (gameWebView != null) gameWebView.post(() -> gameWebView.evaluateJavascript(script, null));
    }

    private final class GameBridge {
        @JavascriptInterface public void rewardedContinue() { runOnUiThread(MainActivity.this::showRewardedContinue); }
        @JavascriptInterface public void levelCompleted(int level) { runOnUiThread(MainActivity.this::showLevelBreakAd); }
        @JavascriptInterface public void privacyOptions() { runOnUiThread(MainActivity.this::showPrivacyOptions); }
        @JavascriptInterface public void exitApp() { runOnUiThread(MainActivity.this::finish); }
    }

    @Override protected void onPause() {
        evaluate("window.pauseGame && window.pauseGame()");
        if (gameWebView != null) gameWebView.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameWebView != null) {
            gameWebView.onResume();
            evaluate("window.resumeGame && window.resumeGame()");
        }
    }

    @Override public void onBackPressed() {
        evaluate("window.handleAndroidBack && window.handleAndroidBack()");
    }

    @Override protected void onDestroy() {
        if (gameWebView != null) {
            gameWebView.removeJavascriptInterface("NativeGame");
            gameWebView.destroy();
            gameWebView = null;
        }
        super.onDestroy();
    }
}
