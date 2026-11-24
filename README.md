# Little Grove

A clean-room Android/Kotlin preschool learning app with original UI and local lesson content. It uses platform Android views and has no third-party runtime dependencies.

## Build

Requirements: JDK 17 or newer and Android SDK 37.

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Activities and data

- Letters, numbers, tracing, and matching activities use large, child-friendly touch targets.
- Lesson data is stored in `app/src/main/assets/content/lessons.json`.
- Progress is stored locally with Android `SharedPreferences`.
- Voice prompts use Android `TextToSpeech`; offline speech depends on the device's installed voice engine and language pack.
- The app has no account or network flow.

The project does not contain assets, code, or trademarks from any reference application.
