# Phone 241 recording announcements

The existing announcement-removal toggle now also handles Fermat's downloaded audio and pre-recording countdown. It selects the native skip-prompts branch immediately before each prompt, because session startup resets the playback field. Capture and saving are unchanged. The new hooks apply only to ordinary call recording; legacy hooks remain in place.

There is no phone-model restriction. Tested on Pixel 10a / Android 17; other models using the same Phone build are unverified and must also meet PixelXpert and Google Phone recording requirements.

Supported: Google Phone 241.0.991235506-pixel (versionCode 20129568). Other versions leave this adapter inactive and show a compatibility warning when enabled. Method signatures are checked before installation; initialization failure rolls back the hooks.

Equivalent hooks were tested through a helper module on a Pixel 10a running Android 17: countdown and announcements were absent, including manual stop/restart, and the saved recording contained audio in both channels. The integrated PixelXpert APK builds but has not been installed for a separate device test. Change the setting while idle, since the existing toggle restarts Phone.
