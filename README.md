# JoyOverlay

Floating joystick overlay with a Material 3 Expressive look (spring motion, shape morphing).
It drives Android's mock-location provider, so you must pick the app under
Developer options > Select mock location app.

## Customize
Everything visual lives in `customize/`: colors (light + dark), icons, app name,
and a Material You toggle. See `customize/README.txt`.

## Build on GitHub
1. Create a repo, upload everything in this folder (keep `.github/`).
2. Push to `main`. Actions > "Build APK" runs (or press "Run workflow").
3. Download the `JoyOverlay-debug-apk` artifact from the finished run, unzip, install.
