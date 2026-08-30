package com.zeroreel.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

final class DeviceStatus {
    private DeviceStatus() {}

    static boolean accessibilityEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (TextUtils.isEmpty(enabled)) return false;
        String needle = context.getPackageName() + "/" + ZeroReelAccessibilityService.class.getName();
        String shortNeedle = context.getPackageName() + "/.ZeroReelAccessibilityService";
        return enabled.contains(needle) || enabled.contains(shortNeedle);
    }

    static ComponentName adminComponent(Context context) {
        return new ComponentName(context, ZeroReelAdminReceiver.class);
    }

    static boolean adminActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(adminComponent(context));
    }

    static boolean strictUninstallLock(Context context) {
        return ProtectLock.isDeviceOwner(context) && ProtectLock.uninstallBlocked(context);
    }

    static boolean batteryUnrestricted(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    static boolean adbEnabled(Context context) {
        try {
            if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1) {
                return true;
            }
        } catch (Exception ignored) {
        }
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                return Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_WIFI_ENABLED, 0) == 1;
            } catch (Exception ignored) {
                try {
                    return Settings.Global.getInt(context.getContentResolver(), "adb_wifi_enabled", 0) == 1;
                } catch (Exception ignoredToo) {
                    return false;
                }
            }
        }
        return false;
    }

    static boolean googleAccountsVisible(Context context) {
        try {
            Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
            return accounts != null && accounts.length > 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
