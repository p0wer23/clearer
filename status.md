# Clearer Status

## Current App State

- repository folder exists
- product intent is documented
- tech stack is documented
- password setup and unlock flow is implemented
- password hash and salt are stored in encrypted local preferences
- protected placeholder screen is shown after successful unlock
- latest debug APK: `apk/Clearer-debug.apk`
- storage filler controls are not implemented yet

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

- build Compose screen for target GB input and start/cancel actions
- implement the filler writer with progress updates
- show generated file location for manual deletion
- add safety checks for available storage and minimum free-space buffer
- build and test debug APK on device
