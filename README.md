# BARE Notes

A tiny, fast, offline notes app for Android.

- No accounts
- No ads
- No tracking
- No internet permission
- No cloud backup; export manually
- Notes stored locally as JSON
- One-level folders with note counts and quick move
- Autosave with saved/unsaved status
- Search
- System, light, and dark themes
- Editor text size, sort by modified/created
- Bold, italic, underline, strike, headings, bullets, numbers, checklists, quotes
- JSON backup export and import

## Status

v0.1 — early build.

## Install

Download the signed release APK from the latest GitHub Actions run:

1. Open the **Actions** tab.
2. Open the latest **Android** run.
3. Download the `bare-notes-release` artifact.
4. Install `app-release.apk` on your phone.

## Build

The APK is built in GitHub Actions with Gradle. No local Android SDK is required.

- Android Gradle Plugin 9.4.1
- Gradle 9.6.0
- Built-in Kotlin
- Compose BOM 2026.09.00
- compileSdk / targetSdk 37
- minSdk 26

## Data

Notes and folders are stored in the app's private files directory. Use
**Settings → Export backup** to write a JSON backup somewhere you choose.
**Import backup** replaces the current data.

Uninstalling the app removes all notes and folders unless you exported a backup
first.

## License

GPL-3.0
