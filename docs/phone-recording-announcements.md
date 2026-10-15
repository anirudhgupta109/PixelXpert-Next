# Phone 241 recording announcements

The existing announcement-removal toggle now also handles Fermat's downloaded audio and pre-recording countdown. It selects the native skip-prompts branch immediately before each prompt, because session startup resets the playback field. Capture and saving are unchanged. The new hooks apply only to ordinary call recording; legacy hooks remain in place.

There is no phone-model restriction. Tested on Pixel 10a / Android 17; other models using the same Phone build are unverified and must also meet PixelXpert and Google Phone recording requirements.

Supported: Google Phone 241.0.991235506-pixel (versionCode 20129568). Other versions leave this adapter inactive and show a compatibility warning when enabled. Method signatures are checked before installation; initialization failure rolls back the hooks.

Tested inside a release build of PixelXpert on Pixel 10a / Android 17, with the separate compatibility helper disabled: the call recorded automatically without countdown or announcements, appeared in Phone history, and the saved recording decoded with audio in both channels. Earlier helper tests also covered manual stop/restart. The standalone PR and combined build both compile successfully. Change the setting while idle, since the existing toggle restarts Phone.
