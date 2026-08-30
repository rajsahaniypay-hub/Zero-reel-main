package com.zeroreel.app;

/**
 * Short-form blocking signatures.
 * YouTube Shorts and Instagram Reels view IDs are taken from AntiScroll
 * (yadavnikhil03/AntiScroll, GPL-3.0). Facebook Reels content-description
 * and Snapchat Spotlight view IDs follow Scrolless
 * (duartebarbosadev/Scrolless, GPL-3.0).
 */
final class BlockRules {

    enum Platform {
        YOUTUBE(Prefs.APP_YOUTUBE, true, false, "YouTube Shorts"),
        INSTAGRAM(Prefs.APP_INSTAGRAM, true, false, "Instagram Reels"),
        TIKTOK(Prefs.APP_TIKTOK, true, true, "TikTok"),
        FACEBOOK(Prefs.APP_FACEBOOK, true, false, "Facebook Reels"),
        SNAPCHAT(Prefs.APP_SNAPCHAT, true, false, "Snapchat Spotlight");

        final String prefKey;
        final boolean defaultEnabled;
        final boolean blockEntireApp;
        final String label;

        Platform(String prefKey, boolean defaultEnabled, boolean blockEntireApp, String label) {
            this.prefKey = prefKey;
            this.defaultEnabled = defaultEnabled;
            this.blockEntireApp = blockEntireApp;
            this.label = label;
        }
    }

    static final String[] YOUTUBE_PACKAGES = {
            "com.google.android.youtube",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube",
            "com.google.android.apps.youtube.mango"
    };

    static final String[] INSTAGRAM_PACKAGES = {
            "com.instagram.android",
            "com.instagram.lite"
    };

    static final String[] TIKTOK_PACKAGES = {
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme"
    };

    static final String[] FACEBOOK_PACKAGES = {
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.orca"
    };

    static final String[] SNAPCHAT_PACKAGES = {
            "com.snapchat.android"
    };

    // AntiScroll YouTube Shorts IDs + extra Shorts surfaces
    static final String[] YOUTUBE_VIEW_IDS = {
            "reel_recycler",
            "reel_player_page",
            "shorts_container",
            "shorts_shelf",
            "reel_watch_player",
            "shorts_player_controls",
            "shorts_video_list",
            "reel_player_overlay",
            "shorts_player",
            "reel_watch_fragment",
            "pivot_shorts"
    };

    static final String[] YOUTUBE_CLASS_HINTS = {
            "shorts",
            "reelwatch",
            "reel_watch",
            "shortsvideo"
    };

    // Viewer IDs, including the overlay window that opens from feed/explore.
    // Matching still requires a large on-screen node so the home tray is ignored.
    static final String[] INSTAGRAM_VIEW_IDS = {
            "clips_viewer_view_pager",
            "clips_video_container",
            "clips_viewer_video_layout",
            "clips_viewer_container",
            "clips_watch_and_browse",
            "reel_viewer_root",
            "reel_viewer",
            "clips_viewer"
    };

    static final String[] INSTAGRAM_CLASS_HINTS = {
            "clipsvieweractivity",
            "clips.viewer.clipsviewer",
            "clipsviewerfragment",
            "reelwatchactivity",
            "reelsviewer"
    };

    static final String[] INSTAGRAM_CHAT_CLASS_HINTS = {
            "directinbox",
            "directthread",
            "direct.inbox",
            "direct.msys",
            "inboxactivity"
    };

    static final String[] INSTAGRAM_CHAT_VIEW_IDS = {
            "direct_inbox",
            "inbox_empty",
            "thread_message",
            "direct_thread",
            "row_inbox"
    };

    // Facebook almost never exposes reel view IDs. Keep these as a fallback
    // only; primary detection is content descriptions + the Reels tab.
    static final String[] FACEBOOK_VIEW_IDS = {
            "reels_viewer",
            "reels_viewer_container",
            "video_player_reels",
            "fb_shorts",
            "shorts_viewer"
    };

    static final String[] FACEBOOK_CLASS_HINTS = {
            "reelsviewer",
            "reels.viewer",
            "reelsactivity",
            "fbshorts",
            "shortformvideo",
            "fullscreenvideoviewer"
    };

    static final String[] FACEBOOK_CONTENT_DESCS = {
            "FbShortsComposerAttachmentComponentSpec_STICKER",
            "FbShortsComposerAttachmentComponentSpec_GIF"
    };

    static final String[] FACEBOOK_CONTENT_CONTAINS = {
            "fbshorts",
            "fb_shorts"
    };

    // Tab a11y labels look like "Reels, tab 2 of 6". Bare "Reels" also appears
    // on home-feed shelves and must not trigger a block.
    static final String[] FACEBOOK_SELECTED_PREFIXES = {
            "Reels,",
            "Reels, tab"
    };

    static final String[] FACEBOOK_SAFE_CLASS_HINTS = {
            "settingsactivity",
            "accountsettings",
            "legacy_app_settings",
            "preferenceactivity"
    };

    static final String[] FACEBOOK_SAFE_VIEW_IDS = {
            "settings_list",
            "preference",
            "account_settings"
    };

    // Spotlight viewer only. A bare "spotlight" or "discover_feed" also matches
    // Camera / Stories and Back then exits Snapchat.
    static final String[] SNAPCHAT_VIEW_IDS = {
            "spotlight_container",
            "spotlight_player",
            "spotlight_pager",
            "spotlight_video"
    };

