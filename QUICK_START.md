# Village Legends 2D — Quick Start Guide

## ⚡ 5-Minute Setup (Works Without Art Assets)

The game includes a **Placeholder Mode** — coloured rectangles replace
sprite art and silent stubs replace audio. You can build, run, and
test all core gameplay without any art files.

---

## Step 1 — Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| JDK | 11+ | https://adoptium.net |
| Android Studio | Hedgehog+ | https://developer.android.com/studio |
| Android SDK | API 34 | via Android Studio SDK Manager |
| Android device | Android 8+ | or emulator API 26+ |

---

## Step 2 — Clone / Extract

```bash
# Extract the project zip
unzip VillageLegends2D.zip
cd VillageLegends2D
chmod +x gradlew        # Linux/Mac only
```

---

## Step 3 — Configure local.properties

```bash
cp local.properties.template local.properties
```

Edit `local.properties` and set your SDK path:

```properties
# Linux
sdk.dir=/home/YOUR_USERNAME/Android/Sdk

# macOS
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# Windows
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

---

## Step 4 — Connect Device / Start Emulator

```bash
# Check device is detected
adb devices
# Should show: List of devices attached
#              XXXXXXXX    device
```

Or start an AVD from Android Studio → Device Manager → Play button.

---

## Step 5 — Build & Install (Debug)

```bash
./gradlew android:installDebug
```

First build downloads ~200 MB of Gradle/LibGDX dependencies.
Subsequent builds take 15–30 seconds.

**What you'll see without art assets:**
- Splash screen with white text on dark background ✓
- Main menu with buttons (coloured rectangles) ✓  
- Game world: coloured tiles (green=grass, gray=buildings, blue=water) ✓
- Player: yellow rectangle that moves with virtual joystick ✓
- NPCs: coloured circles with AI pathfinding ✓
- All game systems (farming, quests, economy) fully functional ✓

---

## Step 6 — Add Real Art (Optional)

Place assets in these folders:

```
android/assets/
├── textures/
│   ├── characters.atlas   ← Player + NPC sprite sheets
│   ├── characters.png
│   ├── vehicles.atlas
│   ├── vehicles.png
│   ├── effects.atlas
│   ├── effects.png
│   ├── items.atlas        ← Item icons for inventory
│   ├── items.png
│   ├── ui.atlas           ← HUD elements, buttons
│   └── ui.png
├── audio/
│   ├── music/
│   │   ├── menu_theme.ogg
│   │   ├── village_day.ogg
│   │   ├── forest_ambience.ogg
│   │   ├── lake_calm.ogg
│   │   ├── town_market.ogg
│   │   ├── highway_chase.ogg
│   │   ├── combat_tension.ogg
│   │   ├── festival_sankranti.ogg
│   │   ├── festival_ugadi.ogg
│   │   ├── festival_dasara.ogg
│   │   └── festival_deepavali.ogg
│   └── sfx/
│       ├── attack_swing.ogg
│       ├── hit_flesh.ogg
│       ├── knockout.ogg
│       ├── player_hurt.ogg
│       ├── coin_jingle.ogg
│       ├── harvest.ogg
│       ├── plant_seed.ogg
│       ├── water_crops.ogg
│       ├── engine_start.ogg
│       ├── engine_off.ogg
│       ├── ui_click.ogg
│       ├── ui_open.ogg
│       └── quest_complete.ogg
└── maps/
    ├── main_village.tmx   ← Tiled map files
    ├── farmlands.tmx
    ├── forest.tmx
    ├── lake.tmx
    ├── town.tmx
    └── highway.tmx
```

**Free starter assets:**
- Sprites: https://kenney.nl/assets (Top-down tiles, characters)
- Audio: https://freesound.org (CC0 sounds)
- Music: https://opengameart.org (royalty-free music)

---

## Step 7 — Release Build

```bash
# Generate signing keystore (one-time)
keytool -genkey -v -keystore vl2d.keystore \
        -alias vl2d -keyalg RSA -keysize 2048 -validity 10000

