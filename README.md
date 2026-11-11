<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="BARE Notes icon">

<h1>BARE Notes</h1>

<p>A small, fast, offline notes app for Android.</p>

<img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+">
<img src="https://img.shields.io/badge/Permissions-none-2ea44f?style=for-the-badge" alt="No permissions">
<img src="https://img.shields.io/badge/License-GPL--3.0-2ea44f?style=for-the-badge" alt="GPL-3.0">

</div>

---

## Features

- Markdown notes stored on your phone
- One-level folders, search, and sorting
- Reader mode and rich-text editing
- Bold, italic, strike, headings, lists, checklists, and quotes
- Light, dark, and system themes
- Markdown ZIP export and import
- Autosave, undo/redo, and delete with Undo

---

## Install

GitHub Releases are not published yet. For now, download the signed APK from
the latest GitHub Actions run:

1. Open the **Actions** tab.
2. Open the latest **Android** run.
3. Download the **`BARE-Notes-Release-APK`** artifact.
4. Install **`BARE-Notes-release.apk`** on your phone.

> Actions artifacts expire. A permanent download link will be added when the
> first GitHub Release is published.

---

## Privacy

- No `INTERNET` permission
- No accounts
- No ads or tracking
- No cloud sync
- No background services

Your notes are stored locally in the app’s private storage. Use
**Settings → Export Markdown** to save a backup before uninstalling; uninstalling
removes all notes and folders.

---

## Data

Notes are stored as Markdown `.md` files. Folder metadata is stored as JSON.

Export creates a `.zip` you can save anywhere. Import replaces the current
notes and folders.

---

## Android support

| | |
|---|---|
| **Android** | 8.0+ (minSdk 26) |
| **Size** | about 1.4 MB |
| **Status** | v0.1 |
| **License** | GPL-3.0 |

---

## Development

- Kotlin + Jetpack Compose
- Android Gradle Plugin 9.4.1
- Gradle 9.6.0
- Compose BOM 2026.09.00
- compileSdk / targetSdk 37

Builds are produced by GitHub Actions. Store metadata and the 512px icon are
in [`fastlane/metadata/android/en-US`](fastlane/metadata/android/en-US).

---

## License

BARE Notes is free software under the **GNU General Public License v3.0**.

See [LICENSE](LICENSE) for the full text.
