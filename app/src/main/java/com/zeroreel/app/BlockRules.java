package com.zeroreel.app;

/**
 * Short-form blocking signatures.
 * YouTube Shorts and Instagram Reels view IDs are taken from AntiScroll
 * (yadavnikhil03/AntiScroll, GPL-3.0) and extended with extra platforms.
 */
final class BlockRules {

    enum Platform {
        YOUTUBE(Prefs.APP_YOUTUBE, true, false),
        INSTAGRAM(Prefs.APP_INSTAGRAM, true, false),
        TIKTOK(Prefs.APP_TIKTOK, true, true),
        FACEBOOK(Prefs.APP_FACEBOOK, true, false),
        SNAPCHAT(Prefs.APP_SNAPCHAT, true, false);

        final String prefKey;
        final boolean defaultEnabled;
        final boolean blockEntireApp;

        Platform(String prefKey, boolean defaultEnabled, boolean blockEntireApp) {
            this.prefKey = prefKey;
            this.defaultEnabled = defaultEnabled;
            this.blockEntireApp = blockEntireApp;
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

    // AntiScroll Instagram Reels IDs + extra reel surfaces
    static final String[] INSTAGRAM_VIEW_IDS = {
            "clips_viewer_view_pager",
            "reel_viewer_title",
            "reel_viewer",
            "clips_video_container",
            "clips_viewer",
            "reel_viewer_root",
            "clips_viewer_video_layout"
    };

    static final String[] INSTAGRAM_CLASS_HINTS = {
            "clipsviewer",
            "reelviewer",
            "clips.viewer",
            "reel.viewer"
    };

    static final String[] FACEBOOK_VIEW_IDS = {
            "reels_viewer",
            "reels_viewer_container",
            "watch_feed",
            "video_player_reels",
            "immersive_video"
    };

    static final String[] FACEBOOK_CLASS_HINTS = {
            "reelsviewer",
            "reels.viewer",
            "immersivevideo",
            "fullscreenvideoviewer"
    };

    static final String[] SNAPCHAT_VIEW_IDS = {
            "spotlight",
            "discover_feed",
            "spotlight_view"
    };

    static final String[] SNAPCHAT_CLASS_HINTS = {
            "spotlight",
            "discoverfeed",
            "discover.feed"
    };

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

    private static boolean contains(String value, String[] list) {
        if (value == null) return false;
        for (String item : list) {
            if (item.equals(value)) return true;
        }
        return false;
    }
}
