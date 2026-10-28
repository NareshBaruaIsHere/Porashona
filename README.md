# Porashona

**A calmer way to stay ready for exams.** Porashona brings exam countdowns, syllabus progress, study sessions, and your weekly class routine together in one Android app.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

## Get the app

[**Download Porashona for Android**](https://tinyurl.com/mvhbfe5f)

## What you can do

- **Plan exams:** Add subjects, exam dates and times, rooms, and syllabus topics. See live countdowns and completion progress at a glance.
- **Stay organized:** Search, filter, and sort exams by urgency, date, readiness, or name.
- **Build a study habit:** Track study sessions, review weekly activity and subject totals, and keep an eye on your streak.
- **Focus with Pomodoro:** Run focus and break sessions and save completed focus time to your study history.
- **Keep your class routine handy:** Set a weekly schedule with rooms and professors, and see what is happening now or next.
- **Use helpful reminders:** Schedule exam notifications and add the home-screen widget for a quick view of upcoming exams.
- **Work with your timetable:** Scan timetable images with text recognition and export your schedule or backup data.
- **Make it yours:** Choose a light, dark, or system theme.

## Privacy by design

Porashona is designed to work without an account. Exam, syllabus, routine, and study-tracking data are stored locally on your device. The app includes optional network-backed features; review the [privacy policy](https://itsnaresh.me/porashona-privacy/) for details.

## Built with

- Kotlin and Jetpack Compose with Material 3
- AndroidX Navigation and lifecycle components
- Room for structured local data and DataStore for preferences
- Kotlin Coroutines, Retrofit, and OkHttp
- Google ML Kit text recognition for timetable scanning

## Run locally

### Requirements

- Android Studio with the Android SDK for API 36
- JDK 11 or newer (the Gradle build uses Java 11 source and target compatibility)
- An Android device or emulator running Android 7.0 (API 24) or newer

1. Clone this repository and open the project folder in Android Studio.
2. Let Android Studio sync the Gradle project and install any requested SDK components.
3. Select the `app` run configuration and launch it on a device or emulator.

The app can be run locally without a Gemini key. If you configure optional Gemini-powered image recognition, put your own key in the root `.env` file as `GEMINI_API_KEY=...`. The Secrets Gradle Plugin reads `.env`; never commit API keys, signing keys, or passwords.

## Build

On Windows, from the project root:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. A release build requires your own signing configuration; keep signing credentials outside source control.

## Project layout

```text
app/src/main/java/com/example/
├── data/          # Room database, DAOs, models, and repository
├── notification/  # Exam reminder notifications
├── receiver/      # Notification and device-boot receivers
├── ui/            # Compose screens, navigation, and view models
├── util/          # OCR, date, calendar, and backup helpers
└── widget/        # Home-screen exam widget
```

## Contributing

Bug reports and focused pull requests are welcome. Please describe the change and include clear steps to reproduce any reported issue.

## License

No license is currently specified. All rights are reserved unless the project owner grants permission. Add a license file before redistributing or reusing this project.

---

Made by [Naresh Barua](https://github.com/NareshBaruaIsHere).
