# Village Legends 2D — Development Roadmap

## Project Overview
- **Engine**: LibGDX 1.12.1
- **Language**: Java 11
- **Platform**: Android 8.0+ (API 26+)
- **Target**: 60 FPS on mid-range Android (Snapdragon 680, 4GB RAM)
- **Team size**: 1–3 developers (solo-developer friendly)

---

## Phase 0 — Foundation (Weeks 1–2)

### Environment Setup
- [ ] Install Android Studio + SDK (API 26, 34)
- [ ] Install LibGDX project template, import VillageLegends2D
- [ ] Set up Git repository (main / develop / feature/* branching)
- [ ] Configure Gradle for LibGDX 1.12.1
- [ ] Connect Android test device; verify 60 FPS on LibGDX "Hello World"

### Tooling
- [ ] Download and configure **Tiled** map editor (https://www.mapeditor.org)
- [ ] Download **TexturePacker** (https://www.codeandweb.com/texturepacker)
- [ ] Download **Audacity** or similar for audio editing
- [ ] Procure/create placeholder 32×32 pixel art tileset (can use Kenney.nl assets initially)

### Deliverable: Project compiles and launches on device with a placeholder coloured square moving with the joystick.

---

## Phase 1 — Core Engine (Weeks 3–6)

### Week 3–4: World & Camera
- [ ] Implement World.java – load placeholder TMX map
- [ ] Tile collision detection (static colliders from Objects layer)
- [ ] OrthographicCamera smooth follow with world-bounds clamping
- [ ] FitViewport letterboxing (1280×720 virtual resolution)
- [ ] DayNightCycle – time progression, RGB tint blending
- [ ] WeatherSystem – rain particle overlay

### Week 5–6: Player & Controls
- [ ] MobileControls – virtual joystick + 6 buttons (multi-touch)
- [ ] Player movement (walk, run, sprint, dodge)
- [ ] Tile collision sliding (X/Y separated)
- [ ] Player facing direction + placeholder rectangle sprite
- [ ] Player health + stamina bars in HUD
- [ ] Basic HUD: HP, stamina, money, clock display

### Deliverable: Player walks around a real Tiled map with working collision.

---

## Phase 2 — NPC & World Systems (Weeks 7–10)

### Week 7–8: NPC Foundation
- [ ] NPC entity with schedule-driven waypoint movement
- [ ] NPCManager – spawn NPCs per region
- [ ] Proximity detection (nearest NPC within interaction radius)
- [ ] DialogueUI – conversation panel with branching choices
- [ ] Dialogue database (hardcoded for main characters)

### Week 9–10: World Interactions
- [ ] Region transitions (trigger zones → load new TMX)
- [ ] World.interact() – farm plots, chests, doors
- [ ] FarmingManager – full crop lifecycle (EMPTY→MATURE)
- [ ] Farming UI: plant, water, harvest prompts
- [ ] EconomyManager – price table, buy/sell transactions
- [ ] InventoryUI – 8×5 grid, item detail panel

### Deliverable: Player can talk to NPCs, buy seeds, plant crops, harvest and sell.

---

## Phase 3 — Vehicles & Combat (Weeks 11–14)

### Week 11–12: Vehicles
- [ ] Vehicle entity – angular-momentum physics, fuel system
- [ ] All 6 vehicle types tuned (speed, turn radius, fuel drain)
- [ ] Mount/dismount interaction
- [ ] VehicleManager – spawn per region, AI bus loop
- [ ] Vehicle fuel bar in HUD
- [ ] Vehicle dealer shop in Town region

### Week 13–14: Combat
- [ ] CombatSystem – player attack sweep, NPC attack on player
- [ ] Stealth takedown mechanic (3× damage from behind)
- [ ] Block and dodge windows
- [ ] NPC hostile detection radius + chase AI
- [ ] Screen shake on heavy hits
- [ ] Combat-specific HUD indicators

### Deliverable: Player can drive all vehicles and engage in non-lethal combat with gang NPCs.

---

## Phase 4 — Quest System & Story (Weeks 15–20)

### Week 15–16: Quest Engine
- [ ] QuestManager – full state machine (LOCKED → AVAILABLE → ACTIVE → COMPLETE)
- [ ] Objective tracking: collect, defeat, talk, visit, harvest, sell
- [ ] Game flag system (quest prerequisites, story gates)
- [ ] QuestLog UI screen (active quests, completed, total progress)
- [ ] Quest reward distribution (money, reputation, items)

### Week 17–18: Story Missions (Act 1)
- [ ] Implement m01–m10 main missions
- [ ] Place trigger zones on TMX maps
- [ ] Populate NPC dialogues for all story characters
- [ ] Crime scene investigation mechanic (collect evidence objects)
- [ ] Temple defence sequence (timed wave fight)

### Week 19–20: Story Missions (Acts 2–3)
- [ ] Implement m11–m30 main missions
- [ ] Boss fight mechanics (Vikram Reddy phase transitions)
- [ ] Final boss (Narayana Rao) – multi-phase fight
- [ ] Multiple endings trigger (based on evidence collected and reputation)
- [ ] Cutscene text overlay system

### Deliverable: Full 30-mission story playthrough possible from start to credits.

---

## Phase 5 — Side Content (Weeks 21–26)

### Week 21–22: Mini-Games
- [ ] FishingGame – cast → wait → bite → reel timing game
- [ ] KabaddiGame – raid and tag turn-based mini-game
- [ ] Cricket mini-game – batting timing challenge (3 balls, score runs)
- [ ] Tractor racing – AI opponent on circuit lap
- [ ] Kite flying – direction control during Sankranti

### Week 23–24: Side Quests (50 of 100)
- [ ] Farming quests: s01–s03
- [ ] Fishing quests: s04–s05
- [ ] Racing quests: s06–s07
- [ ] Mini-game quests: s08–s09
- [ ] Delivery quests: s10–s11
- [ ] Treasure hunts: s12–s13
- [ ] Wildlife quests: s14–s15

### Week 25–26: Festivals & Remaining Side Quests
- [ ] FestivalManager – Sankranti, Ugadi, Dasara, Deepavali
- [ ] Festival decorations (confetti, diya particles, kite objects)
- [ ] Festival missions (s16–s17 + new festival quests)
- [ ] Remaining side quests s18–s100 (use template system)
- [ ] NPC schedule updates during festivals

### Deliverable: 50 side quests playable, all 4 festivals activate with unique content.

---

## Phase 6 — Art & Audio (Weeks 27–32)

### Week 27–28: Pixel Art Production
- [ ] Complete tileset: village_atlas.png (all tiles listed in SPRITE_REQUIREMENTS.md)
- [ ] Player character all animation frames
- [ ] All 12 NPC types animated
- [ ] 6 vehicle sprites (all directions)
- [ ] All items.atlas (50+ item icons)

### Week 29–30: Effects & UI Polish
- [ ] All effects.atlas (combat, farming, weather, festival)
- [ ] UI atlas (HUD, buttons, panels)
- [ ] Telugu/English font integration (NotoSans TTF via LibGDX FreeType)
- [ ] Minimap region colour coding
- [ ] Day/night sprite tinting validation

### Week 31–32: Audio Production
- [ ] 12 background music tracks (OGG format)
- [ ] 40+ sound effects (OGG format)
- [ ] AudioManager crossfade testing
- [ ] Music volume / SFX volume settings validation

### Deliverable: Game looks and sounds like a complete product.

---

## Phase 7 — Save, Settings & Polish (Weeks 33–36)

### Week 33–34: Save System
- [ ] SaveManager full validation (serialize → deserialize round trip)
- [ ] 3 save slots with thumbnail preview (day, money, region)
- [ ] Auto-save every 5 real-world minutes
- [ ] Save on app backgrounding (Activity.onPause)
- [ ] Load game continues from last auto-save position

### Week 35–36: Settings & Accessibility
- [ ] Music volume slider (0–100%)
- [ ] SFX volume slider
- [ ] Control size options (small / medium / large joystick)
- [ ] Language selection (EN / TE / HI) with instant UI refresh
- [ ] Colour blind mode (optional: high-contrast health bars)
- [ ] Screen shake intensity toggle

### Deliverable: Complete settings menu, three stable save slots, language switching.

---

## Phase 8 — Testing & Optimisation (Weeks 37–40)

### Week 37–38: Performance
- [ ] Profile on minimum spec device (Snapdragon 430, 2GB RAM)
- [ ] Texture atlas packing — ensure all atlases under 2048×2048
- [ ] Object pooling for particles (rain drops, confetti)
- [ ] NPC update budget (skip off-screen NPCs)
- [ ] Spatial hash or quadtree for collision checks
- [ ] Reduce draw calls: batch all entities by atlas
- [ ] Profile GC pressure; eliminate per-frame allocations

### Week 39–40: Bug Fixing & Balance
- [ ] Play test all 30 main missions end-to-end
- [ ] Play test 20 representative side quests
- [ ] Economy balance: price ranges, quest money rewards
- [ ] NPC AI edge cases (stuck pathfinding, double-spawn)
- [ ] Save/load edge cases (interrupted save, corrupt slot recovery)
- [ ] Soft-lock prevention (ensure every quest is completable)

### Deliverable: Game runs at 60 FPS on mid-range, all missions completable without crashes.

---

## Phase 9 — Release (Weeks 41–44)

### Week 41–42: Release Build
- [ ] Generate release keystore: `keytool -genkey -keyalg RSA -keysize 2048 ...`
- [ ] Configure `local.properties` with keystore path (DO NOT commit)
- [ ] Enable ProGuard minification
- [ ] Build release APK: `./gradlew android:assembleRelease`
- [ ] Build AAB (for Play Store): `./gradlew android:bundleRelease`
- [ ] Sign and zipalign APK manually if needed

### Week 43–44: Store Submission
- [ ] Create Google Play Developer account (USD 25 one-time)
- [ ] Prepare store listing:
  - Icon 512×512 PNG
  - Feature graphic 1024×500 PNG
  - 8 screenshots (phone + tablet)
  - Short description (80 chars)
  - Full description (4000 chars, EN + TE)
- [ ] Content rating questionnaire (Everyone / ESRB E10+)
- [ ] Privacy policy URL (required)
- [ ] Submit to internal testing → closed testing → production

---

## Post-Launch Roadmap

### v1.1 (Month 2)
- Online leaderboard for racing and fishing high scores
- 10 additional side quests based on player feedback
- Hindi language completion

### v1.2 (Month 4)
- 4-player co-op (WiFi LAN, same local network)
- Festival-exclusive online trading market

### v1.3 (Month 6)
- New region: Hill Station (player travels to cooler mountain area)
- New vehicle: Jeep
- Premium DLC: Extended story epilogue (5 bonus missions)

---

## Technical Debt Tracker

| Item | Priority | Status |
|------|----------|--------|
| Replace placeholder ShapeRenderer sprites with TextureAtlas | HIGH | Pending |
| Implement A* pathfinding for NPCs | HIGH | Stub only |
| Add LibGDX Ashley ECS for scalable entity management | MEDIUM | Not started |
| JSON-driven dialogue loading (not hardcoded) | MEDIUM | Hardcoded |
| Spatial hash for collision optimisation | MEDIUM | Not started |
| Cloud save via Google Play Games API | LOW | Not started |
| Multiplayer via Kryonet or Netty | LOW | Not started |

---

## Build Commands Reference

```bash
# Debug build (install directly to connected device)
./gradlew android:installDebug

# Release APK
./gradlew android:assembleRelease

# Release AAB (for Play Store)
./gradlew android:bundleRelease

# Clean build
./gradlew clean

# Run on desktop (for faster iteration — add desktop module)
./gradlew desktop:run

# Check for dependency updates
./gradlew dependencyUpdates
```

## File Size Budget (uncompressed assets)

| Category        | Budget   | Notes |
|-----------------|----------|-------|
| Texture atlases | 32 MB    | 6 atlases × ~5MB |
| Music (OGG)     | 48 MB    | 12 tracks × ~4MB |
| Sound effects   | 8 MB     | 40 SFX × ~200KB |
| Maps (TMX)      | 4 MB     | 8 regions × ~500KB |
| JSON data       | 1 MB     | Items, crops, quests |
| Fonts (TTF)     | 2 MB     | 3 font files |
| **Total**       | **95 MB**| Well under 100MB Play limit |