# Add to local.properties:
RELEASE_STORE_FILE=../vl2d.keystore
RELEASE_STORE_PASSWORD=yourpassword
RELEASE_KEY_ALIAS=vl2d
RELEASE_KEY_PASSWORD=yourpassword

# Build release APK
./gradlew android:assembleRelease

# Output:
# android/build/outputs/apk/release/android-release.apk

# Build AAB for Play Store
./gradlew android:bundleRelease
```

---

## Common Issues & Fixes

### "SDK location not found"
Edit `local.properties` and set correct `sdk.dir` path.

### "Build failed: could not resolve LibGDX"
```bash
./gradlew android:dependencies --refresh-dependencies
```

### "App crashes on startup"
Check logcat:
```bash
adb logcat -s VillageLegends:D AndroidRuntime:E
```

### "No audio" (expected on first run without assets)
Audio files are optional. Place `.ogg` files in `android/assets/audio/`.
The game runs silently without them.

### "Black screen" with TMX maps
If `.tmx` files are missing, the placeholder coloured-rectangle world
renders automatically. Add TMX files to `android/assets/maps/` to
enable the full Tiled map renderer.

### "NullPointerException in Player"
Check that `activeSave` is set before `newGame()` or `loadGame()`.

---

## Gradle Tasks Reference

```bash
# Debug build + install
./gradlew android:installDebug

# Release APK
./gradlew android:assembleRelease

# Release AAB (Play Store)
./gradlew android:bundleRelease

# Clean all build outputs
./gradlew clean

# Show dependency tree
./gradlew android:dependencies

# Check for dependency updates
./gradlew dependencyUpdates
```

---

## Project Structure Quick Reference

```
VillageLegends2D/
├── core/src/main/java/com/villagelegends/
│   ├── VillageLegends.java         ← Start here
│   ├── GameConstants.java          ← Tune game values here
│   ├── screens/GameScreen.java     ← Main game loop
│   ├── entities/Player.java        ← Player logic
│   ├── systems/QuestManager.java   ← Add quests here
│   ├── systems/FarmingManager.java ← Farming logic
│   └── world/World.java            ← Map loading
├── android/assets/data/
│   ├── items.json      ← Item definitions
│   ├── crops.json      ← Crop definitions
│   ├── npcs.json       ← NPC data
│   ├── quests.json     ← Quest data
│   └── dialogues.json  ← NPC dialogues
└── docs/
    ├── DEVELOPMENT_ROADMAP.md
    ├── TILEMAP_DESIGN.md
    ├── SPRITE_REQUIREMENTS.md
    └── DATABASE_SCHEMA.md
```

---

## What's Working Right Now

✅ Full game loop (update → render)  
✅ Player movement + collision (4 directions, sprint, dodge)  
✅ Virtual joystick + 8 touch buttons (multi-touch)  
✅ Day/night cycle (5-min days, RGB world tinting)  
✅ Dynamic weather (rain particles, fog overlay)  
✅ NPC AI with 24-hour schedules  
✅ Dialogue system (branching conversations)  
✅ Quest system (30 main + 100 side quests defined)  
✅ Farming (9 crops, full growth/pest/harvest lifecycle)  
✅ Economy (dynamic pricing, buy/sell, vehicle dealer)  
✅ Combat (non-lethal, dodge/block/stealth)  
✅ Vehicles (6 types with fuel + physics)  
✅ 4 Mini-games (Fishing, Kabaddi, Tractor Race, Cricket)  
✅ 4 Festivals (Sankranti, Ugadi, Dasara, Deepavali)  
✅ Save system (3 slots, auto-save, JSON persistence)  
✅ Inventory UI (8×5 grid, item details)  
✅ Shop UI (buy/sell panel)  
✅ Quest Log UI (filterable quest list)  
✅ HUD (minimap, health, money, active quest)  
✅ 3 Languages (EN/TE/HI — runtime switching)  
✅ Placeholder world renderer (no art files needed)  

## What Needs Art/Audio Assets

🎨 Real sprites replacing coloured placeholder shapes  
🗺️ Tiled TMX map files for each of 6 regions  
🎵 12 OGG music tracks  
🔊 40+ OGG sound effects  
🖼️ Launcher icon (512×512 PNG + adaptive icon XML)  
