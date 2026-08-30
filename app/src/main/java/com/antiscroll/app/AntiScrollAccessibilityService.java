package com.antiscroll.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AntiScrollAccessibilityService extends AccessibilityService {

    private static final String TAG = "AntiScrollService";
    private static final String PREFS_NAME = "AntiScrollPrefs";

    private static final String[] YOUTUBE_PACKAGES = {
        "com.google.android.youtube",
        "app.revanced.android.youtube"
    };
    private static final String[] INSTAGRAM_PACKAGES = {
        "com.instagram.android"
    };

    private static final String[] SHORTS_VIEW_IDS = {
        "reel_recycler",
        "reel_player_page",
        "shorts_container",
        "shorts_shelf",
        "reel_watch_player",
        "shorts_player_controls",
        "shorts_video_list"
    };
    private static final String[] REELS_VIEW_IDS = {
        "clips_viewer_view_pager",
        "reel_viewer_title",
        "reel_viewer",
        "clips_video_container"
    };

    private long lastBlockTime = 0;
    private static final long BLOCK_COOLDOWN_MS = 1500;

    private PrintWriter logWriter;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "AntiScroll Service connected");

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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean masterEnabled = prefs.getBoolean("master_toggle", false);
        boolean debugLog = prefs.getBoolean("debug_log_toggle", false);

        manageLogFile(debugLog);

        if (!masterEnabled) return;

        boolean isYoutube = matchesPackage(packageName, YOUTUBE_PACKAGES);
        boolean isInstagram = matchesPackage(packageName, INSTAGRAM_PACKAGES);
        if (!isYoutube && !isInstagram) return;

        if (isYoutube && !prefs.getBoolean("app_youtube", false)) return;
        if (isInstagram && !prefs.getBoolean("app_instagram", false)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            String matchedId = findViewId(root,
                    isYoutube ? SHORTS_VIEW_IDS : REELS_VIEW_IDS, 0);

            if (matchedId != null) {
                long now = System.currentTimeMillis();
                if (now - lastBlockTime > BLOCK_COOLDOWN_MS) {
                    lastBlockTime = now;
                    Log.w(TAG, "BLOCKED [" + packageName + "] matched: " + matchedId);
                    writeLog("BLOCKED [" + packageName + "] matched: " + matchedId);
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
            } else if (debugLog && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String cls = event.getClassName() != null ? event.getClassName().toString() : "?";
                writeLog("SCAN [" + packageName + "] class=" + cls + " -> no match");
            }
        } catch (Exception ignored) {
        } finally {
            root.recycle();
        }
    }

    // Recursively search for known view IDs in the node tree
    private String findViewId(AccessibilityNodeInfo node, String[] targetIds, int depth) {
        if (node == null || depth > 8) return null;

        try {
            String viewId = node.getViewIdResourceName();
            if (viewId != null) {
                for (String target : targetIds) {
                    if (viewId.contains(target)) return viewId;
                }
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

    private void manageLogFile(boolean shouldLog) {
        if (shouldLog && logWriter == null) {
            try {
                File dir = getExternalFilesDir(null);
                if (dir != null) {
                    File logFile = new File(dir, "antiscroll_debug.log");
                    logWriter = new PrintWriter(new FileWriter(logFile, true), true);
                    logWriter.println("\n=== Session " + new Date() + " ===");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to open log: " + e.getMessage());
            }
        } else if (!shouldLog && logWriter != null) {
            logWriter.println("=== End " + new Date() + " ===");
            logWriter.close();
            logWriter = null;
        }
    }

    private void writeLog(String message) {
        if (logWriter != null) {
            logWriter.println(timeFormat.format(new Date()) + " " + message);
        }
    }

    private boolean matchesPackage(String pkg, String[] targets) {
        for (String t : targets) {
            if (t.equals(pkg)) return true;
        }
        return false;
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

    private void closeLog() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }
}
