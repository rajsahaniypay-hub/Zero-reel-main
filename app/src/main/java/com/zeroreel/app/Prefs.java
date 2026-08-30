package com.zeroreel.app;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String FILE = "zero_reel_prefs";

    static final String TOTP_SECRET = "totp_secret";
    static final String TOTP_LINKED = "totp_linked";
    static final String SETUP_COMPLETE = "setup_complete";
    static final String LOCK_ARMED = "lock_armed";
    static final String MASTER_ENABLED = "master_enabled";
    static final String DAILY_LIMIT_MINUTES = "daily_limit_minutes";
    static final String PAUSE_UNTIL_MS = "pause_until_ms";
    static final String DEBUG_LOG = "debug_log";

    static final String APP_YOUTUBE = "app_youtube";
    static final String APP_INSTAGRAM = "app_instagram";
    static final String APP_TIKTOK = "app_tiktok";
    static final String APP_FACEBOOK = "app_facebook";
    static final String APP_SNAPCHAT = "app_snapchat";

    static final String FAIL_COUNT = "totp_fail_count";
    static final String FAIL_LOCK_UNTIL = "totp_fail_lock_until";

    static SharedPreferences get(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean setupComplete(Context context) {
        return get(context).getBoolean(SETUP_COMPLETE, false);
    }

    static boolean lockArmed(Context context) {
        return get(context).getBoolean(LOCK_ARMED, false);
    }

    static boolean masterEnabled(Context context) {
        return get(context).getBoolean(MASTER_ENABLED, false);
    }

    static boolean platformEnabled(Context context, String key, boolean fallback) {
        return get(context).getBoolean(key, fallback);
    }

    static int dailyLimitMinutes(Context context) {
        return get(context).getInt(DAILY_LIMIT_MINUTES, 0);
    }

    static boolean isPaused(Context context) {
        return get(context).getLong(PAUSE_UNTIL_MS, 0L) > System.currentTimeMillis();
    }

    static long pauseRemainingMs(Context context) {
        return Math.max(0L, get(context).getLong(PAUSE_UNTIL_MS, 0L) - System.currentTimeMillis());
    }

    static boolean debugLog(Context context) {
        return get(context).getBoolean(DEBUG_LOG, false);
    }

    static String totpSecret(Context context) {
        return get(context).getString(TOTP_SECRET, "");
    }

    static boolean totpLockedOut(Context context) {
        return get(context).getLong(FAIL_LOCK_UNTIL, 0L) > System.currentTimeMillis();
    }

    static long totpLockRemainingMs(Context context) {
        return Math.max(0L, get(context).getLong(FAIL_LOCK_UNTIL, 0L) - System.currentTimeMillis());
    }

    static boolean recordTotpFailure(Context context) {
        SharedPreferences prefs = get(context);
        int fails = prefs.getInt(FAIL_COUNT, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit().putInt(FAIL_COUNT, fails);
        if (fails >= 5) {
            editor.putLong(FAIL_LOCK_UNTIL, System.currentTimeMillis() + 30_000L);
            editor.putInt(FAIL_COUNT, 0);
            editor.apply();
            return true;
        }
        editor.apply();
        return false;
    }

    static void clearTotpFailures(Context context) {
        get(context).edit().putInt(FAIL_COUNT, 0).putLong(FAIL_LOCK_UNTIL, 0L).apply();
    }

    static void completeAndArm(Context context) {
        SharedPreferences prefs = get(context);
        boolean firstArm = !prefs.getBoolean(SETUP_COMPLETE, false);
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(SETUP_COMPLETE, true)
                .putBoolean(LOCK_ARMED, true)
                .putBoolean(MASTER_ENABLED, true);
        if (firstArm) {
            editor.putBoolean(APP_YOUTUBE, true)
                    .putBoolean(APP_INSTAGRAM, true)
                    .putBoolean(APP_TIKTOK, true)
                    .putBoolean(APP_FACEBOOK, true)
                    .putBoolean(APP_SNAPCHAT, true);
        }
        editor.apply();
    }
}
