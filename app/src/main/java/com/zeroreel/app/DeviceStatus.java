package com.zeroreel.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }
}
