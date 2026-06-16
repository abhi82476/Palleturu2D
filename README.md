# Village Legends 2D
### A GTA 2–style open-world village game set in South India

```
 ██╗   ██╗██╗██╗     ██╗      █████╗  ██████╗ ███████╗
 ██║   ██║██║██║     ██║     ██╔══██╗██╔════╝ ██╔════╝
 ██║   ██║██║██║     ██║     ███████║██║  ███╗█████╗
 ╚██╗ ██╔╝██║██║     ██║     ██╔══██║██║   ██║██╔══╝
  ╚████╔╝ ██║███████╗███████╗██║  ██║╚██████╔╝███████╗
   ╚═══╝  ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝
         L E G E N D S  2D  —  విలేజ్ లెజెండ్స్
```

**Platform**: Android 8.0+ | **Engine**: LibGDX 1.12.1 | **Language**: Java 11
**Genre**: Top-down open-world action-adventure
**Languages**: English · Telugu · Hindi

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Project Structure](#3-project-structure)
4. [Quick Start — Build & Run](#4-quick-start)
5. [Core Systems](#5-core-systems)
6. [Game World](#6-game-world)
7. [Story & Quests](#7-story--quests)
8. [Controls](#8-controls)
9. [Asset Pipeline](#9-asset-pipeline)
10. [Configuration](#10-configuration)
11. [Performance Notes](#11-performance-notes)
12. [Extending the Game](#12-extending-the-game)

---

## 1. Project Overview

Village Legends 2D is a GTA 2–style top-down open-world game set in the villages and landscapes of South India (Andhra Pradesh). The player begins as a student/farmer and uncovers a criminal gang's operations, becoming a village hero through 30 main missions and 100 side quests.

### Core Features
| Feature | Details |
|---------|---------|
| World | 6 interconnected regions (village, farmlands, forest, lake, town, highway) |
| Story | 30 main missions across 3 acts with multiple endings |
| Side Content | 100 side quests (farming, fishing, racing, mini-games, festivals) |
| Farming | 9 crop types with full growth lifecycle |
| Vehicles | 6 vehicle types with fuel system and upgrades |
| Economy | Dynamic pricing with daily fluctuation |
| NPC AI | 13 NPC types with 24-hour schedules and reputation reactions |
| Festivals | 4 South Indian festivals (Sankranti, Ugadi, Dasara, Deepavali) |
| Day/Night | Full day/night cycle with 5-minute real-time days |
| Weather | Dynamic rain, fog, and clear weather |
| Languages | English, Telugu, Hindi |

---

## 2. Architecture

```
VillageLegends (Game)          ← Root LibGDX Game class
│
├── Screens
│   ├── SplashScreen           ← Studio logo + asset loading
│   ├── MainMenuScreen         ← New/Load/Settings/Language
│   ├── GameScreen             ← Main gameplay loop
│   └── SettingsScreen
│
├── Managers (owned by VillageLegends)
│   ├── GameEventBus           ← Decoupled pub/sub event system
│   ├── SaveManager            ← JSON serialise to SharedPreferences
│   ├── AudioManager           ← Music crossfade + SFX pool
│   ├── QuestManager           ← 30 main + 100 side quests
│   ├── NPCManager             ← NPC spawning + AI + dialogue
│   ├── EconomyManager         ← Prices + shops + vehicle dealer
│   ├── FarmingManager         ← Crop lifecycle + pest events
│   ├── VehicleManager         ← Vehicles + physics + AI bus
│   ├── CombatSystem           ← Non-lethal fight resolution
│   └── FestivalManager        ← Festival calendar + decorations
│
├── World
│   ├── World                  ← TMX map loading + collision + triggers
│   ├── DayNightCycle          ← Time progression + RGB tint
│   └── WeatherSystem          ← Rain/fog particle overlay
│
├── Entities
│   ├── Entity                 ← Base: position, health, collision
│   ├── Player                 ← Input, state machine, inventory
│   ├── NPC                    ← AI, schedule, dialogue
│   └── Vehicle                ← Angular physics, fuel, cargo
│
├── UI
│   ├── MobileControls         ← Multi-touch joystick + buttons
│   ├── HUD                    ← Minimap, health, quest, notifications
│   ├── InventoryUI            ← 8×5 inventory grid
│   └── DialogueUI             ← Branching conversation panel
│
├── MiniGames
│   ├── FishingGame            ← Cast → wait → bite → reel
│   └── KabaddiGame            ← Raid-and-tag turn-based game
│
└── Data
    ├── GameSaveData           ← Root serialisable save object
    ├── PlayerData             ← Player stats + inventory
    ├── Quest / QuestObjective ← Quest data model
    ├── Item                   ← Inventory item model
    ├── CropData               ← Crop type definitions
    └── FarmPlot               ← Farm plot runtime state
```

### Event-Driven Communication
Systems communicate through `GameEventBus` (publish/subscribe).  
This prevents tight coupling between managers.

```java
// Subscribe (in manager constructor)
game.eventBus.subscribe(EventType.CROP_HARVESTED, e ->
    questManager.advanceObjective("m02", "harvest_groundnut", 1));

// Publish (anywhere)
game.eventBus.post(new Event(EventType.CROP_HARVESTED, "groundnut"));
```

---

## 3. Project Structure

```
VillageLegends2D/
├── android/
│   ├── AndroidManifest.xml
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── java/com/villagelegends/android/
│       │   └── AndroidLauncher.java       ← App entry point
│       ├── res/values/strings.xml
│       └── assets/                        ← Symlinked from android/assets
│           ├── maps/                      ← .tmx region files
│           ├── textures/                  ← .atlas + .png
│           ├── audio/music/               ← .ogg music tracks
│           ├── audio/sfx/                 ← .ogg sound effects
│           ├── data/                      ← .json databases
│           └── fonts/                     ← .ttf font files
│
├── core/src/main/java/com/villagelegends/
│   ├── VillageLegends.java                ← Root Game class
│   ├── GameConstants.java                 ← All tuning constants
│   ├── screens/                           ← Screen classes
│   ├── world/                             ← World, DayNight, Weather
│   ├── entities/                          ← Player, NPC, Vehicle
│   ├── systems/                           ← All manager classes
│   ├── ui/                                ← HUD, controls, dialogs
│   ├── minigames/                         ← Mini-game implementations
│   └── data/                              ← Data model classes
│
├── docs/
│   ├── TILEMAP_DESIGN.md
│   ├── SPRITE_REQUIREMENTS.md
│   ├── DATABASE_SCHEMA.md
│   └── DEVELOPMENT_ROADMAP.md
│
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## 4. Quick Start

### Prerequisites
```bash
Java 11+               # java -version
Android SDK 34         # via Android Studio SDK Manager
Gradle 8.3             # via wrapper (./gradlew)
Android device/emulator# API 26+ (Android 8.0)
```

### Build & Install (Debug)
```bash
# Clone / unzip project
cd VillageLegends2D

# Make gradlew executable
chmod +x gradlew

# Install debug build to connected Android device
./gradlew android:installDebug

# Or build an APK file
./gradlew android:assembleDebug
# Output: android/build/outputs/apk/debug/android-debug.apk
```

### Release Build
```bash
# 1. Generate signing keystore (one time)
keytool -genkey -v -keystore vl2d.keystore \
        -alias vl2d -keyalg RSA -keysize 2048 -validity 10000

# 2. Add to local.properties (do NOT commit this file)
echo "RELEASE_STORE_FILE=../vl2d.keystore" >> local.properties
echo "RELEASE_STORE_PASSWORD=yourpassword" >> local.properties
echo "RELEASE_KEY_ALIAS=vl2d" >> local.properties
echo "RELEASE_KEY_PASSWORD=yourpassword" >> local.properties

# 3. Build release APK
./gradlew android:assembleRelease
# Output: android/build/outputs/apk/release/android-release.apk

# 4. Build AAB (for Google Play)
./gradlew android:bundleRelease
```

### Placeholder Assets
The game runs with placeholder coloured rectangles for sprites and silent audio until real assets are placed in:
- `android/assets/textures/` — `.atlas` + `.png` files
- `android/assets/audio/music/` — `.ogg` music files  
- `android/assets/audio/sfx/` — `.ogg` sound effect files
- `android/assets/maps/` — `.tmx` Tiled map files

---

## 5. Core Systems

### 5.1 Player State Machine
```
IDLE ←→ WALK ←→ RUN
  ↓        ↓
ATTACK   DODGE
  ↓
DIALOGUE (frozen)
IN_VEHICLE (delegates movement to Vehicle)
FARMING / FISHING (context-locked)
```

### 5.2 Quest System
```
LOCKED → AVAILABLE → ACTIVE → COMPLETE
                           ↘ FAILED
```
Requirements checked on every `NEW_DAY` event:
- `moneyRequirement` — player must have at least this much money
- `reputationTier` — reputation thresholds (0=any, 1=20, 2=50 …)
- `requiredFlag` — game flag that must be set

### 5.3 Farming Lifecycle
```
EMPTY → [plow] → PLOWED → [plant seed] → PLANTED
  ↑                                          ↓
  ↑                                       GROWING (water required)
  ↑                                          ↓
  ← ← ← [harvest] ← ← MATURE ← ← ← ← ← ← ←
                            ↘ DEAD (drought/pests)
```

### 5.4 Economy
- Prices drift ±15% each in-game day
- Buy price = base × 1.2 − reputation discount (max 20%)
- Sell price = base × 0.8 + reputation bonus (max 15%)
- Market closes at 9 PM, opens at 7 AM

### 5.5 Day/Night Cycle
- 1 real second = ~4.8 in-game minutes (5-minute real day)
- Tint blended through 5 key frames: midnight → dawn → day → sunset → dusk → night
- NPCs follow 24-hour schedules keyed to in-game hour

---

## 6. Game World

| Region | Size | Key Locations |
|--------|------|---------------|
| Main Village | 128×128 tiles | Temple, panchayat, market, school, bus stand |
| Farmlands | 96×96 tiles | 6 farm zones, barn, irrigation channels |
| Forest | 160×160 tiles | Smuggler camp, ruins, 3 treasures, forest post |
| Lake | 128×96 tiles | Dock, fishing zones, island treasure |
| Town | 192×160 tiles | Large market, vehicle dealer, police station |
| Highway | 256×64 tiles | 4 bus stops, petrol bunk, police checkpoint |

Region transitions happen automatically when the player walks to the edge of a map (trigger zone). Interior maps load on door interaction.

---

## 7. Story & Quests

### Act 1 — Village Life Begins (m01–m10)
Meet village characters, start farming, discover the gang.

### Act 2 — Rising Hero (m11–m20)  
Deeper investigation, temple defence, lake smugglers, expose the corrupt official.

### Act 3 — Final Showdown (m21–m30)
Village militia, highway blockade, forest base assault, boss fights, court testimony.

### Multiple Endings
- **Hero Ending**: All evidence collected, high reputation (200+), m30 complete
- **Vigilante Ending**: Some evidence missing, moderate reputation
- **Outlaw Ending**: Negative reputation — village distrusts you despite victory

### Side Quest Categories
| Category | Count | Examples |
|----------|-------|---------|
| Farming | 8 | Grow 50 rice bags, build irrigation |
| Fishing | 6 | Catch legendary catfish, supply 30 fish |
| Racing | 4 | Win tractor derby, motorcycle race |
| Mini-games | 5 | Kabaddi champion, cricket cup |
| Delivery | 8 | Morning milk run, emergency medicine |
| Treasure | 4 | Ancient treasure, temple ruins |
| Wildlife | 4 | Wild boar problem, leopard sighting |
| Festival | 8 | Sankranti kite, Deepavali lamps |
| Investigation | 12 | Corruption trails, witness statements |
| Misc | 41 | Various village life activities |

---

## 8. Controls

### Mobile Layout (Landscape)
```
┌────────────────────────────────────────────────────────────────┐
│  [MAP] [QUEST] [INV] [PAUSE]                        (top right)│
│                                                                │
│                                                                │
│  ◉ Joystick           [DODGE]  [ATTACK]                        │
│  (left half)          [SPRINT] [ACTION]             (right)    │
│                                                   40px from edge│
└────────────────────────────────────────────────────────────────┘
```

| Button | On Foot | In Vehicle |
|--------|---------|------------|
| Joystick | Move player | Steer vehicle |
| ATTACK | Swing weapon | — |
| ACTION | Interact / Mount | Dismount |
| SPRINT | Sprint (drains stamina) | Accelerate |
| DODGE | Roll dodge | Brake |
| MAP | Toggle minimap expanded view | Same |
| QUEST | Open quest log | Same |
| INV | Open inventory | — |
| PAUSE | Pause menu | Same |

---

## 9. Asset Pipeline

### Tiled Maps (.tmx)
1. Open `Tiled` → New Map → Orthogonal, 32×32 tiles
2. Import `android/assets/textures/village_atlas.png` as tileset
3. Create layers: Ground, Water, Decor, Objects, Collision, Triggers, Overhead
4. Export to `android/assets/maps/<region_name>.tmx`

### Sprites (TexturePacker)
1. Create folders: `assets/textures/raw/<atlas_name>/`
2. Drop PNG frames into folders
3. Pack: `TexturePacker assets/textures/raw/<atlas_name>/ --format libgdx --output android/assets/textures/`
4. Outputs: `<atlas_name>.atlas` + `<atlas_name>.png`

### Audio (Audacity → OGG)
1. Record/source audio at 44.1 kHz stereo (music) or mono (SFX)
2. Export: `File → Export → Export as OGG Vorbis`
   - Music: Quality 5 (~128 kbps)
   - SFX: Quality 3 (~80 kbps)
3. Place in `android/assets/audio/music/` or `android/assets/audio/sfx/`
4. Filename must match the key in `AudioManager.loadSfx()` / `playTrack()`

### Fonts (FreeType)
1. Download NotoSans-Regular.ttf and NotoSansTelugu-Regular.ttf
2. Place in `android/assets/fonts/`
3. Load via LibGDX FreeTypeFontGenerator:
```java
FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
    Gdx.files.internal("fonts/NotoSansTelugu-Regular.ttf"));
FreeTypeFontParameter param = new FreeTypeFontParameter();
param.size = 24;
param.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "అఆఇఈఉఊఋఌఎఏఐఒఓఔకఖగఘ…";
BitmapFont teluguFont = gen.generateFont(param);
```

---

## 10. Configuration

All game balance values are in `GameConstants.java`:

```java
// Player tuning
PLAYER_SPEED           = 120f    // pixels/second walking
PLAYER_RUN_SPEED       = 190f    // running
PLAYER_SPRINT_BOOST    = 1.7f   // multiplier when sprinting
PLAYER_STAMINA_MAX     = 100f
PLAYER_STAMINA_DRAIN   = 20f    // per second sprinting
PLAYER_STAMINA_REGEN   = 10f    // per second idle/walking

// Time
DAY_DURATION_SECONDS   = 300f   // 5 real minutes per day

// Economy
PRICE_FLUCTUATION      = 0.15f  // ±15% daily price drift

// Farming
CROP_PEST_CHANCE       = 0.05f  // 5% chance per day per plot
```

---

## 11. Performance Notes

### Target
- **Minimum**: Snapdragon 430, 2GB RAM, Android 8.0 → 30 FPS
- **Target**: Snapdragon 680, 4GB RAM, Android 10+ → 60 FPS

### Optimisation Checklist
- [ ] Texture atlases — all sprites in max 4 atlases (2048×2048 each)
- [ ] Camera frustum culling — only render visible tiles (LibGDX does this by default)
- [ ] NPC updates — skip full AI for NPCs > 600px from player
- [ ] Object pooling — rain drops, confetti particles, hit sparks
- [ ] String allocation — avoid new String() in render loops
- [ ] GC pressure — use LibGDX Array/ObjectMap over Java collections in hot paths
- [ ] SpriteBatch — keep a single batch for the game; never create per-frame

### Known Heavy Areas
| Area | Issue | Solution |
|------|-------|---------|
| Forest region | Dense tile overdraw | Clip tree canopy layers |
| Rain heavy | Many particles | Limit to 300 drops max |
| Festival fireworks | Many particles | Pool reuse, max 80 |
| NPC pathfinding | Full recalculate each schedule change | Cache path, only rebuild on destination change |

---

## 12. Extending the Game

### Adding a New Crop
1. Add entry in `FarmingManager.buildCropDatabase()`:
```java
addCrop("banana", "Banana", 90, 0.14f, 8, 18, 65);
```
2. Add price in `EconomyManager.buildPriceTable()`:
```java
setPrice("banana_seed", 30);
setPrice("banana_bag",  65);
```
3. Add seed + bag entries to `android/assets/data/items.json`
4. Add 5-stage sprite sheet: `banana_stage_0..4.png`

### Adding a New Quest
```java
// In QuestManager.registerSideQuests()
addQuest("s101_new_quest", "A New Quest",
    "Description of the quest.",
    Quest.Type.SIDE, 100, 0,
    obj("collect", "item_id",  3, "Collect 3 items"),
    obj("talk",    "npc_id",   1, "Speak to the NPC")
).setReward(300, 15, null);
```

### Adding a New NPC
```java
// In NPCManager.spawnMainVillage() or relevant region method
NPC myNPC = new NPC(game, "unique_npc_id", "Display Name",
                    NPC.Type.VILLAGER, tileX * 32, tileY * 32);
myNPC.setDialogueId("dlg_my_npc");
myNPC.setQuestGiver("s101_new_quest");  // optional
activeNPCs.add(myNPC);
```

Then add the dialogue node in `DialogueUI.buildDialogueDB()`.

### Adding a New Region
1. Create `android/assets/maps/my_region.tmx` in Tiled
2. Add case in `World.loadRegion()` (already falls back gracefully)
3. Add NPCs in `NPCManager.spawnNPCsForRegion()`
4. Add vehicles in `VehicleManager.spawnVehiclesForRegion()`
5. Add music track in `AudioManager.playRegionMusic()`
6. Add trigger zones on edges of neighbouring regions

---

## Credits & Licence

**Village Legends 2D** — Pixel South Studios  
Engine: [LibGDX](https://libgdx.com) (Apache 2.0)  
Map editor: [Tiled](https://www.mapeditor.org) (GPL 2+)  
Art pipeline: [TexturePacker](https://www.codeandweb.com/texturepacker)

Music references (to be licensed):
- Carnatic classical instrumentals (veena, mridangam, flute)
- South Indian folk percussion for festival tracks
- Ambient village soundscapes

All game content, story, and South Indian cultural elements are created with respect and appreciation for the rich cultural heritage of Andhra Pradesh.

---

*For questions or contributions, open an issue in the repository.*
