# Zero Reel

Android self-control app that blocks short-form video (Reels, Shorts, TikTok, Facebook Reels, Spotlight). After you arm it once, blocking starts again when the phone reboots. Changing settings, pausing, or removing uninstall protection requires a **6-digit authenticator code** (Google Authenticator, Authy, or any TOTP app).

AntiScroll is kept only as the source of YouTube Shorts and Instagram Reels view-ID rules.

## What this is

A **visible** app on your phone. You install it, add a TOTP secret to an authenticator app, turn on Accessibility and Device Admin, then arm it.

Once armed:

- A foreground service and boot receiver start protection after restart
- Device admin makes Android ask you to deactivate the app before uninstall
- Authenticator code is required inside Zero Reel to pause, change the block list, or disarm

## What this is not

Zero Reel is **not** a hidden or undeletable program. Android does not allow an app to hide itself from Settings or survive every removal path.

These still work, by design:

- Factory reset
- `adb uninstall`
- Turning off Accessibility in system settings
- Deactivating device admin from Android Settings (the app warns you first)

Those limits are intentional. A secretly undeletable app would be malware.

## What gets blocked

From AntiScroll (GPL-3.0):

- YouTube Shorts view IDs such as `shorts_container`, `reel_player_page`, `reel_recycler`
- Instagram Reels view IDs such as `clips_viewer_view_pager`, `reel_viewer`

Added in Zero Reel:

- Extra YouTube / Instagram IDs and class-name fallbacks
- TikTok (whole app, because the feed is short-form)
- Facebook and Messenger Reels
- Snapchat Spotlight
- Daily allowance (off / 5 / 15 / 30 minutes) before hard block
- Blocks-today counter
- 5-minute pause (authenticator required)
- Debug log in app storage

Normal YouTube videos, Instagram posts, Facebook feed, and Snapchat chat are not the target. TikTok is treated as all short-form.

## Setup

1. Open the project in Android Studio and install it on a phone (Android 8+).
2. Add the shown secret to an authenticator app and confirm a live code.
3. Enable **Zero Reel** in Accessibility settings.
4. Enable **uninstall protection** (device admin). This does not wipe data or hide the app.
5. Optionally ignore battery optimizations so OEMs do not kill the service.
6. Tap **Arm Zero Reel**.

To uninstall later: **Disarm and allow uninstall** with a valid authenticator code, then uninstall from Settings.

## License

GPL-3.0. YouTube Shorts and Instagram Reels signatures come from [yadavnikhil03/AntiScroll](https://github.com/yadavnikhil03/AntiScroll).
