# Clearer Intent

Password-protected local Android app for filling available storage with gibberish data.

The user enters an amount of data to generate, measured in GB, and the app writes random non-sparse files until that target is reached or a safe free-space limit is hit. The user deletes the generated files manually outside the app.

## Purpose

- reduce recoverable deleted-file remnants by consuming logical free space
- provide a simple user-controlled free-space filler
- require a password before the filler controls can be used
- keep all behavior local to the device
- avoid accounts, sync, ads, analytics, or network access

## V1

- user enters target size in GB
- app validates the requested size against available storage
- app generates random gibberish files in an app-owned storage location
- app shows progress while writing
- app allows canceling an active fill operation
- app does not delete generated filler files
- app shows where generated files are stored so the user can delete them manually
- app keeps a safety buffer so the phone does not reach zero free space
- app is password protected
- first launch requires setting a password
- later launches require the password before showing the main controls
- app explains that free-space filling is not a guaranteed forensic secure erase on flash storage

## Out Of Scope

- app-driven deletion of generated filler files
- raw block-device overwrite
- root-only storage wiping
- forensic secure erase guarantees
- cloud photo deletion
- gallery trash deletion
- factory reset automation
- background storage filling without visible user consent
