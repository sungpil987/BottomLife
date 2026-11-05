# BottomLife 2.0 REBORN

Godot 4 mobile idle RPG prototype. Separate from the existing Java Android app.

## Important save compatibility
The existing Android APK stores data in Android SharedPreferences `save_v1`, while Godot uses `user://bottomlife_reborn.json`. The Godot APK deliberately has a **different application ID** (`com.bottomlife.reborn`) to avoid overwriting or deleting the original app's local saves. This version **does not automatically migrate the original save**. A secure export/import migration bridge is required before replacing the existing APK.

## Build
The `Godot Reborn APK` workflow installs Godot headless, export templates and Android tooling and exports a debug APK. Keep the original Java app installed until migration is implemented and tested.

## Content
Street tapping, business income, mine digging, drill, relics, prestige, soul upgrades, offline earnings and procedural pixel scenes.
