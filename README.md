# Zero Reel

Android self-control app that blocks short-form video (Reels, Shorts, TikTok, Facebook Reels, Spotlight). After you arm it once, blocking starts again when the phone reboots. Changing settings, pausing, or removing uninstall protection requires a **6-digit authenticator code** (Google Authenticator, Authy, or any TOTP app).

AntiScroll is kept only as the source of YouTube Shorts and Instagram Reels view-ID rules.

## What this is

A **visible** app on your phone. You install it, add a TOTP secret to an authenticator app, turn on Accessibility and Device Admin, then arm it.

Once armed:

- A foreground service and boot receiver start protection after restart
- Regular device admin only adds an extra Settings tap (uninstall is still easy)
- **Strict uninstall lock** (Android Device Owner) disables the Uninstall button until you disarm with an authenticator code
- Authenticator code is required inside Zero Reel to pause, change the block list, or disarm

## What this is not

Zero Reel is **not** a hidden or undeletable program. Android does not allow an app to hide itself from Settings or survive every removal path.

Factory reset still wipes the phone. That is intentional so you cannot get permanently locked out.

## What gets blocked

From AntiScroll (GPL-3.0):

- YouTube Shorts view IDs such as `shorts_container`, `reel_player_page`, `reel_recycler`
- Instagram Reels view IDs such as `clips_viewer_view_pager`

Added in Zero Reel:

- Extra YouTube / Instagram IDs and class-name fallbacks
- TikTok (whole app, because the feed is short-form)
- Facebook and Messenger Reels (content descriptions, selected Reels tab, not the main feed)
- Snapchat Spotlight only (`spotlight_container`; Camera, Chat, and Stories stay open)
- Instagram / Snapchat / Facebook send you to chat or messages instead of closing the app
- Daily allowance (off / 5 / 15 / 30 minutes) before hard block
- Blocks-today counter
- 5-minute pause (authenticator required)
- Debug log in app storage

Normal YouTube videos, Instagram posts, Facebook feed, and Snapchat chat are not the target. TikTok is treated as all short-form.

## Setup

Zero Reel will not arm until it is Device Owner.

1. Install the APK and finish authenticator + Accessibility + device admin.
2. Remove Google accounts from the phone (temporary).
3. On the Device Owner screen, tap **Copy the one command** and paste it once:

```
adb shell dpm set-device-owner com.zeroreel.app/.ZeroReelAdminReceiver && adb shell pm grant com.zeroreel.app android.permission.WRITE_SECURE_SETTINGS && adb shell dumpsys deviceidle whitelist +com.zeroreel.app
```

4. The app notices Device Owner by itself. Tap **Sign back into Google**, then **Open Zero Reel**.

That locks uninstall, force-stop, battery limits, Safe Mode, extra users, and USB debugging. Accessibility is turned back on if you switch it off.

To uninstall later: **Disarm and allow uninstall** with a valid authenticator code, then uninstall from Settings. Factory reset still wipes the phone.

## License

GPL-3.0. YouTube Shorts and Instagram Reels signatures come from [yadavnikhil03/AntiScroll](https://github.com/yadavnikhil03/AntiScroll).
