package com.zeroreel.app;

import android.content.Context;

final class StaySignedIn {
    private StaySignedIn() {}

    static String batteryCommand(Context context) {
        return "adb shell dumpsys deviceidle whitelist +" + context.getPackageName();
    }

    static String bothCommands(Context context) {
        return AccessibilityKeeper.grantCommand(context) + "\n" + batteryCommand(context);
    }

    static boolean ready(Context context) {
        return AccessibilityKeeper.canRestore(context);
    }
}
