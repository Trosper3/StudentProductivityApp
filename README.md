# StudentProductivityApp
Midterm Presentation Guide: Student Productivity App

## 1. Project Overview & Core Functionality
The Student Productivity App is a centralized hub designed to help students manage their academic life.

### Current Status: The app is stable, follows a modular architecture, and features a functional navigation system.

### Working Features:
Assignment Tracker: Full CRUD operations for student tasks using a local database.
Canvas API Sync: Access Token based retrieval of "To-Do" items from Idaho State University’s Canvas instance (allowed with FERPA according to ISU IT department recommendation)
Interactive Campus Map: Integrated WebView with interactive ISU map capabilities.
PDF Scanner (OCR): Ability to scan documents and extract text to automatically create assignments.
Video Lecture Tool: Transcript search and synchronized video playback.

## 2. Code Structure & Architecture
The project follows a Feature-Based Package Structure, separating concerns and ensuring scalability.

### Activities & Fragments:
#### MainActivity: Uses a BottomNavigationView to swap between HomeFragment and ScheduleFragment.
CampusMapActivity, VideoLectureActivity, PdfHubActivity: Dedicated activities for specialized, full-screen features.

#### Navigation: Uses Intents for activity transitions and FragmentTransactions within the MainActivity for a seamless hub experience.

#### Separation of Concerns: Logic is separated into:
features/: Organized by functionality (assignments, campus_map, pdf_scanner, video_lectures).
database/: Room persistence layer.
viewmodel/: State management and business logic.

## 3. Data Handling & Background Work
### Local Storage (Room DB):
Uses Room to store assignments (Assignment.kt).
Uses Flow and LiveData to ensure the UI updates automatically when data changes (Reactive UI).
Local Storage (SharedPreferences): Used for lightweight user state, such as the login status ("Jerry").

### Remote API (Retrofit):
Connected to Canvas LMS.
Uses Coroutines (lifecycleScope.launch) to perform network calls and database operations on background threads, preventing UI "jank" or freezing.

### OCR & AI: Uses Google ML Kit for text recognition and document scanning.

## 4. UI / UX Implementation
### RecyclerView: Used in HomeFragment and ScheduleFragment to display lists efficiently. It utilizes ListAdapter and DiffUtil for optimized UI updates.
### ConstraintLayout: Used across all XML files to ensure a flat view hierarchy and responsive design across different screen sizes.
### Custom Styling: Implemented a dark theme using ISU's brand colors (Orange/Black/Gray).
### System UI Awareness: Layouts include specific margins/padding to avoid overlapping with the status bar or camera cutouts.

## 5. Technical Stack (Libraries Used)
### Jetpack Room: Local SQLite abstraction.
### Retrofit & Gson: For API communication and JSON parsing.
### ML Kit: For Document Scanning and Text Recognition.
### CameraX: For a custom, high-performance camera interface.
### Material Design 3: For modern UI components (Buttons, DatePickers, Nav Bars).