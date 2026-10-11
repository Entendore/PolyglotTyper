# Polyglot Typer

A multilingual typing game for Android. Kotlin 2.0 · Jetpack Compose · Material 3 ·
Hilt · Room · DataStore · WorkManager · type-safe Navigation.

## Features

- **7 languages** with real diacritics: English, Spanish, French, German, Italian,
  Portuguese, Turkish
- **9 keyboard layouts** in 3 groups (QWERTY/AZERTY/QWERTZ · Dvorak/Colemak/BÉPO/
  Neo/Turkish F · ABC) with an **Automatic** mode per language, plus an on-screen
  practice keyboard with next-key highlight — learn Dvorak or AZERTY without
  installing a system keyboard
- **6 game modes**: 30s/60s timed, 10/25 word count, **Sudden Death** (one wrong
  word ends it), **Zen** (endless), seeded **Daily Challenge**, and the **Word Rain**
  arcade game (falling words, 3 lives, level ramp)
- **Gamification**: XP with accuracy weighting, quadratic level curve, daily-streak
  tracking with honest decay + at-risk banner, 14 auto-evaluated achievements
- **Stats**: WPM sparkline (last 30 games), per-language and per-layout bests,
  recent history, achievement grid
- **Reliability**: process-death game restoration (SavedStateHandle +
  kotlinx.serialization), split-clock pause (background time never counts),
  junk-result guard, midnight-safe Daily, Room v1→v2 migration with schema export

## Architecture

Single activity, MVVM + unidirectional data flow (StateFlow down, events up).
Domain layer is pure Kotlin; data layer binds Room/DataStore behind interfaces;
a single `RecordGameResult` use case is the finish pipeline for every mode.

## Build

Open in Android Studio (creates the Gradle wrapper), sync, run on API 26+.
Unit tests: `./gradlew testDebugUnitTest`.
