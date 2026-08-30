# AntiScroll

An open-source Android accessibility service app designed to help users reduce screen time by blocking short-form video feeds across popular platforms like YouTube Shorts and Instagram Reels.

## Features
- **YouTube Shorts Blocking:** Analyzes UI structures combined with Window State events to back you out of endless Shorts.
- **Instagram Reels Blocking:** Targets explicit Reel viewer View IDs to prevent doom-scrolling. 
- **Privacy-First:** Operates entirely locally on your device via Android Accessibility Services. No data is collected or transmitted.
- **Anime Wisdom:** Displays motivational Anime quotes to inspire discipline.
- **Beautiful UI:** Polished Material Design 3 UI with dark mode, subtle animations, and zero user-tracking ads.

## How it works
This app utilizes the `AccessibilityService` API to listen for specific Window State Change events. Once it detects the resource IDs or class names specific to the short-form content in target apps, it immediately triggers the `GLOBAL_ACTION_BACK` feature (a simulated "Back" button press) to pop the user back into their main feed or home screen.

## Setup
To install AntiScroll on your phone during development:
1. Open this project in Android Studio.
2. Build -> Make Project
3. Run the app on a connected physical device or emulator.
4. Enable the **Accessibility Permission** when prompted inside the app. 

*Note: For Android 13+ devices, sideloaded applications enforce restricted settings. You must open Settings -> Apps -> AntiScroll -> 3-dots Menu -> "Allow restricted settings" before Android will allow you to enable the Accessibility Service.*

## License
AntiScroll is licensed under the [GPL-3.0 License](LICENSE).
In short: You can freely use, modify, and distribute this software, but any derivative works or distributed versions must also be fully open-source.

---
Made with ❤️ by @yadavnikhil03

Source: imported from [yadavnikhil03/AntiScroll](https://github.com/yadavnikhil03/AntiScroll) (`AntiScroll-main`).
