# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the project
./gradlew assembleDebug

# Run unit tests (JVM, does not require a device)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.mybill.ExampleUnitTest"

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean
```

Gradle wrapper is at the project root. Do not use a system Gradle installation.

## Architecture

Single-module Android app (`com.example.mybill`), generated from the Android Studio template.

- **Language**: Java 11
- **UI**: XML layouts with Material 3 theme (`Theme.Material3.DayNight.NoActionBar`), ConstraintLayout
- **Activity**: single `MainActivity` using `EdgeToEdge` with system bar insets handling
- **Theme**: `Theme.MyBill` → `Base.Theme.MyBill` → `Theme.Material3.DayNight.NoActionBar`

## Dependencies

Versions are managed via Gradle version catalog at `gradle/libs.versions.toml`. Key libraries:
- AndroidX AppCompat, Activity (with `EdgeToEdge`), ConstraintLayout
- Google Material Components
- JUnit 4 (unit tests), AndroidX Test + Espresso (instrumentation tests)
