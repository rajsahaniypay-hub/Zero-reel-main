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

    private UsageStore() {}

    static void recordBlock(Context context) {
        SharedPreferences prefs = roll(context);
        prefs.edit().putInt(BLOCKS, prefs.getInt(BLOCKS, 0) + 1).apply();
    }

    static void addAllowedMs(Context context, long deltaMs) {
        if (deltaMs <= 0) return;
        SharedPreferences prefs = roll(context);
        prefs.edit().putLong(ALLOWED_MS, prefs.getLong(ALLOWED_MS, 0L) + deltaMs).apply();
    }

    static int blocksToday(Context context) {
        return roll(context).getInt(BLOCKS, 0);
    }

    static long allowedMsToday(Context context) {
        return roll(context).getLong(ALLOWED_MS, 0L);
    }

    static boolean budgetExhausted(Context context) {
        int limitMin = Prefs.dailyLimitMinutes(context);
        if (limitMin <= 0) return true;
        return allowedMsToday(context) >= limitMin * 60_000L;
    }

    private static SharedPreferences roll(Context context) {
        SharedPreferences prefs = Prefs.get(context);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (!today.equals(prefs.getString(DAY, ""))) {
            prefs.edit()
                    .putString(DAY, today)
                    .putInt(BLOCKS, 0)
                    .putLong(ALLOWED_MS, 0L)
                    .apply();
        }
        return prefs;
    }
}
