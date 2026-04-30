# Walkthrough: Fragment Refactoring

I have refactored the application to use a single-activity architecture with Fragments. This significantly improves code maintainability and user experience.

## Key Changes

### 1. Single Host Activity
`MainActivity` now serves as the central hub for the entire application. It contains the `BottomNavigationView` and a `FragmentContainerView` where different features are swapped in and out.

### 2. Feature Fragments
- **HomeFragment**: Contains the dashboard with the daily greeting, quick assignment view, and recent scans.
- **ScheduleFragment**: Contains the full assignment tracker with pending and completed lists, swipe-to-delete, and the "Add Assignment" button.

### 3. Smoother Navigation
By using Fragments, switching between "Home" and "Schedule" is now instantaneous and doesn't require reloading the entire activity. This preserves the "frame" of the app (the navigation bar) while updating the content.

### 4. Reduced Code Duplication
The logic for the Bottom Navigation bar is now defined only once in `MainActivity.kt`, instead of being repeated in every activity.

## How to use:
- Simply use the Bottom Navigation bar as before. You'll notice that switching between the first two tabs is smoother.
- Other features (Map, PDF, Lectures) still function as separate activities but are launched from the same central `MainActivity`.
