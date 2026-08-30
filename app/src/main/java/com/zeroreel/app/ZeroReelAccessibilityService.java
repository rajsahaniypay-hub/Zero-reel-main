package com.zeroreel.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ZeroReelAccessibilityService extends AccessibilityService {
    private static final String TAG = "ZeroReelService";
    private static final long BLOCK_COOLDOWN_MS = 1500L;
    private static final long SAMPLE_MS = 1000L;

    private long lastBlockTime = 0L;
    private long lastSampleTime = 0L;
    private PrintWriter logWriter;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.notificationTimeout = 100;
            info.packageNames = null;
            setServiceInfo(info);
        }
        if (Prefs.setupComplete(this) && Prefs.masterEnabled(this)) {
            BlockGuardService.start(this);
        }
        Log.i(TAG, "Zero Reel accessibility connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!Prefs.masterEnabled(this) || Prefs.isPaused(this)) return;

        String packageName = event.getPackageName().toString();
        if (packageName.equals(getPackageName())) return;

        BlockRules.Platform platform = BlockRules.matchPackage(packageName);
        if (platform == null) return;
        if (!Prefs.platformEnabled(this, platform.prefKey, platform.defaultEnabled)) return;

        boolean matched;
        String reason;
        if (platform.blockEntireApp) {
            matched = true;
            reason = "entire-app";
        } else {
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            String classHit = matchHint(className, BlockRules.classHints(platform));
            String viewHit = null;
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    viewHit = findViewId(root, BlockRules.viewIds(platform), 0);
                } finally {
                    root.recycle();
                }
            }
            if (viewHit != null) {
                matched = true;
                reason = viewHit;
            } else if (classHit != null) {
                matched = true;
                reason = classHit;
            } else {
                matched = false;
                reason = null;
            }
        }

        if (!matched) {
            if (Prefs.debugLog(this) && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                writeLog("SCAN [" + packageName + "] no match");
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (!UsageStore.budgetExhausted(this)) {
            if (now - lastSampleTime >= SAMPLE_MS) {
                UsageStore.addAllowedMs(this, now - (lastSampleTime == 0L ? now : lastSampleTime));
                lastSampleTime = now;
            }
            if (!UsageStore.budgetExhausted(this)) return;
        }

        if (now - lastBlockTime <= BLOCK_COOLDOWN_MS) return;
        lastBlockTime = now;
        UsageStore.recordBlock(this);
        writeLog("BLOCKED [" + packageName + "] " + reason);
        Log.w(TAG, "Blocked " + packageName + " via " + reason);

        if (platform.blockEntireApp) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
        Toast.makeText(this, getString(R.string.blocked_toast), Toast.LENGTH_SHORT).show();
    }

    private String findViewId(AccessibilityNodeInfo node, String[] targetIds, int depth) {
        if (node == null || depth > 8 || targetIds.length == 0) return null;
        try {
            String viewId = node.getViewIdResourceName();
            if (viewId != null) {
                String hit = matchHint(viewId, targetIds);
                if (hit != null) return viewId;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    String result = findViewId(child, targetIds, depth + 1);
                    child.recycle();
                    if (result != null) return result;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String matchHint(String value, String[] hints) {
        if (value == null) return null;
        String lower = value.toLowerCase(Locale.US);
        for (String hint : hints) {
            if (lower.contains(hint.toLowerCase(Locale.US))) return hint;
        }
        return null;
    }

    private void writeLog(String message) {
        if (!Prefs.debugLog(this)) {
            closeLog();
            return;
        }
        if (logWriter == null) {
            try {
                File dir = getExternalFilesDir(null);
                if (dir == null) return;
                logWriter = new PrintWriter(new FileWriter(new File(dir, "zero_reel_debug.log"), true), true);
                logWriter.println("\n=== Session " + new Date() + " ===");
            } catch (Exception e) {
                Log.e(TAG, "log open failed: " + e.getMessage());
                return;
            }
        }
        logWriter.println(timeFormat.format(new Date()) + " " + message);
    }

    private void closeLog() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }

    @Override
    public void onInterrupt() {
        closeLog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeLog();
    }
}
