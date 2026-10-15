# Automatically record all contacts

The optional Phone tweak includes current and future saved contacts without copying the address book or changing Google's selected-number list. It defaults to off. Enable automatic recording for selected numbers in Google Phone as well; unknown numbers follow Google's separate setting. Other recording eligibility and retention settings remain controlled by Phone. This feature does not remove announcements.

There is no phone-model restriction. Tested inside a release build of PixelXpert on Pixel 10a / Android 17 with the separate helper disabled: a saved contact outside the selected-number list recorded automatically, appeared in Phone history, and saved playable audio in both channels. Other models are unverified and must meet PixelXpert and Google Phone recording requirements. The standalone PR and combined release builds pass, along with three scope-isolation tests.

Supported: Google Phone 241.0.991235506-pixel (versionCode 20129568). Version and method checks prevent installing these mappings on other releases. Unsupported versions show a compatibility warning and retain Google's selected contacts.

The shared collection helper is overridden only during the eligibility invocation. A nesting-aware thread-local scope is cleared in finally, including failure and suspension. The caller is deoptimized to prevent inlining. Runtime synthetic checks and JVM tests cover scope isolation and cleanup. Initialization failure rolls back installed hooks.

Change the setting while idle because it restarts Phone. Disabling restores selected-number behavior without editing the list.