    static final String[] SNAPCHAT_CLASS_HINTS = {
            "spotlightactivity",
            "spotlight.activity",
            "discover.spotlight",
            "spotlightfeed"
    };

    static final String[] SNAPCHAT_SAFE_CLASS_HINTS = {
            "chatsactivity",
            "chat.feed",
            "chatpage",
            "cameraactivity",
            "ngs.camera",
            "camerapage"
    };

    static final String[] SNAPCHAT_SAFE_VIEW_IDS = {
            "chat_feed",
            "chat_drawer",
            "conversation_list",
            "camera_view",
            "camera_page",
            "ngs_camera"
    };

    static final String FACEBOOK_REEL_RECYCLER = "androidx.recyclerview.widget.RecyclerView";
    static final String FACEBOOK_REEL_BUTTON = "android.widget.Button";
    static final String FACEBOOK_REEL_SURFACE = "android.view.SurfaceView";
    static final float FACEBOOK_REEL_MIN_WIDTH = 0.7f;
    static final float FACEBOOK_REEL_MIN_HEIGHT = 0.5f;
    static final float VIEWER_MIN_WIDTH = 0.55f;
    static final float VIEWER_MIN_HEIGHT = 0.45f;

    private BlockRules() {}

    static Platform matchPackage(String packageName) {
        if (contains(packageName, YOUTUBE_PACKAGES)) return Platform.YOUTUBE;
        if (contains(packageName, INSTAGRAM_PACKAGES)) return Platform.INSTAGRAM;
        if (contains(packageName, TIKTOK_PACKAGES)) return Platform.TIKTOK;
        if (contains(packageName, FACEBOOK_PACKAGES)) return Platform.FACEBOOK;
        if (contains(packageName, SNAPCHAT_PACKAGES)) return Platform.SNAPCHAT;
        return null;
    }

    static String[] viewIds(Platform platform) {
        switch (platform) {
            case YOUTUBE: return YOUTUBE_VIEW_IDS;
            case INSTAGRAM: return INSTAGRAM_VIEW_IDS;
            case FACEBOOK: return FACEBOOK_VIEW_IDS;
            case SNAPCHAT: return SNAPCHAT_VIEW_IDS;
            default: return new String[0];
        }
    }

    static String[] classHints(Platform platform) {
        switch (platform) {
            case YOUTUBE: return YOUTUBE_CLASS_HINTS;
            case INSTAGRAM: return INSTAGRAM_CLASS_HINTS;
            case FACEBOOK: return FACEBOOK_CLASS_HINTS;
            case SNAPCHAT: return SNAPCHAT_CLASS_HINTS;
            default: return new String[0];
        }
    }

    static String[] safeViewIds(Platform platform) {
        switch (platform) {
            case INSTAGRAM: return INSTAGRAM_CHAT_VIEW_IDS;
            case FACEBOOK: return FACEBOOK_SAFE_VIEW_IDS;
            case SNAPCHAT: return SNAPCHAT_SAFE_VIEW_IDS;
            default: return new String[0];
        }
    }

    static String[] safeClassHints(Platform platform) {
        switch (platform) {
            case INSTAGRAM: return INSTAGRAM_CHAT_CLASS_HINTS;
            case FACEBOOK: return FACEBOOK_SAFE_CLASS_HINTS;
            case SNAPCHAT: return SNAPCHAT_SAFE_CLASS_HINTS;
            default: return new String[0];
        }
    }

    static boolean usesRedirect(Platform platform) {
        return platform == Platform.INSTAGRAM || platform == Platform.FACEBOOK;
    }

    static boolean matchContains(String value, String[] needles) {
        if (value == null || needles == null) return false;
        String lower = value.toLowerCase(java.util.Locale.US);
        for (String needle : needles) {
            if (lower.contains(needle.toLowerCase(java.util.Locale.US))) return true;
        }
        return false;
    }

    static boolean isMessengerPackage(String packageName) {
        return "com.facebook.orca".equals(packageName);
    }

    static boolean matchHint(String value, String[] hints) {
        return firstHint(value, hints) != null;
    }

    static String firstHint(String value, String[] hints) {
        if (value == null || hints == null) return null;
        String lower = value.toLowerCase(java.util.Locale.US);
        for (String hint : hints) {
            if (lower.contains(hint.toLowerCase(java.util.Locale.US))) return hint;
        }
        return null;
    }

    static boolean matchExactContentDesc(String value, String[] expected) {
        if (value == null || expected == null) return false;
        for (String item : expected) {
            if (value.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    static boolean matchSelectedPrefix(String value, boolean selected, String[] prefixes) {
        if (!selected || value == null || prefixes == null) return false;
        for (String prefix : prefixes) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) return true;
        }
        return false;
    }

    static boolean isFacebookReelClass(String className) {
        return FACEBOOK_REEL_RECYCLER.equals(className)
                || FACEBOOK_REEL_BUTTON.equals(className)
                || FACEBOOK_REEL_SURFACE.equals(className);
    }

    private static boolean contains(String value, String[] list) {
        if (value == null) return false;
        for (String item : list) {
            if (item.equals(value)) return true;
        }
        return false;
    }
}
