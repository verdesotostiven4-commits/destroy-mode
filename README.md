# Destroy Mode

Android launcher/optimizer made specifically for **Xiaomi 14T Pro + Arena Breakout Lite**.

## What it does

When the user taps **ACTIVAR Y JUGAR**, the app uses Shizuku (ADB shell identity) to apply only Android Game Manager interventions:

```sh
cmd game set --mode 2 --downscale 0.8 --fps 120 com.proximabeta.mf.liteuamo
cmd game mode 2 com.proximabeta.mf.liteuamo
am force-stop com.proximabeta.mf.liteuamo
```

Then it launches Arena Breakout Lite normally.

The **RESTAURAR ARENA** button removes the override and returns Arena to Android's normal game profile.

## Safety / scope

- No root required.
- Requires Shizuku to be installed, started, and authorized.
- Does not modify Arena Breakout's APK, assets, game files, memory, network traffic, or anti-cheat.
- Does not disable thermal protection.
- Game Turbo settings such as texture filtering / LOD remain configured separately in Xiaomi's own UI.

## Recommended Xiaomi/Arena settings

- Arena Breakout Lite: 120 FPS, minimum/performance graphics, 480p.
- Game Turbo: anisotropic filtering 1X, texture filtering "Alta velocidad", mipmap LOD +2.0, multithreaded rendering ON.
- Xiaomi Game DND can be used for automatic notification blocking while Arena is active.

## Build

Open the project in Android Studio and build `app`.

Or push the repository to GitHub; the included Actions workflow builds a debug APK automatically.
