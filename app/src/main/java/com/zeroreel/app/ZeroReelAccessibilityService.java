package com.zeroreel.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.util.DisplayMetrics;
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
    private static final long REDIRECT_COOLDOWN_MS = 4000L;
    private static final long SAMPLE_MS = 1000L;
    private static final int TREE_DEPTH = 20;

    private long lastBlockTime = 0L;
    private long lastSampleTime = 0L;
    private PrintWriter logWriter;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private final Rect nodeBounds = new Rect();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_SELECTED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
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

        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        ScanResult scan = new ScanResult();
        scan.classHit = BlockRules.firstHint(className, BlockRules.classHints(platform));
        scan.safeSurface = BlockRules.matchHint(className, BlockRules.safeClassHints(platform));

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                walkTree(root, platform, scan, 0, metrics.widthPixels, metrics.heightPixels);
            } finally {
                root.recycle();
            }
        }

        // Chat/camera wins only when the reel viewer is not actually on screen.
        // A chat icon on Spotlight or a Messages tab on Facebook must not hide Reels.
        if (scan.safeSurface && !scan.hasReelSignal()) {
            return;
        }

        boolean matched;
        String reason;
        if (platform.blockEntireApp) {
            matched = true;
            reason = "entire-app";
        } else if (scan.viewHit != null) {
            matched = true;
            reason = scan.viewHit;
        } else if (scan.classHit != null) {
            matched = true;
            reason = scan.classHit;
        } else if (scan.contentHit != null) {
            matched = true;
            reason = scan.contentHit;
        } else if (scan.selectedHit != null) {
            matched = true;
            reason = scan.selectedHit;
        } else if (scan.facebookStructure) {
            matched = true;
            reason = "facebook-reel-structure";
        } else {
            matched = false;
            reason = null;
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

        long cooldown = BlockRules.usesChatRedirect(platform) ? REDIRECT_COOLDOWN_MS : BLOCK_COOLDOWN_MS;
        if (now - lastBlockTime <= cooldown) return;
        lastBlockTime = now;
        UsageStore.recordBlock(this, platform);
        writeLog("BLOCKED [" + packageName + "] " + reason);
        Log.w(TAG, "Blocked " + packageName + " via " + reason);

        if (platform == BlockRules.Platform.INSTAGRAM) {
            if (!openUris(packageName, instagramChatUris())) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            Toast.makeText(this, R.string.blocked_instagram_chat, Toast.LENGTH_SHORT).show();
        } else if (platform == BlockRules.Platform.SNAPCHAT) {
            if (!openUris(packageName, snapchatChatUris())) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            Toast.makeText(this, R.string.blocked_snapchat_chat, Toast.LENGTH_SHORT).show();
        } else if (platform == BlockRules.Platform.FACEBOOK) {
            if (!openUris(packageName, facebookChatUris(packageName))) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            Toast.makeText(this, R.string.blocked_facebook_messages, Toast.LENGTH_SHORT).show();
        } else if (platform.blockEntireApp) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            Toast.makeText(this, getString(R.string.blocked_toast), Toast.LENGTH_SHORT).show();
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK);
            Toast.makeText(this, getString(R.string.blocked_toast), Toast.LENGTH_SHORT).show();
        }
    }

    private void walkTree(
            AccessibilityNodeInfo node,
            BlockRules.Platform platform,
            ScanResult scan,
            int depth,
            int screenWidth,
            int screenHeight
    ) {
        if (node == null || depth > TREE_DEPTH) return;
        try {
            boolean visible = node.isVisibleToUser();
            String viewId = node.getViewIdResourceName();
            String className = node.getClassName() != null ? node.getClassName().toString() : null;
            String content = textOf(node.getContentDescription());
            String label = textOf(node.getText());

            if (visible) {
                if (!scan.safeSurface && isLargeSafeSurface(node, viewId, className, platform, screenHeight)) {
                    scan.safeSurface = true;
                }
                if (scan.viewHit == null) {
                    String hit = BlockRules.firstHint(viewId, BlockRules.viewIds(platform));
                    if (hit != null) scan.viewHit = viewId;
                }
                if (scan.classHit == null) {
                    scan.classHit = BlockRules.firstHint(className, BlockRules.classHints(platform));
                }
                if (platform == BlockRules.Platform.FACEBOOK) {
                    if (scan.contentHit == null && BlockRules.matchExactContentDesc(content, BlockRules.FACEBOOK_CONTENT_DESCS)) {
                        scan.contentHit = content;
                    }
                    if (scan.selectedHit == null) {
                        boolean selected = node.isSelected();
                        if (BlockRules.matchSelectedPrefix(content, selected, BlockRules.FACEBOOK_SELECTED_PREFIXES)) {
                            scan.selectedHit = content;
                        } else if (BlockRules.matchSelectedPrefix(label, selected, BlockRules.FACEBOOK_SELECTED_PREFIXES)) {
                            scan.selectedHit = label;
                        }
                    }
                    if (!scan.facebookStructure
                            && BlockRules.FACEBOOK_REEL_RECYCLER.equals(className)
                            && isFacebookReelStructure(node, className, screenWidth, screenHeight, depth)) {
                        scan.facebookStructure = true;
                    }
                }
            }

            if (scan.hasReelSignal()) return;

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    walkTree(child, platform, scan, depth + 1, screenWidth, screenHeight);
                    child.recycle();
                    if (scan.hasReelSignal()) return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isFacebookReelStructure(
            AccessibilityNodeInfo node,
            String className,
            int screenWidth,
            int screenHeight,
            int depth
    ) {
        if (!node.isVisibleToUser()) return false;
        node.getBoundsInScreen(nodeBounds);
        float widthFrac = screenWidth > 0 ? (float) nodeBounds.width() / screenWidth : 0f;
        float heightFrac = screenHeight > 0 ? (float) nodeBounds.height() / screenHeight : 0f;
        if (widthFrac < BlockRules.FACEBOOK_REEL_MIN_WIDTH
                || heightFrac < BlockRules.FACEBOOK_REEL_MIN_HEIGHT) {
            return false;
        }
        if (BlockRules.FACEBOOK_REEL_RECYCLER.equals(className)) {
            return node.isScrollable()
                    && hasFacebookDescendant(node, BlockRules.FACEBOOK_REEL_BUTTON, true, screenWidth, screenHeight, depth + 1);
        }
        return false;
    }

    private boolean hasFacebookDescendant(
            AccessibilityNodeInfo node,
            String targetClass,
            boolean requireLongClickable,
            int screenWidth,
            int screenHeight,
            int depth
    ) {
        if (node == null || depth > TREE_DEPTH) return false;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                String childClass = child.getClassName() != null ? child.getClassName().toString() : "";
                child.getBoundsInScreen(nodeBounds);
                float widthFrac = screenWidth > 0 ? (float) nodeBounds.width() / screenWidth : 0f;
                float heightFrac = screenHeight > 0 ? (float) nodeBounds.height() / screenHeight : 0f;
                boolean sized = child.isVisibleToUser()
                        && widthFrac >= BlockRules.FACEBOOK_REEL_MIN_WIDTH
                        && heightFrac >= BlockRules.FACEBOOK_REEL_MIN_HEIGHT;
                if (targetClass.equals(childClass) && sized && (!requireLongClickable || child.isLongClickable())) {
                    if (BlockRules.FACEBOOK_REEL_BUTTON.equals(targetClass)) {
                        if (hasFacebookDescendant(child, BlockRules.FACEBOOK_REEL_SURFACE, false, screenWidth, screenHeight, depth + 1)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                if (hasFacebookDescendant(child, targetClass, requireLongClickable, screenWidth, screenHeight, depth + 1)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private boolean openUris(String packageName, String[] uris) {
        for (String uri : uris) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage(packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        if (uris.length == 0) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uris[uris.length - 1]));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String[] instagramChatUris() {
        return new String[] {
                "instagram://direct-inbox",
                "https://www.instagram.com/direct/inbox/",
                "https://ig.me/"
        };
    }

    private static String[] snapchatChatUris() {
        return new String[] {
                "snapchat://chat",
                "snapchat://notification/chat_on_friendsfeed",
                "https://www.snapchat.com/"
        };
    }

    private static String[] facebookChatUris(String packageName) {
        if (BlockRules.isMessengerPackage(packageName)) {
            return new String[] {
                    "fb-messenger://threads",
                    "https://www.messenger.com/"
            };
        }
        return new String[] {
                "fb://messaging",
                "fb://messages",
                "https://www.facebook.com/messages",
                "https://m.facebook.com/messages"
        };
    }

    private static String textOf(CharSequence value) {
        return value != null ? value.toString() : null;
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

    private static final class ScanResult {
        String viewHit;
        String classHit;
        String contentHit;
        String selectedHit;
        boolean safeSurface;
        boolean facebookStructure;

        boolean hasReelSignal() {
            return viewHit != null || classHit != null || contentHit != null || selectedHit != null || facebookStructure;
        }
    }

    private boolean isLargeSafeSurface(
            AccessibilityNodeInfo node,
            String viewId,
            String className,
            BlockRules.Platform platform,
            int screenHeight
    ) {
        boolean idHit = BlockRules.matchHint(viewId, BlockRules.safeViewIds(platform));
        boolean classHit = BlockRules.matchHint(className, BlockRules.safeClassHints(platform));
        if (!idHit && !classHit) return false;
        if (classHit && className != null && className.toLowerCase(Locale.US).contains("activity")) {
            return true;
        }
        node.getBoundsInScreen(nodeBounds);
        return screenHeight > 0 && nodeBounds.height() >= screenHeight * 0.4f;
    }
}
