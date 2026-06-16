# Village Legends 2D — Database Schema
# Storage: LibGDX Preferences (SharedPreferences on Android)
# Format:  JSON strings, one key per save slot
# Library: LibGDX built-in Json

══════════════════════════════════════════════
  TOP-LEVEL SAVE STRUCTURE
══════════════════════════════════════════════

Preferences key:  "vl2d_saves"

Keys stored:
  "slot_0"  → JSON string: GameSaveData (slot 0)
  "slot_1"  → JSON string: GameSaveData (slot 1)
  "slot_2"  → JSON string: GameSaveData (slot 2)
  "meta_0"  → String: "Save 1|12h 30m|Day 14|₹4500"
  "meta_1"  → String: "Save 2|2h 5m|Day 3|₹800"
  "meta_2"  → String: "Empty Slot"

══════════════════════════════════════════════
  GameSaveData (root object)
══════════════════════════════════════════════

  Field                Type        Default   Description
  ─────────────────────────────────────────────────────────
  slot                 int         0         Save slot index (0-2)
  saveName             String      "Save 1"  Display name
  lastSaveTimestamp    long        0         Unix ms epoch
  totalPlayTime        float       0.0       Real seconds played
  gameHour             float       6.0       In-game hour (0..24)
  currentDay           int         1         Day counter

  playerData           PlayerData  (see below)
  questStates          Map<String,Quest.State>    quest progress
  objectiveProgress    Map<String,Integer>         "questId.objId" → count
  gameFlags            Set<String>                story flag strings
  defeatedNPCIds       Set<String>                defeated hostile NPC ids
  farmPlots            List<FarmPlot>             all farm plot states
  activeFestival       String      "NONE"    Festival.name()
  festivalTimer        float       0.0       Seconds into current festival
  musicVolume          float       0.7
  sfxVolume            float       1.0
  language             String      "en"
  miniGameScores       Map<String,Integer>         minigame high scores

══════════════════════════════════════════════
  PlayerData
══════════════════════════════════════════════

  Field                   Type          Default
  ──────────────────────────────────────────────
  worldX                  float         400.0
  worldY                  float         300.0
  currentRegion           String        "main_village"
  health                  int           100
  maxHealth               int           100
  stamina                 float         100.0
  day                     int           1
  money                   int           500
  reputation              int           0
  inventoryItems          List<Item>    []
  ownedVehicleIds         List<String>  []
  hotbarSlot              int           0
  totalCropsHarvested     int           0
  totalFishCaught         int           0
  totalEnemiesDefeated    int           0
  totalQuestsComplete     int           0
  totalDistanceTravelled  float         0.0
  highestReputationEver   int           0
  visitedRegions          Set<String>   ["main_village"]
  farmPlotsOwned          int           6

══════════════════════════════════════════════
  Item (inventory object)
══════════════════════════════════════════════

  Field          Type      Description
  ──────────────────────────────────────────────
  id             String    Unique item identifier
  displayName    String    Shown in UI
  category       Enum      SEED/CROP/TOOL/WEAPON/CONSUMABLE/QUEST_ITEM/FUEL/FISH/MISC
  price          int       Current market value (can fluctuate)
  quantity       int       Stack count
  stackable      boolean   Can stack in single slot
  description    String    Item tooltip text
  iconRegion     String    TextureAtlas region name "item_<id>"

══════════════════════════════════════════════
  FarmPlot (one cultivated tile)
══════════════════════════════════════════════

  Field              Type      Default   Description
  ────────────────────────────────────────────────────
  plotId             int       auto      Unique plot index
  worldX             float               World pixel X
  worldY             float               World pixel Y
  state              Enum      EMPTY     EMPTY/PLOWED/PLANTED/GROWING/MATURE/DEAD
  cropId             String    null      Active crop type id
  growthHours        float     0.0       Accumulated growth hours
  waterLevel         float     1.0       0..1 hydration
  hasPest            boolean   false
  pestDamage         float     0.0       0..1 pest damage
  dehydrationTimer   float     0.0       Hours without water

══════════════════════════════════════════════
  Quest State Map values
══════════════════════════════════════════════

  Quest.State enum values (stored as String):
    LOCKED      — prerequisites not met
    AVAILABLE   — can be started (shows ! marker on NPC)
    ACTIVE      — in progress
    COMPLETE    — finished, rewards granted
    FAILED      — timed out or conditions violated

══════════════════════════════════════════════
  Game Flags Reference (important ones)
