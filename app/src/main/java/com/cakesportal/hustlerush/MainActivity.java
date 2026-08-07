package com.cakesportal.hustlerush;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;

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
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public final class MainActivity extends Activity implements HustleRushView.AdHost {
    private static final String LIVE_REWARDED_ID = "ca-app-pub-8512097229727629/6574376119";
    private static final String LIVE_INTERSTITIAL_ID = "ca-app-pub-8512097229727629/3171538913";
    private static final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    private static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    private HustleRushView gameView;
    private ConsentInformation consentInformation;
    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private boolean adsInitialized;
    private boolean rewardedLoading;
    private boolean interstitialLoading;
    private int failedRunTransitions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        gameView = new HustleRushView(this, this);
        setContentView(gameView);
        requestConsentAndStartAds();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.rgb(9, 11, 22));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void requestConsentAndStartAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters parameters = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                parameters,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> startAdsIfAllowed()),
                requestConsentError -> startAdsIfAllowed());

        if (consentInformation.canRequestAds()) startAdsIfAllowed();
    }

    private void startAdsIfAllowed() {
        if (consentInformation == null || !consentInformation.canRequestAds()) return;
        if (!adsInitialized) {
            adsInitialized = true;
            MobileAds.initialize(this, initializationStatus -> {
                loadRewardedAd();
                loadInterstitialAd();
            });
        } else {
            loadRewardedAd();
            loadInterstitialAd();
        }
    }

    private String rewardedId() {
        return BuildConfig.DEBUG ? TEST_REWARDED_ID : LIVE_REWARDED_ID;
    }

    private String interstitialId() {
        return BuildConfig.DEBUG ? TEST_INTERSTITIAL_ID : LIVE_INTERSTITIAL_ID;
    }

    private void loadRewardedAd() {
        if (!adsInitialized || rewardedLoading || rewardedAd != null) return;
        rewardedLoading = true;
        RewardedAd.load(this, rewardedId(), new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                        if (gameView != null) gameView.onAdAvailabilityChanged();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedLoading = false;
                        rewardedAd = null;
                        if (gameView != null) gameView.onAdAvailabilityChanged();
                    }
                });
    }

    private void loadInterstitialAd() {
        if (!adsInitialized || interstitialLoading || interstitialAd != null) return;
        interstitialLoading = true;
        InterstitialAd.load(this, interstitialId(), new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialLoading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
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
    public void showRewardedContinue(@NonNull Runnable onRewarded, @NonNull Runnable onUnavailable) {
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            loadRewardedAd();
            onUnavailable.run();
            return;
        }

        rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                loadRewardedAd();
                if (gameView != null) gameView.onAdAvailabilityChanged();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                loadRewardedAd();
                onUnavailable.run();
            }
        });
        ad.show(this, rewardItem -> onRewarded.run());
    }

    @Override
    public void maybeShowInterstitial(@NonNull Runnable afterAd) {
        failedRunTransitions++;
        if (failedRunTransitions % 3 != 0 || interstitialAd == null) {
            loadInterstitialAd();
            afterAd.run();
            return;
        }

        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitialAd();
                afterAd.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                loadInterstitialAd();
                afterAd.run();
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
        UserMessagingPlatform.showPrivacyOptionsForm(this, this::onPrivacyOptionsClosed);
    }

    private void onPrivacyOptionsClosed(FormError formError) {
        startAdsIfAllowed();
        if (gameView != null) gameView.onAdAvailabilityChanged();
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
