package com.zeroreel.app;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import java.util.Collections;

final class ProtectLock {
    private ProtectLock() {}

    static String adbCommand(Context context) {
        return "adb shell dpm set-device-owner "
                + context.getPackageName()
                + "/.ZeroReelAdminReceiver";
    }

    static String requiredCommands(Context context) {
        return adbCommand(context) + "\n"
                + AccessibilityKeeper.grantCommand(context) + "\n"
                + StaySignedIn.batteryCommand(context);
    }

    static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = dpm(context);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    static boolean ready(Context context) {
        return isDeviceOwner(context)
                && uninstallBlocked(context)
                && DeviceStatus.accessibilityEnabled(context)
                && AccessibilityKeeper.canRestore(context);
    }

    static boolean uninstallBlocked(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null) return false;
        return dpm.isUninstallBlocked(DeviceStatus.adminComponent(context), context.getPackageName());
    }

    static boolean apply(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) return false;
        ComponentName admin = DeviceStatus.adminComponent(context);
        String pkg = context.getPackageName();

        dpm.setUninstallBlocked(admin, pkg, true);
        try {
            dpm.setPermittedAccessibilityServices(admin, Collections.singletonList(pkg));
        } catch (Exception ignored) {
        }
        addRestriction(dpm, admin, UserManager.DISALLOW_SAFE_BOOT);
        addRestriction(dpm, admin, UserManager.DISALLOW_ADD_USER);
        if (AccessibilityKeeper.canRestore(context)) {
            addRestriction(dpm, admin, UserManager.DISALLOW_DEBUGGING_FEATURES);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                dpm.setUserControlDisabledPackages(admin, Collections.singletonList(pkg));
            } catch (Exception ignored) {
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                dpm.setPermissionGrantState(admin, pkg, Manifest.permission.POST_NOTIFICATIONS,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            } catch (Exception ignored) {
            }
        }
        try {
            dpm.setShortSupportMessage(admin, "Zero Reel is Device Owner. Disarm with your authenticator code to uninstall.");
            dpm.setLongSupportMessage(admin, "Zero Reel uses Device Owner so Uninstall, force-stop, and battery limits stay locked. Disarm from the app with a 6-digit authenticator code. Factory reset still wipes the phone.");
        } catch (Exception ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                dpm.setOrganizationName(admin, "Zero Reel");
            } catch (Exception ignored) {
            }
        }
        AccessibilityKeeper.restoreIfAllowed(context);
        return uninstallBlocked(context);
    }

    @SuppressWarnings("deprecation")
    static void release(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null) return;
        ComponentName admin = DeviceStatus.adminComponent(context);
        try {
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dpm.setUserControlDisabledPackages(admin, Collections.emptyList());
                }
                dpm.setPermittedAccessibilityServices(admin, null);
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER);
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES);
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

    private static void addRestriction(DevicePolicyManager dpm, ComponentName admin, String key) {
        try {
            dpm.addUserRestriction(admin, key);
        } catch (Exception ignored) {
        }
    }

    private static DevicePolicyManager dpm(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
}
