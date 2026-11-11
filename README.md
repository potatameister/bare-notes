<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="BARE Notes icon">

<h1>BARE Notes</h1>

<p>A small, fast, offline notes app for Android.</p>

<img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+">
<img src="https://img.shields.io/badge/Permissions-none-2ea44f?style=for-the-badge" alt="No permissions">
<img src="https://img.shields.io/badge/Size-1.4_MB-007EC6?style=for-the-badge" alt="1.4 MB">
<img src="https://img.shields.io/badge/License-GPL--3.0-6f42c1?style=for-the-badge" alt="GPL-3.0">

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

## Download

<a href="https://github.com/potatameister/bare-notes/releases/latest">
  <img src="https://img.shields.io/badge/Download-Latest_APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download latest APK">
</a>

Download the latest signed APK from
[GitHub Releases](https://github.com/potatameister/bare-notes/releases/latest).

Requires Android 8.0 or newer.

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

## Support BARE Notes

BARE Notes is free, ad-free, and built in spare time. If it saves you a little
work, you can help keep it going:

<a href="https://github.com/sponsors/potatameister">
  <img src="https://img.shields.io/badge/GitHub_Sponsors-Support-EA4AAA?style=for-the-badge&logo=github&logoColor=white" alt="GitHub Sponsors">
</a>
<a href="https://buymeacoffee.com/potatameister">
  <img src="https://img.shields.io/badge/Buy_Me_a_Coffee-Support-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee">
</a>

<p>
  <a href="https://potatameister.github.io/support">
    <img src="https://img.shields.io/badge/More_ways_to_support-Donate-E11D48?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="More ways to support">
  </a>
</p>

Every contribution helps keep BARE Notes free and offline. You can also help by
reporting bugs, suggesting features, or sharing the app.

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
