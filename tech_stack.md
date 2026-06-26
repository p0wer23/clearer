# Clearer Tech Stack

Clearer should use the same native Android direction as the sibling apps.

## Stack

- Kotlin
- Gradle Kotlin DSL
- Gradle Wrapper
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- `ViewModel` + `StateFlow`
- coroutines
- foreground service or WorkManager for long-running generation
- AndroidX Security Crypto or platform Keystore-backed password hash storage

## Storage Approach

- write filler files to app-owned external storage, such as `context.getExternalFilesDir(...)`
- generate real bytes, not sparse files
- use chunked streaming writes so memory usage stays stable
- use random or pseudorandom bytes to avoid filesystem/storage compression benefits
- split output into multiple files instead of one huge file
- leave generated filler files on disk for the user to delete manually
- show the generated file location in the UI after completion
- keep a configurable minimum free-space buffer before stopping

## Password Protection

- require password setup on first launch
- require password entry on later launches before showing filler controls
- store only a salted password hash, not the raw password
- keep password and salt in encrypted local preferences or Keystore-backed storage
- add change-password support in a later iteration
- keep all authentication local to the device

## Android Constraints

- normal apps cannot wipe raw flash blocks
- normal apps cannot reliably overwrite deleted files because flash storage uses wear leveling
- Android scoped storage limits broad filesystem access
- `MANAGE_EXTERNAL_STORAGE` should be avoided for v1 unless there is a strong reason
- the app must not claim guaranteed secure deletion

## Suggested Package

- `com.example.clearer`

## Suggested Project Shape

`app/`
- Gradle project root

`app/app/`
- Android app module

`app/app/src/main/java/com/example/clearer/`
- `MainActivity.kt`
- `ClearerApp.kt`
- `data/`
- `storage/`
- `ui/`
- `ui/theme/`

## Implementation Notes

- represent the requested amount internally as bytes using `Long`
- validate user input before starting generation
- expose progress as bytes written, target bytes, current file, and status
- stop safely on low storage, cancellation, write failure, or app shutdown
- use a foreground notification if the operation can continue while the app is backgrounded
- keep partial filler files after cancellation so the user can decide what to delete
