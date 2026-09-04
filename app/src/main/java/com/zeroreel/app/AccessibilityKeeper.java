package com.zeroreel.app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

final class AccessibilityKeeper {
    private AccessibilityKeeper() {}

    static String serviceFlat(Context context) {
        return context.getPackageName() + "/" + ZeroReelAccessibilityService.class.getName();
    }

    static String grantCommand(Context context) {
        return "adb shell pm grant " + context.getPackageName()
                + " android.permission.WRITE_SECURE_SETTINGS";
    }

    static boolean canRestore(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean restoreIfAllowed(Context context) {
        if (DeviceStatus.accessibilityEnabled(context)) return true;
        if (!canRestore(context)) return false;
        try {
            ContentResolver resolver = context.getContentResolver();
            String current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String ours = serviceFlat(context);
            String next;
            if (TextUtils.isEmpty(current) || "null".equals(current)) {
                next = ours;
            } else if (current.contains(ours) || current.contains(context.getPackageName() + "/.ZeroReelAccessibilityService")) {
                next = current;
            } else {
                next = current + ":" + ours;
            }
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next);
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return DeviceStatus.accessibilityEnabled(context);
        } catch (SecurityException ignored) {
            return false;
        }
    }
}