══════════════════════════════════════════════

  Flag String               Set By               Unlocks
  ───────────────────────────────────────────────────────────────
  intro_done                m01 complete         m02, m03
  school_complete           m03 complete         m04
  gang_known                m06 complete         m07, m08, gang thugs hostile
  first_arrest              combat+m07           m09
  temple_saved              m07 complete         m14
  chief_exposed             m15 complete         m19, m20
  game_complete             m30 complete         credits screen
  sankranti_active          Day 14               s16, s17 festival quests
  ugadi_active              Day 30               ugadi quests
  dasara_active             Day 60               m14 prerequisite
  deepavali_active          Day 75               deepavali quests
  m01_intro_done            m01 complete
  m07_temple_robbery_done   m07 complete
  … (pattern: <questId>_done for all completed quests)

══════════════════════════════════════════════
  MiniGame High Scores Map
══════════════════════════════════════════════

  Key                    Value Type  Description
  ──────────────────────────────────────────────
  "fishing_biggest"      int         Biggest fish catch (grams)
  "fishing_most_day"     int         Most fish in one day
  "kabaddi_score"        int         Best kabaddi score
  "cricket_runs"         int         Highest single innings runs
  "tractor_race_time"    int         Best lap time (milliseconds)
  "bike_race_time"       int         Best bike race time
  "kite_contest_score"   int         Kite contest score
  "treasure_found"       int         Total treasures found (0-8)

══════════════════════════════════════════════
  JSON SERIALISATION EXAMPLE (abbreviated)
══════════════════════════════════════════════

{
  "slot": 0,
  "saveName": "My Village",
  "lastSaveTimestamp": 1703846400000,
  "totalPlayTime": 3720.5,
  "gameHour": 8.5,
  "currentDay": 7,
  "playerData": {
    "worldX": 640.0,
    "worldY": 480.0,
    "currentRegion": "main_village",
    "health": 85,
    "maxHealth": 100,
    "stamina": 72.3,
    "day": 7,
    "money": 2450,
    "reputation": 45,
    "inventoryItems": [
      { "id": "hoe", "displayName": "Farmer's Hoe", "category": "TOOL",
        "price": 60, "quantity": 1, "stackable": false },
      { "id": "groundnut_bag", "displayName": "Groundnut Bag", "category": "CROP",
        "price": 62, "quantity": 4, "stackable": true }
    ],
    "ownedVehicleIds": ["bicycle"],
    "visitedRegions": ["main_village", "farmlands", "lake"],
    "farmPlotsOwned": 6,
    "totalCropsHarvested": 28,
    "totalFishCaught": 12
  },
  "questStates": {
    "m01_intro": "COMPLETE",
    "m02_first_harvest": "COMPLETE",
    "m03_school_day": "COMPLETE",
    "m04_strange_smoke": "ACTIVE",
    "s01_rice_harvest": "AVAILABLE"
  },
  "objectiveProgress": {
    "m04_strange_smoke.visit_forest_north": 1,
    "m04_strange_smoke.collect_evidence_crate": 0
  },
  "gameFlags": ["intro_done", "school_complete", "m01_intro_done",
                "m02_first_harvest_done", "m03_school_day_done"],
  "defeatedNPCIds": [],
  "farmPlots": [
    { "plotId": 0, "worldX": 160.0, "worldY": 160.0,
      "state": "GROWING", "cropId": "groundnut",
      "growthHours": 24.5, "waterLevel": 0.62,
      "hasPest": false, "pestDamage": 0.0 },
    { "plotId": 1, "worldX": 224.0, "worldY": 160.0,
      "state": "PLOWED", "cropId": null,
      "growthHours": 0.0, "waterLevel": 1.0 }
  ],
  "activeFestival": "NONE",
  "festivalTimer": 0.0,
  "musicVolume": 0.7,
  "sfxVolume": 1.0,
  "language": "te",
  "miniGameScores": {
    "fishing_biggest": 850,
    "kabaddi_score": 12,
    "treasure_found": 2
  }
}

══════════════════════════════════════════════
  SETTINGS PREFERENCES (separate preference file)
══════════════════════════════════════════════

Preference file: "vl2d_settings"

  Key                  Type    Default   Description
  ──────────────────────────────────────────────────
  music_volume         float   0.7       0.0..1.0
  sfx_volume           float   1.0       0.0..1.0
  language             String  "en"      "en"|"te"|"hi"
  control_size         int     1         0=small, 1=medium, 2=large
  screen_shake         boolean true
  show_minimap         boolean true
  last_save_slot       int     0         Most recently used slot
  total_sessions       int     0         Number of app launches
  first_launch         boolean true
