package com.hustlerush.cashrunner;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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

public final class MainActivity extends Activity implements HustleRushView.AdHost {
    // Google demo units are intentional in version 7 Internal Testing.
    private static final String REWARDED_TEST_UNIT = "ca-app-pub-3940256099942544/5224354917";
    private static final String INTERSTITIAL_TEST_UNIT = "ca-app-pub-3940256099942544/1033173712";

    private HustleRushView gameView;
    private ConsentInformation consentInformation;
    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private boolean adsInitialized;
    private boolean rewardedLoading;
    private boolean interstitialLoading;
    private int breakCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        gameView = new HustleRushView(this, this);
        setContentView(gameView);
        requestConsentAndPrepareAds();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.rgb(9, 11, 22));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void requestConsentAndPrepareAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> {
                            initializeAdsIfAllowed();
                            refreshGameAdState();
                        }),
                requestConsentError -> {
                    initializeAdsIfAllowed();
                    refreshGameAdState();
                });

        initializeAdsIfAllowed();
    }

    private synchronized void initializeAdsIfAllowed() {
        if (adsInitialized || consentInformation == null || !consentInformation.canRequestAds()) return;
        adsInitialized = true;
        new Thread(() -> MobileAds.initialize(this, initializationStatus ->
                runOnUiThread(() -> {
                    loadRewarded();
                    loadInterstitial();
                }))).start();
    }

    private void loadRewarded() {
        if (rewardedLoading || rewardedAd != null || isFinishing()) return;
        rewardedLoading = true;
        RewardedAd.load(
                this,
                REWARDED_TEST_UNIT,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                        refreshGameAdState();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        rewardedLoading = false;
                        rewardedAd = null;
                        refreshGameAdState();
                    }
                });
    }

    private void loadInterstitial() {
        if (interstitialLoading || interstitialAd != null || isFinishing()) return;
        interstitialLoading = true;
        InterstitialAd.load(
                this,
                INTERSTITIAL_TEST_UNIT,
                new AdRequest.Builder().build(),
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

    @Override
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }

    @Override
    public void showRewardedContinue(Runnable onReward) {
        RewardedAd ad = rewardedAd;
        if (ad == null) return;
        rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                loadRewarded();
                refreshGameAdState();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                loadRewarded();
                refreshGameAdState();
            }
        });
        ad.show(this, rewardItem -> onReward.run());
        refreshGameAdState();
    }

    @Override
    public void showBreakInterstitial(Runnable afterAd) {
        breakCounter++;
        InterstitialAd ad = interstitialAd;
        if (ad == null || breakCounter % 2 != 0) {
            afterAd.run();
            loadInterstitial();
            return;
        }

        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            private boolean finished;

            private void finishOnce() {
                if (finished) return;
                finished = true;
                afterAd.run();
                loadInterstitial();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                finishOnce();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                finishOnce();
            }
        });
        ad.show(this);
    }

    @Override
    public boolean isPrivacyOptionsRequired() {
        return consentInformation != null
                && consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @Override
    public void showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
            initializeAdsIfAllowed();
            refreshGameAdState();
        });
    }

    private void refreshGameAdState() {
        if (gameView != null) gameView.refreshAdState();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.pauseFromActivity();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.handleBackPressed()) return;
        super.onBackPressed();
    }
}
