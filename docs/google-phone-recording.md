# Google Phone recording compatibility

Two independent settings are available under Phone tweaks:

- **Remove call recording announcement** also skips the pre-recording countdown and downloaded start/stop announcements used by the supported Fermat implementation. The existing legacy resource/TTS hooks are retained.
- **Automatically record all contacts** includes current and future contacts in Google's automatic-recording eligibility decision. It defaults to off. Enable automatic recording for selected numbers in Google Phone as well. To include unknown numbers, enable Google's separate non-contact setting.

The new adapter supports **Google Phone 241.0.991235506-pixel, versionCode 20129568**. It checks both the version and reflected signatures. An unsupported version or adapter initialization failure leaves the new hooks inactive and displays a compatibility warning when either setting is enabled. Updating Phone requires verifying the mappings again.

The all-contacts option does not enumerate, copy or modify the address book or Google's selected-number list. It only overrides nonempty string membership checks during the automatic-recording eligibility method. The recording master switch, selected-contact switch, non-contact switch, geographic/device eligibility and retention remain controlled by Google Phone. Enabling all contacts does not enable announcement removal.

## Implementation

Fermat uses downloaded audio rather than the old string/TTS prompts. Its session has an existing skip-prompts branch. The adapter selects that branch immediately before countdown, starting-announcement and ending-announcement methods, for ordinary call-recording sessions only. Changing the field in the constructor alone is ineffective because the start path resets it. Capture, saving and call history are not replaced. This adapter does not alter Call Notes or Call Screen.

The collection membership helper is shared by other code, so the override is confined to a thread-local, nesting-aware scope around the eligibility method. The scope is cleared on normal return, failure and coroutine suspension. Resumed invocations establish their own scope. The eligibility caller is deoptimized to avoid ART inlining the lookup. Runtime self-checks use synthetic values, and initialization errors roll back the hooks.

## Validation and limits

The equivalent version-pinned hooks were exercised on Android 17 with the supported Phone build:

- Automatic recording started for a saved contact outside Google's selected-number list and saved in call history.
- An answered test call had no countdown or audible announcement. Capture started within approximately 80 ms of the connected-call event; zero latency is not promised.
- Manual recording stop and restart also skipped the prompts. The saved recording decoded successfully with audio in both channels.

These are manual observations of the equivalent hooks, not a device test of the complete integrated PixelXpert APK. Unit tests cover scope isolation, nesting, invalid candidates, exceptions and coroutine-like thread changes. No contacts, phone numbers, recordings, call identifiers or private logs are included in this contribution.

Toggling recording settings currently restarts Phone, matching the existing announcement setting. Change them while no call is active. Disabling all contacts restores Google's original selected-number behavior without changing that list.
