# Morrowlight

**A little light, every day.**

A native Android incremental game about bringing a floating garden back to life. Start with a single ember, gather Glow, and grow a home for the stars.

## Screenshot

<img src="store/images/morrowlight-garden.webp" alt="Morrowlight garden screen on an Android emulator" width="420">

## Play

- Tap the garden or **Hold to gather light**. Press and hold either surface to gather continuously after a brief delay.
- Spend your first 15 Glow on a **Glowcap** to start producing light automatically. Grow six cultivators; each colony doubles its output at 10, 25, 50, and 100 plants.
- In **Cultivate**, choose one, ten, or Max per purchase. Hold a plant's button to buy repeatedly; research upgrades remain single-action.
- Every 25 touches completes a **Firefly Wish**, adding 15 seconds of current production by default.
- After the first dawn, **cultivator synergies** reward deliberate combinations: Glowcaps support Lantern lilies, Moonwillows support Firefly havens, and Cloud looms support Starwells. Cultivate previews the incremental Glow per second and estimated payback time of each purchase.
- After your first dawn, choose one of four **Constellations** for the next garden. Each strengthens a different strategy with a trade-off; confirm the choice, which lasts until the following dawn.
- **Skyworks** unlocks after the first dawn: restore the lantern bridge (5 million Glow), windmills (30 million), and moon terrace (180 million), in order. Choose to contribute 10%, 50%, or all current Glow. Investments survive dawns. The windmills permanently add 15% cultivator production; the terrace adds 20% base touch power. Completing the terrace prepares the neighboring island for a future update, but does not yet make it playable.
- Restore the dawn after earning one million Glow in a garden. Each **Dawn Seed** permanently adds 15% to production and base touch power; larger gardens earn more. Seeds, memories, lifetime statistics, and preferences survive the reset.
- Your garden keeps growing while you are away, for up to 8 hours initially and up to 24 hours with research.

## Look and feel

The garden uses a custom native Canvas scene, with four destinations: **Garden**, **Cultivate**, **Skyworks**, and **Dawn**. Reduced motion stops decorative animation, and haptics are optional.

## Build and install

Requires **JDK 21**, Android SDK platform **37**, and build tools **36.0.0**. Android 8.0 / API 26 is the minimum supported version. Set `ANDROID_HOME` or add `sdk.dir` to `local.properties`.

Build on Linux or macOS:

```sh
sh gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Build on Windows:

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Install the debug APK on either platform:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The standard Gradle Wrapper launchers use the checked-in `gradle/wrapper/gradle-wrapper.jar` to download Gradle **9.8.1** and verify its pinned distribution SHA-256. Both GitHub Actions workflows use this wrapper instead of a separately installed Gradle version. The Android workflow tests, builds, and lints the app; its `Morrowlight-debug-APK` artifact contains the debug APK. Tag pushes publish the unsigned release APK and release AAB as GitHub Releases assets; production release signing is not configured.

## Project notes

- `core`: dependency-free Kotlin game logic, saves, number formatting, and shared garden geometry.
- `app`: native Android UI, lifecycle integration, and app state.

Saves are local; Android's system backup may include them. Save format v3 reads existing v1 and v2 saves without resetting progress. Checkpoint reads and writes share one ordered queue. Consecutive pending saves coalesce to the newest snapshot without crossing a load, so reopening the Activity observes all progress queued before its load without waiting for redundant intermediate writes. The app has no network permission, ads, analytics, accounts, or in-app purchases.
