package com.zeroreel.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

final class ProtectLock {
    private ProtectLock() {}

    static String adbCommand(Context context) {
        return "adb shell dpm set-device-owner "
                + context.getPackageName()
                + "/.ZeroReelAdminReceiver";
    }

    static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = dpm(context);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    static boolean uninstallBlocked(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null) return false;
        return dpm.isUninstallBlocked(DeviceStatus.adminComponent(context), context.getPackageName());
    }

    static boolean apply(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) return false;
        dpm.setUninstallBlocked(DeviceStatus.adminComponent(context), context.getPackageName(), true);
        return uninstallBlocked(context);
    }

    @SuppressWarnings("deprecation")
    static void release(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null) return;
        ComponentName admin = DeviceStatus.adminComponent(context);
        try {
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                dpm.setUninstallBlocked(admin, context.getPackageName(), false);
                dpm.clearDeviceOwnerApp(context.getPackageName());
            }
        } catch (Exception ignored) {
        }
        try {
            if (dpm.isAdminActive(admin)) {
                dpm.removeActiveAdmin(admin);
            }
        } catch (Exception ignored) {
        }
    }

    private static DevicePolicyManager dpm(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
}
