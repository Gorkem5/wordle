# Wordle — JavaFX (IBM 80s White Theme)

A simple Wordle-like clone built with JavaFX. The target 5-letter word changes every hour (UTC).

## Features
- 5-letter word, 6 guesses
- Colors: green (correct spot), yellow (in word), gray (not in word)
- Word changes every hour deterministically (UTC) — no server needed
- Retro white IBM-inspired visual style

## Run
Using the Gradle wrapper:

```powershell
# From the repository root
./gradlew.bat :app:run
```

If you see JavaFX errors, ensure JDK 21 is used. The wrapper reports the JVM in use when it starts.

## Notes
- The wordlist is embedded in `WordleApp.java` as `WordRepository`.
- The word resets at the top of the hour automatically and clears the board.
