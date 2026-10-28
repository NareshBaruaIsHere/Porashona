# Play Store Launch & Student Companion Upgrade Plan

This plan prepares the East West University (EWU) Student Companion app for Google Play Store publication while adding four high-value student utilities: offline JSON backup & restore, course attendance tracking, one-tap Google Calendar exam export, and an upgraded Home Screen widget displaying both upcoming exams and the next class.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and technical decisions reflect your confirmed choices and keep the app fast, minimal, and fully offline-capable for Play Store release.

- **Confirmed Scope**:
  1. **Play Store Release Readiness & Policy Compliance**: Built-in Privacy Policy sheet + external policy link, full local data wipe/reset control, release R8/ProGuard rules for serialization and OCR, and accessibility verification.
  2. **Local Backup & Restore (Offline JSON)**: Export and import all routines, exams, study tasks, syllabus topics, and attendance logs via the Android system file picker.
  3. **Course Attendance Tracker in Class Routine**: Track attended vs. missed classes per course with live percentage badges and an 80% attendance threshold indicator.
  4. **Export Exams to Google Calendar**: One-tap export from Exam cards and Exam Detail views into the device's calendar app without requiring sensitive calendar permissions.
  5. **Enhanced Home Screen Widget**: Display both the nearest exam countdown and today's next scheduled class on the Android home screen with instant refresh.
- **Confirmed Backup Strategy**: Offline local JSON file export and import using the system Storage Access Framework (zero storage permissions required).
- **Confirmed Privacy & Data Controls**: Minimal built-in Privacy Policy bottom sheet paired with an external browser link and a one-tap "Reset All Local Data" safeguard in Settings.

---

## 1. Overview & Core Concept

- **What It Does**: Elevates the app from a personal utility into a Play Store–ready academic suite where students can safeguard their semester data across devices via JSON backups, track course attendance directly inside their weekly routine, push exam schedules to Google Calendar, and glance at both their next class and next exam from their phone's home screen.
- **Target Audience / Persona**: University students (specifically East West University students following the Saturday–Thursday academic week) who want a clean, distraction-free, offline-first academic planner.
- **Key Value**: Eliminates the risk of losing semester schedules when switching phones, prevents falling below university attendance requirements, and satisfies all Google Play Data Safety, permission, and release-signing requirements.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Course Attendance Tracking (Routine Screen)**:
   - Each class card on the Routine screen displays a compact attendance pill (e.g., `92% • 11/12`).
   - Tapping compact `+ Present` or `+ Absent` quick-action chips on the class card increments the attended or total class count immediately with haptic feedback, or tapping the badge opens a minimal adjustment popover to edit exact counts.
   - If attendance drops below 80%, the badge shifts from emerald/primary to a warm amber/rose alert color and shows how many consecutive classes are needed to recover to 80%.
2. **One-Tap Google Calendar Exam Export (Dashboard & Exam Detail)**:
   - Exam cards and the Exam Detail screen include a clean calendar action icon (`Add to Calendar`).
   - Tapping it launches the native Google Calendar event creation sheet pre-populated with the course code, title, exam date, start/end time, room number, and seat details.
3. **Offline JSON Backup & Restore (Settings Screen)**:
   - Under a dedicated **Data & Backup** section in Settings, students can tap **Export Backup (.json)** to save a timestamped backup file anywhere on their device or Google Drive.
   - Tapping **Restore Backup** opens the system file picker, validates the JSON structure, and restores courses, routines, exams, syllabus items, and focus logs with a confirmation snackbar.
4. **Privacy Policy & Data Reset (Settings Screen)**:
   - A **Privacy & Play Compliance** section offers a clean **Privacy Policy** item that opens a minimal, scannable bottom sheet summarizing local data storage, optional AI timetable scanning, and camera/photo picker usage, plus a button to open the web policy URL.
   - Includes a **Clear All App Data** action with a confirmation dialog so users have complete control over their stored records.
5. **Dual-Section Home Screen Widget**:
   - The resized/updated widget displays two clean sections: **Next Class Today** (Course code, time slot, and room number) and **Upcoming Exams** (top countdowns with days/hours remaining), plus a header refresh button.

### Visual Identity & Theme

- **Aesthetic Direction**: Minimalist, high-contrast academic workspace consistent with the simplified UI across Routine, Focus, and Stats.
- **Color Palette & Mood**:
  - Uses Material 3 semantic tokens (`MaterialTheme.colorScheme.surface`, `surfaceContainerLow`, `primary`, `tertiary`, `error`).
  - Status-driven semantic accents: Emerald/Teal for safe attendance (`>= 80%`), Amber for borderline (`75%–79%`), and Rose/Error for critical (`< 75%`).
- **Typography & Hierarchy**:
  - Clean tabular numerals for attendance percentages, countdowns, and widget timestamps.
  - Concise single-line labels with minimal helper copy to preserve a calm, uncluttered layout.
