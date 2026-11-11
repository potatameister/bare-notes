<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="112" alt="BARE Notes icon">

<h1>BARE Notes</h1>

<p><strong>A quiet place for text.</strong></p>

<p>Markdown notes that stay on your phone. No accounts, no sync, no tracking.</p>

<a href="https://github.com/potatameister/bare-notes/actions/workflows/android.yml">
  <img src="https://img.shields.io/github/actions/workflow/status/potatameister/bare-notes/android.yml?style=for-the-badge&logo=github&label=build" alt="Build status">
</a>
<img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+">
<img src="https://img.shields.io/badge/Permissions-none-2ea44f?style=for-the-badge" alt="No permissions">
<img src="https://img.shields.io/badge/Offline-100%25-111111?style=for-the-badge" alt="100% offline">
<img src="https://img.shields.io/badge/License-GPL--3.0-2ea44f?style=for-the-badge" alt="GPL-3.0">

</div>

---

## At a glance

| | |
|---|---|
| **Notes** | Markdown files stored locally |
| **Folders** | One level, with note counts |
| **Editor** | Reader mode plus WYSIWYG editing |
| **Search** | Title and body |
| **Themes** | System, light, dark |
| **Export** | Markdown ZIP |
| **Permissions** | None |
| **Android** | 8.0+ (minSdk 26) |
| **Size** | about 1.4 MB |
| **Status** | v0.1 · early build |
| **License** | GPL-3.0 |

---

## Why BARE Notes

Most notes apps start with an account and a server. BARE Notes starts with a text field.

Your notes are ordinary Markdown files in the app’s private storage. The app has no internet permission, no analytics, no ads, and nothing to sign into. It opens instantly and gets out of the way.

It works on a plane, on a train, or anywhere else with no signal.

---

## What you can do

| Area | Details |
|---|---|
| **Write** | Bold, italic, strike, H1/H2, bullets, numbered lists, checklists, quotes |
| **Read** | Notes open in reader mode; tap the title or body to edit |
| **Organize** | One-level folders, search, sort by created or modified |
| **Make it yours** | System/light/dark theme, editor text size |
| **Keep it safe** | Autosave, undo/redo, delete with Undo |
| **Move it** | Markdown `.zip` export and import |

---

## Install

GitHub Releases are not published yet. Current signed builds are available from
GitHub Actions:

1. Open the **Actions** tab.
2. Open the latest **Android** run.
3. Download the **`BARE-Notes-Release-APK`** artifact.
4. Install **`BARE-Notes-release.apk`** on your phone.

> Actions artifacts expire. A permanent GitHub Release will be added when the
> first release is published.

---

## Privacy

- No `INTERNET` permission
- No accounts
- No ads
- No analytics or crash reporting
- No cloud backup
- No background services

Everything is local. Export a Markdown backup before uninstalling; uninstalling
removes all notes and folders.

---

## Data

Notes are stored as Markdown files in the app’s private files directory.
Folder metadata is stored separately as JSON.

Use **Settings → Export Markdown** to write a `.zip` somewhere you choose.
**Import Markdown** replaces the current data.

---

## Build

Built entirely in GitHub Actions with Gradle. No local Android SDK is required.

- Kotlin + Jetpack Compose
- Android Gradle Plugin 9.4.1
- Gradle 9.6.0
- Compose BOM 2026.09.00
- compileSdk / targetSdk 37
- minSdk 26
- Signed R8 release APK

Store metadata and the 512px icon live in
[`fastlane/metadata/android/en-US`](fastlane/metadata/android/en-US).

---

## License

BARE Notes is free software under the **GNU General Public License v3.0**.

See [LICENSE](LICENSE) for the full text.

<div align="center">

<strong>Made with care. Entirely offline.</strong>

</div>
