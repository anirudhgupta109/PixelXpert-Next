### This repository is [forked](https://github.com/siavash79/PixelXpert) ###
The original project was shutdown and this repository was created to continue the work for future Android builds, you can find older builds in the original repository.


### For Pixel Stock Android 17 and newer:  
[![Latest Release](https://img.shields.io/github/v/release/anirudhgupta109/PixelXpert-Next?color=green&include_prereleases&label=Download%20Latest%20Stable)](https://github.com/anirudhgupta109/PixelXpert-Next/releases/latest)
[![Latest Canary Release](https://img.shields.io/badge/Download%20Latest-Canary-blue)](https://github.com/anirudhgupta109/PixelXpert-Next/releases/tag/canary_builds)

![Downloads - Stable channel](https://img.shields.io/github/downloads/anirudhgupta109/PixelXpert-Next/total?color=red&label=Downloads%20-%20Stable%20Channel)

### For Older Pixel Stock builds:
Refer to [The original project](https://github.com/siavash79/PixelXpert#for-pixel-stock-android-12-and-13-up-to-nov-2022---aosp-13r8)



[![Telegram URL](https://img.shields.io/badge/Telegram-Join-2CA5E?style=social&logo=telegram)](https://t.me/PixelXpert_Github)

<p align="center">
  <img src="PixelXpert.svg" alt="PixelXpert Logo" width="200" height="200" />
</p>

This is a mixed Xposed+Magisk/KSU module, which is made to allow customizations that are not originally designed in AOSP (Android Open Source Project). Please read thorough below before reaching to download links
<hr>

### **Features:**
Currently, PixelXpert-Next offers customizations on different aspects of system framework and SystemUI, including:
- Status bar
- Quick Settings panel
- Lock screen
- Notifications
- Gesture Navigations
- Phone & Dialer
- Hotspot
- Package Manager
- Screen properties
<hr>

### **Compatibility:**
PixelXpert-Next is ONLY compatible with pixel stock firmware on Google Pixel devices. Any custom ROM (including PE, PE plus, pixel plus ui and etc) or stock ROM outside stock pixel firmware on Google pixel devices (e.g. OneUI on Samsung, MIUI on Xiaomi and etc) is not supported and may not be fully (or even at all) compatible.

Here is the compatibility chart according to different android versions and QPRs:

- Android 17 newer: [latest stable version](https://github.com/anirudhgupta109/PixelXpert-Next/releases/latest)
- Android 16 (June 2022) and older Android Versions: [Refer to original project](https://github.com/siavash79/PixelXpert#compatibility).
<hr>

### **Prerequisites:**
- Compatible ROM (see Compatibility text above)
- Device Rooted with Magisk/KSU
- If you're using KSU, you need to flash a metamodule like [Mountify](https://github.com/backslashxx/mountify/releases/latest) or [NoMount](https://github.com/maxsteeel/nomount/releases/latest)
- LSPosed (Zygisk Version preferred) (For Android 14+ use [LSPosed fork (Vector) by JingMatrix](https://github.com/JingMatrix/Vector/releases)) or the [closed source LSPosed by the original team](https://lsposed.zip)
<hr>

### **How to install:**
- Download the module according to your firmware as mentioned above
- If you're using KSU, Flash a metamodule in KSU that has overlay support like [Mountify](https://github.com/backslashxx/mountify/releases/latest) or [NoMount](https://github.com/maxsteeel/nomount/releases/latest) [SKIP IF MAGISK!!!!]
PS. You might need to configure your metamodule (in the case of Mountify, set `MOUNT_DEVICE_NAME` to `KSU` and ensure `mountify_mounts` is `2` and `mountify_custom_umount` is `0`)
- Reboot
- Flash PixelXpert-Next in Magisk/KSU
- Reboot
- Grant root for PixelXpert-Next in KSU (doesn’t automatically request) or Magisk (if it’s not already)
- Enable PixelXpert-Next in LSPosed if it isn’t already (ensure all the scopes are checked)
- Preferably reboot
- Open PixelXpert-Next app and apply changes

<hr>

### **Release Variants:**  
The module is also released in 2 flavors with different manual download and update procedures. But both can utilize automated updates through magisk manager, or through in-app updater (for canary, updates will not count against the module's download count).

<ins>Stable release:</ins> 
- Manual Install/Update: through repository's Github release page (link below) AND through in-app updater

<ins>Canary release:</ins>
- Manual Install/Update: through repository's Actions page and [telegram channel](https://t.me/PixelXpert_Github) (latest version is available from [here](https://github.com/anirudhgupta109/PixelXpert-Next/releases/tag/canary_builds) also)

*No matter which flavor you're on, you can always switch to the other one with in-app updater
<hr>

### **Translations:**  
[![Crowdin](https://badges.crowdin.net/aospmods/localized.svg)](https://crowdin.com/project/aospmods)  
Want to help translate PixelXpert to your language? Visit [Crowdin](https://crowdin.com/project/aospmods)
<hr>

### **Donations:**
This project is open source and free for usage, build or copy. However, if you really feel like it, you can donate to your favorite charity on our behalf, or help funding education for children in need, at [Child Foundation](https://mycf.childfoundation.org/s/donate)
<hr>

### **Credits / Thanks:**
- Android Team
- @topjohnwu for Magisk
- @rovo89 for Xposed
- Team LSPosed
- apsun@github for remote-preferences
- @nijel8 for double-tap to wake


**UI design:**  
- @Mahmud0808  

**Graphic design:**  
- JstormZx@Telegram (Icon and Banner) 
- RKBDI@Telegram  (Icon)

**Brought to you by:**
@siavash79, @ElTifo & @anirudhgupta109
<hr>
