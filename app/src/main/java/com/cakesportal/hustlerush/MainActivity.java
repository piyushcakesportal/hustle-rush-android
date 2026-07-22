package com.cakesportal.hustlerush;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class MainActivity extends Activity {
    private static final String CRASH_PREFS = "hustle_rush_diagnostics";
    private static final String LAST_CRASH = "last_crash";

    private HustleRushView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        installCrashRecorder();
        super.onCreate(savedInstanceState);
        configureWindowSafely();

        try {
            gameView = new HustleRushView(this);
            setContentView(gameView);
            showPreviousCrashIfPresent();
        } catch (Throwable startupError) {
            saveCrash(startupError);
            showStartupError(startupError);
        }
    }

    private void configureWindowSafely() {
        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.rgb(15, 17, 41));
            window.setNavigationBarColor(Color.rgb(9, 11, 22));
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } catch (Throwable ignored) {
            // The game can still run with the device's default window styling.
        }
    }

    private void installCrashRecorder() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            saveCrash(throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private void saveCrash(Throwable throwable) {
        try {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(LAST_CRASH, writer.toString())
                    .commit();
        } catch (Throwable ignored) {
            // Never let diagnostic recording create another crash.
        }
    }

    private void showPreviousCrashIfPresent() {
        SharedPreferences preferences = getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE);
        String report = preferences.getString(LAST_CRASH, "");
        if (report == null || report.trim().isEmpty()) return;

        preferences.edit().remove(LAST_CRASH).apply();
        String shortened = report.length() > 3500 ? report.substring(0, 3500) + "\n…" : report;

        new AlertDialog.Builder(this)
                .setTitle("Previous crash detected")
                .setMessage(shortened)
                .setPositiveButton("Continue", null)
                .show();
    }

    private void showStartupError(Throwable throwable) {
        String message = throwable.getClass().getSimpleName();
        if (throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty()) {
            message += ": " + throwable.getMessage();
        }

        new AlertDialog.Builder(this)
                .setTitle("Hustle Rush could not start")
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
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
