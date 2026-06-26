# Clearer Status

## Current App State

- repository folder exists
- product intent is documented
- tech stack is documented
- password setup and unlock flow is implemented
- password hash and salt are stored in encrypted local preferences
- unlocked screen accepts a target amount in GB
- random filler files are written to app-owned external storage
- progress and current file are shown during generation
- active generation can be canceled and partial files are kept
- writes stop at the 1 GB free-space safety buffer
- filler generation currently runs only while the app stays in the foreground
- latest debug APK: `apk/Clearer-debug.apk`

## Product Decision

The app is a free-space filler, not a guaranteed secure erase tool.

It should honestly describe what it does:

- writes random data until the requested target or safety limit is reached
- leaves generated files on disk for the user to delete manually
- may reduce simple recovery opportunities from logical free space
- requires a local password before use

It should also clearly state what it cannot guarantee:

- exact physical overwrite of previously deleted photos
- deletion from cloud backups, gallery trash, or messaging apps
- forensic-grade secure wipe on flash storage

## Next Implementation Steps

- move generation into a foreground service for background-safe runs
- show clearer low-space and partial-write summaries
- test large runs on device and verify manual cleanup flow
