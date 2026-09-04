package com.zeroreel.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class UsageStore {
    private static final String DAY = "usage_day";
    private static final String BLOCKS = "usage_blocks";
    private static final String ALLOWED_MS = "usage_allowed_ms";
    private static final String URGE_TODAY = "urge_today_";
    private static final String URGE_TOTAL = "urge_total_";
    private static final String LAST_AT = "urge_last_at";
    private static final String LAST_APP = "urge_last_app";

    private UsageStore() {}

    static void recordBlock(Context context, BlockRules.Platform platform) {
        SharedPreferences prefs = roll(context);
        SharedPreferences.Editor editor = prefs.edit()
                .putInt(BLOCKS, prefs.getInt(BLOCKS, 0) + 1)
                .putLong(LAST_AT, System.currentTimeMillis());
        if (platform != null) {
            editor.putInt(todayKey(platform), prefs.getInt(todayKey(platform), 0) + 1);
            editor.putInt(totalKey(platform), prefs.getInt(totalKey(platform), 0) + 1);
            editor.putString(LAST_APP, platform.label);
        }
        editor.commit();
    }

    static void addAllowedMs(Context context, long deltaMs) {
        if (deltaMs <= 0) return;
        SharedPreferences prefs = roll(context);
        prefs.edit().putLong(ALLOWED_MS, prefs.getLong(ALLOWED_MS, 0L) + deltaMs).apply();
    }

    static int blocksToday(Context context) {
        return roll(context).getInt(BLOCKS, 0);
    }

    static int urgesToday(Context context, BlockRules.Platform platform) {
        return roll(context).getInt(todayKey(platform), 0);
    }

    static int urgesTotal(Context context, BlockRules.Platform platform) {
        return roll(context).getInt(totalKey(platform), 0);
    }

    static int urgesTotalAll(Context context) {
        SharedPreferences prefs = roll(context);
        int total = 0;
        for (BlockRules.Platform platform : BlockRules.Platform.values()) {
            total += prefs.getInt(totalKey(platform), 0);
        }
        return total;
    }

    static long allowedMsToday(Context context) {
        return roll(context).getLong(ALLOWED_MS, 0L);
    }

    static boolean budgetExhausted(Context context) {
        int limitMin = Prefs.dailyLimitMinutes(context);
        if (limitMin <= 0) return true;
        return allowedMsToday(context) >= limitMin * 60_000L;
    }

    static String formatAppCount(Context context, BlockRules.Platform platform) {
        return urgesTotal(context, platform) + " total  ·  " + urgesToday(context, platform) + " today";
    }

    static String formatLast(Context context) {
        SharedPreferences prefs = roll(context);
        long at = prefs.getLong(LAST_AT, 0L);
        String app = prefs.getString(LAST_APP, "");
        if (at <= 0L || app == null || app.isEmpty()) {
            return "No urges recorded yet";
        }
        long agoSec = Math.max(0L, (System.currentTimeMillis() - at) / 1000L);
        String when;
        if (agoSec < 5) when = "just now";
        else if (agoSec < 60) when = agoSec + "s ago";
        else if (agoSec < 3600) when = (agoSec / 60) + "m ago";
        else when = (agoSec / 3600) + "h ago";
        return "Last: " + app + " · " + when;
    }

    private static String todayKey(BlockRules.Platform platform) {
        return URGE_TODAY + platform.prefKey;
    }

    private static String totalKey(BlockRules.Platform platform) {
        return URGE_TOTAL + platform.prefKey;
    }

    private static SharedPreferences roll(Context context) {
        SharedPreferences prefs = Prefs.get(context);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (!today.equals(prefs.getString(DAY, ""))) {
            SharedPreferences.Editor editor = prefs.edit()
                    .putString(DAY, today)
                    .putInt(BLOCKS, 0)
                    .putLong(ALLOWED_MS, 0L);
            for (BlockRules.Platform platform : BlockRules.Platform.values()) {
                editor.putInt(todayKey(platform), 0);
            }
            editor.apply();
        }
        return prefs;
    }
}
