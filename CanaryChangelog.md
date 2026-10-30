**canary-540**  
- QSBrightnessSlider: add reorderable layout for QS brightness, tiles, and media player  
- Allow media player on top + QQS slider with shade transition fixes  
  
**canary-539**  
- allow customizing the QS footer build number  
  
**canary-538**  
- add cursor control keys while the keyboard is open  
- fix SystemUI hot reload null dereferences  
- remove KSU scopes and associated hook code  
  
**canary-537**  
- Make tap on battery icon on QS/QQS redirect to battery usage page  
- migrate Xposed module to libxposed API 102  
  
**canary-536**  
- data: Only restart unhooked processes instead of always SysUI  
- Add a keyboard space toggle to gesture navigation  
- Some misc. fixes for QS clock/date tap
  
**canary-535**  
- Make the QS clock/date taps work on the Compose shade header  
- screens: Hook the battery bar page to statusbar instead of leaving just the toggle  
  
**canary-534**  
- xposed: Add an adaptive brightness toggle to the brightness slider  
- replace hand-rolled helpers with platform APIs  
- ui: derive the Google-design glyph from its accent  
  
**canary-533**  
- ui: add a Google settings design toggle  
- ui: home: group the tiles into categories  
- leave the device-credential pad unshuffled  
- ui: PxnNav: solid destinations and predictive-back transitions  
- capture the edge handler from createDisplayBackGestureHandler  
  
**canary-532**  
- Only customize the clock on Compose clock builds + keep companions ticking  
- investigator: Add the preference store to the markdown report  
  
**canary-531**  
- Give the flanking clock text the clock's font weight  
- Wrap Compose clock in a container with flanking text views + fix compose clock check  
  
**canary-530**  
- Restore color customization and color pickers  
  
**canary-529**  
- Add toggle to block sensitive QS tiles and power menu  
- Explicitly target SystemUI for profile switch broadcast  
- Fix flashlight fade effect with strength-leveled torch  
- Remove flashlight-leveled tile and associated code  
- Fix QQS rows on builds with inlined repository constructor  
  
**canary-528**  
- update: Fix crash due to shorter changelog when switching branch + fix paddings  
  
**canary-527**  
- audit: Modify file name to include android build version  
- Audit status bar clock subsystem, Compose ClockKt and notification icon binder hooks  
- Dual-dispatch status bar icons to CommandQueue for Compose status bar  
- Add Hook Audit & Diagnostics tool with markdown export  
- Never rewrite ACTION_OUTSIDE in one-finger pull-up hook  
  
**canary-526**  
- Install new app and drop legacy  
- Bringup JetpackCompose PX-N app  
  
**canary-525**  
- Keep QS brightness slider full width below tiles and media in landscape  
  
**canary-524**  
- integrate bottom QS brightness slider + brightness slider in QQS  
  
**canary-523**  
- prevent duplicated carrier text hooks  
  
**canary-522**  
- Integrate native High Brightness Flashlight with gesture support  
  
**canary-521**  
- Fix caffeine tile missing field crash  
  
**canary-520**  
- Add caffeine QS tile  
- Update module structure to install as a user app  
  
**canary-519**  
- securely disable Auto-Confirm PIN during FBE (Direct Boot)  
  
**canary-518**  
- replace thread logic with native LockPatternUtils bypass  
  
**canary-517**  
- fix Shuffle PIN for Compose Keyguard  
- Disable root grant on !magisk installs + abort install incase no pkg uid found  
  
**canary-516**  
- fix Notification Default Expansion on CP3A/CP41  
- fix Quick QS panel pulldown gesture on CP3A/CP41  
  
**canary-515**  
- fix hiding status bar icons  
- resolve Network Traffic mid-right visibility issue  
  
**canary-514**  
- Adapt setUserExpanded signature to prevent SysUI crash  
  
**canary-513**  
- handle signature change of calculateChargingSpeed in Android 17 QPR  
- correct ReflectionConsumer type for multi-method hooks  
  
**canary-512**  
- prevent handler freeze and massive speed spikes  
  
**canary-511**  
- enable custom text variables (temperature, persian date, network stats) for Android 17 Compose clock via SimpleDateFormat override  
  
**canary-510**  
- force leading zero on 24-hour clock for Android 17 QPR2 (0:xx -> 00:xx)  
- Stable release  
  
**canary-509**  
- OwnPrefs: Fix GitHub URL to point to fork  
- strictly gate Jetpack clock detection to prevent false positives on non-Jetpack clock  
- cache clock ComposeView to prevent accidentally moving the battery ComposeView on Android 17  
  
**canary-508**  
- restrict ComposeView LayoutParams modifications and disposal hook to Jetpack clock only  
- prevent ComposeView disposal when moving clock to the right side on Android 17  
- github: Don't send zip on TG, keep on GH releases  
- resolve ConcurrentModificationException and ClassNotFoundException in xposed hooks  
  
**canary-507**  
- PixelXpert-Next: Update app icon SVG and vector drawables + refine install steps  
- issue_template: Mandate android build number for bug reports  
  
**canary-506**  
- pin wireless ADB to a static port via internal TCP forwarder  
- physically move ComposeView on Android 17 so clock repositioning works  
- add option to move clock to second row when multi-row notifications are enabled  
  
**canary-505**  
- use GO_TO_SLEEP_REASON_POWER_BUTTON to force immediate lock on double tap to sleep  
- Add A17 build id to pixelbuild filters  
  
**canary-504**  
- Do not reposition 'Ongoing Chip' to prevent clock from disappearing when notifications are cleared  
- Use system clock seconds setting from SysUI Tuner if show seconds is enabled  
- implement Android 17 ClockInteractor hook to support custom clock formats  
- disable markdown formatting for Telegram posts to prevent parsing errors  
- service.sh: Restart SysUI after boot complete to allow lsposed hook into it  
- Uprev to canary version and only post in TG  
  
**canary-503**  
- prevent system crash due to NullPointerException when updating preferences  
  
**canary-502**  
- {MagiskModBase,app} Add support for newer android beta build numbers  
  
**canary-501**  
- Allow magiskd to interact with priv-apps targetting API 36  
  
**canary-500**  
- PixelXpert-Next: Introduction of fork and GH Actions rectifications  
- Resolve Android 17QPR1 background camera restrictions + power button detection  
- xposed: Rectify scope list for Android system  
- RecordingMessage: Suppress call-{recording,notes} TTS  
- Fix lockscreen double tap to sleep gesture support  
