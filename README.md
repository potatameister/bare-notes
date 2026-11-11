# BARE Notes

A tiny, fast, offline notes app for Android.

- No accounts
- No ads
- No tracking
- No internet permission
- No cloud backup; export manually
- Notes stored locally as Markdown (`.md`)
- One-level folders with note counts and quick move
- Autosave with saved/unsaved status
- Delete a note with Undo
- Search
- System, light, and dark themes
- Editor text size, sort by modified/created
- Bold, italic, strike, headings, bullets, numbers, checklists, quotes
- Markdown backup export and import (`.zip`)

## Status

v0.1 — early build.

## Install

Download the signed release APK from the latest GitHub Actions run:

1. Open the **Actions** tab.
2. Open the latest **Android** run.
3. Download the `BARE-Notes-Release-APK` artifact.
4. Install `BARE-Notes-release.apk` on your phone.

## Release

Only the signed release APK is published. Store metadata lives in
`fastlane/metadata/android/en-US`; update the matching `changelogs/<versionCode>.txt`
when cutting a release. No tag or GitHub Release is created automatically.

## Build

The APK is built in GitHub Actions with Gradle. No local Android SDK is required.

- Android Gradle Plugin 9.4.1
- Gradle 9.6.0
- Built-in Kotlin
- Compose BOM 2026.09.00
- compileSdk / targetSdk 37
- minSdk 26

## Data

Notes are stored in the app's private files directory as Markdown `.md`
files. Folder metadata is stored as JSON. Use **Settings → Export Markdown**
to write a `.zip` of the Markdown files somewhere you choose.
**Import Markdown** replaces the current data.

Uninstalling the app removes all notes and folders unless you exported a backup
first.

## License

GPL-3.0
