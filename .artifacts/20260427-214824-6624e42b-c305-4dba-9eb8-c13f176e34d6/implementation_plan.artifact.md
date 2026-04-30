# Refactoring to Single Activity Architecture with Fragments

This plan outlines the steps to convert the multi-activity navigation structure into a single-activity structure using Fragments. `MainActivity` will serve as the host, and each major feature (Home, Schedule, PDF Hub, etc.) will be a Fragment.

## Proposed Changes

### [Layouts]
- **[activity_main.xml](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/res/layout/activity_main.xml)**: Replace static content with a `FragmentContainerView`.
- **[fragment_home.xml](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/res/layout/fragment_home.xml)**: Create from the original `activity_main.xml` content.
- **[fragment_schedule.xml](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/res/layout/fragment_schedule.xml)**: Create from `activity_schedule.xml` content (removing `BottomNavigationView`).

### [Fragments]
- **[HomeFragment.kt](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/java/com/example/studentproductivityapp/features/home/HomeFragment.kt)**: Extract logic from `MainActivity.kt`.
- **[ScheduleFragment.kt](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/java/com/example/studentproductivityapp/features/home/ScheduleFragment.kt)**: Extract logic from `ScheduleActivity.kt`.

### [Activity Refactoring]
- **[MainActivity.kt](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/java/com/example/studentproductivityapp/features/home/MainActivity.kt)**: Implement Fragment navigation logic.
- **[DELETE] [ScheduleActivity.kt](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/java/com/example/studentproductivityapp/ScheduleActivity.kt)**: Logic moved to `ScheduleFragment`.
- **[DELETE] [activity_schedule.xml](file:///C:/Users/mtros/AndroidStudioProjects/StudentProductivityApp/app/src/main/res/layout/activity_schedule.xml)**: Replaced by `fragment_schedule.xml`.

## Verification Plan

### Manual Verification
1.  **Navigation Check**: Verify that clicking each item in the Bottom Navigation bar correctly swaps the Fragment in `MainActivity`.
2.  **Functionality Check**: Ensure the Home screen "Quick View" and the Schedule screen "Pending/Completed" lists still work correctly.
3.  **Lifecycle Check**: Verify that navigating away and back to a fragment preserves or correctly reloads data.