- **Interactive Feedback & Motion**:
  - Smooth animated progress bars on attendance indicators, subtle ripple feedback on all 48dp touch targets, and clear toast/snackbar confirmations after backup, restore, or calendar export.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Intent-Based Calendar Export vs. Direct Calendar Provider Permission**
  - *Chosen Approach*: Use Android's `Intent.ACTION_INSERT` with `CalendarContract.Events` extras.
  - *Why*: Requires **zero runtime permissions** (`READ_CALENDAR` / `WRITE_CALENDAR` are sensitive permissions that trigger stricter Google Play review). It also lets the student confirm or tweak reminder notifications directly in Google Calendar before saving.
  - *Alternatives Considered*: Background Calendar Provider insertion (rejected due to Play Store permission friction).
- **Decision 2: System Storage Access Framework (SAF) for JSON Backup**
  - *Chosen Approach*: Use `ActivityResultContracts.CreateDocument("application/json")` and `ActivityResultContracts.OpenDocument()`.
  - *Why*: Complies 100% with Google Play's Scoped Storage policy without requesting any storage permissions, and works seamlessly with local folders, SD cards, and Google Drive.
  - *Alternatives Considered*: Cloud account sync (rejected to keep the app fast, private, and login-free).
- **Decision 3: Embedded Attendance Fields on Class Routine Items**
  - *Chosen Approach*: Store `attendedClasses` and `totalClasses` directly within the Routine course model (grouped by course code across days) with a database migration.
  - *Why*: Keeps routine cards fast to render and allows attendance updates for a course (e.g., `CSE101`) to stay synchronized even if the course meets on multiple days of the week.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                              UI & Widget Layer                            │
│                                                                           │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │
│  │    Routine Screen    │  │  Dashboard / Detail  │  │ Settings Screen │  │
│  │  • Day Swipe Pager   │  │  • Exam Countdowns   │  │  • JSON Backup  │  │
│  │  • Attendance Pills  │  │  • Calendar Intent   │  │  • Privacy Sheet│  │
│  │  • +Present / +Absent│  │    Exporter          │  │  • Data Reset   │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └────────┬────────┘  │
│             │                         │                       │           │
│  ┌──────────┴─────────────────────────┴───────────────────────┴────────┐  │
│  │                    Home Screen AppWidgetProvider                    │  │
│  │           • Next Scheduled Class Today  • Upcoming Exams            │  │
│  └──────────────────────────────────┬──────────────────────────────────┘  │
└─────────────────────────────────────┼─────────────────────────────────────┘
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        ViewModel & Domain Services                        │
│                                                                           │
│  ┌──────────────────────────┐            ┌─────────────────────────────┐  │
│  │     ExamViewModel        │            │   BackupRestoreManager      │  │
│  │  • Attendance Mutations  │◄──────────►│  • kotlinx.serialization    │  │
│  │  • Calendar Intent Build │            │  • Atomic DB Import/Export  │  │
│  │  • Full Data Reset       │            │  • Schema Version Check     │  │
│  └─────────────┬────────────┘            └──────────────┬──────────────┘  │
└────────────────┼────────────────────────────────────────┼─────────────────┘
                 ▼                                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         Local Persistence Layer                           │
│                                                                           │
│  ┌────────────────────────────────────┐  ┌─────────────────────────────┐  │
│  │           Room Database            │  │    Preferences DataStore    │  │
│  │  • Exams, Tasks, Syllabus, Logs    │  │  • Theme, Focus Targets,    │  │
│  │  • ClassRoutines (+Attendance)     │  │    Notification & App Prefs │  │
│  └────────────────────────────────────┘  └─────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **Class Routine Attendance Extension**:
  - Extend the routine entity with `attendedCount: Int = 0`, `totalClassesCount: Int = 0`, and `targetAttendancePercent: Int = 80`.
  - Synchronize attendance counts across all weekly slots sharing the same normalized `courseCode` so marking attendance on Saturday automatically updates Monday's slot for the same course.
- **Backup Payload Schema (`AppBackupPayload`)**:
  - A `@Serializable` envelope containing `schemaVersion`, `exportedAtMillis`, `exams`, `syllabusTopics`, `studyTasks`, `studySessions`, `classRoutines`, and key user preferences.
- **Release & ProGuard Hardening**:
  - Keep rules for `@Serializable` backup and Gemini REST models, Room entities, and ML Kit text recognition classes so release builds (`isMinifyEnabled = true`) run without reflection crashes.

### Interactive Component & State Mapping

- **Attendance Controls (`RoutineScreen`)**:
  - `+ Present` increments both `attendedCount` and `totalClassesCount` for matching course codes; `+ Absent` increments only `totalClassesCount`.
  - Long-pressing or tapping the attendance pill opens a compact adjustment dialog to manually set or reset `attendedCount` and `totalClassesCount`.
- **Calendar Export (`CalendarExportHelper`)**:
  - Converts the exam's stored date and start time into epoch milliseconds, sets a default 2-hour duration, formats the location (`Room • Seat`), and launches `Intent(Intent.ACTION_INSERT)` safely with fallback error handling if no calendar app is installed.
- **Backup & Restore (`SettingsScreen`)**:
  - Export serializes the current database snapshot on `Dispatchers.IO` and writes to the user-selected `Uri`.
  - Import parses and validates the JSON file, replaces/merges local tables inside a Room transaction, refreshes the Home Screen widget, and displays a completion message.
- **Widget Update Pipeline (`ExamTrackWidgetProvider`)**:
  - Queries both upcoming exams and today's remaining classes (ordered by start time for the current day of the week) and renders the next class alongside the top exam countdowns.
