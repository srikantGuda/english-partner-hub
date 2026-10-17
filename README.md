# English Partner Learning Hub

A personal, offline Android app for organising and studying the English Partner course classes you receive on WhatsApp — Day 1 to Day 60 and as many extra days as you need.

Built with Kotlin, Jetpack Compose, Material 3, Room, MVVM, AndroidX Media3 (ExoPlayer), the Storage Access Framework and DataStore.

No login. No internet permission. No ads, analytics or paid APIs. Your course files stay on your phone.

## Features

| Area | What it does |
|---|---|
| Home | Overall progress, **Continue learning** (last opened lesson with its position), next class to finish, recently opened |
| Library | Day 1–60 created on first launch, **Add day** for Day 61+, search, filters (To do / Done / Has files) |
| Import | **Add files** opens Android's file picker (audio, video, PDF, image, text, multiple at once). Or in WhatsApp: long-press a file → Share → *English Partner*. Shared text messages are saved as articles. The day number is guessed from the file name (e.g. `Day 12.mp3`) |
| Day | Many materials per day, rename, move to another day, mark done, delete; per-day **Notes** and **Words** tabs |
| Audio | Play/pause, ±10 s, seek bar, speed 0.5×–2×, remembers position, keeps playing with screen off while the page is open, auto-marks done at the end |
| Video | Media3 player, fullscreen landscape mode, same resume and bookmarks |
| PDF | Smooth page list, remembers last page, tap a page to pinch-zoom, page bookmarks |
| Articles | Adjustable font size, text selection, paragraph bookmarks, resume position, "Save word" while reading |
| Images | Pinch-zoom viewer |
| Vocabulary | Word, meaning, example, linked day, mark learned (★), search |
| Backup | One `.zip` with all data and files, saved anywhere through the file picker; restore replaces current data |

## Project layout

```
app/src/main/java/com/englishpartner/hub/
  data/db/          Room entities, DAOs, database
  data/storage/     Copies imported files into private app storage
  data/backup/      JSON codec + zip backup/restore
  data/settings/    DataStore preferences (speed, font size, theme)
  data/LearningRepository.kt
  ui/               Compose screens + ViewModels (home, library, day, viewer, vocab, settings, share)
.github/workflows/android-apk.yml   Cloud APK build
```

Imported files are copied to the app's private folder (`filesDir/media`). They are not committed to Git, not uploaded to GitHub Actions, and `.gitignore` blocks common media, PDF, zip and keystore files as an extra safety net.

## Building without a computer (GitHub Actions)

Every push to `main`, every pull request, and every manual run builds the app in the cloud:

1. Installs JDK 17 and the Android SDK (platform 35, build-tools 35.0.0), accepting licences.
2. Runs unit tests: `./gradlew testDebugUnitTest`.
3. Builds the APK: `./gradlew assembleDebug`.
4. Fails if the APK is missing, otherwise uploads **EnglishPartnerLearningHub-debug-apk** (kept 14 days).

### Watch a build on your phone

1. Open the repository on github.com in your mobile browser.
2. Tap **Actions** (if hidden, use the browser menu → *Desktop site*, or open `https://github.com/<you>/<repo>/actions`).
3. Tap the newest **Android APK** run. Yellow = running, green ✓ = success, red ✗ = failed.
4. To build again without changing code: Actions → **Android APK** → **Run workflow** → choose `main` → **Run workflow**.

### Download and install the APK

1. Open the green ✓ run → scroll to **Artifacts** → tap **EnglishPartnerLearningHub-debug-apk**. You must be signed in to GitHub.
2. A `.zip` downloads. Open your phone's **Files** app → **Downloads** → tap the zip → **Extract** → you get `EnglishPartnerLearningHub-debug.apk`.
3. Tap the APK. If Android asks, allow **Install unknown apps** for Files (or your browser), then go back and tap **Install**.
4. If Play Protect warns about an unknown developer, tap **More details → Install anyway** (this is your own debug build).

### Keep updates installable (important)

Android only installs an update over the old app if both are signed with the same key. The workflow caches the debug key between runs, so:

- **Install APKs from builds of the `main` branch** (after merging). A pull-request build may use a different key.
- The cache is kept while you build at least once every 7 days. If it expires, the next APK has a new key and Android says *"App not installed"*. Then: **Settings → Create backup** in the old app, uninstall, install the new APK, **Restore from backup**.
- For a permanent key, add a repository secret named `DEBUG_KEYSTORE_BASE64` containing a base64-encoded keystore (alias `androiddebugkey`, passwords `android`). The workflow uses it automatically.

## Using the app

- **WhatsApp → app:** long-press the audio/video/PDF in the chat → Share icon → *Add to English Partner* → check the day number → **Save to Day N**.
- **Files → app:** open a day → **Add files** → pick one or many files.
- **Backups:** Settings → **Create backup** regularly, and keep a copy off the phone (pen drive, laptop). Restore needs the same zip.

## Tests

Unit tests in `app/src/test` cover file-type detection, formatting helpers, day-number guessing, article paragraph splitting and backup JSON round-trips. They run on every CI build.

## Known limitations (v1)

- Audio continues with the screen off only while the player page stays open; there is no lock-screen media notification yet (no background `MediaSessionService`).
- Files are copied into the app, so storage is used twice until you delete the WhatsApp originals.
- Backups of large video courses produce large zip files; keep enough free space while creating one.
- `.doc/.docx`, `.ppt` and other office files are stored but cannot be previewed.
- Debug build only; no Play Store release signing.
