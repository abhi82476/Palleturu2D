# Village Legends 2D — Tilemap Design Specification
# All maps exported as TMX (Tiled Map Editor format), 32×32 tiles
# Tool: Tiled 1.10+ | Engine: LibGDX OrthogonalTiledMapRenderer

════════════════════════════════════════════════════════════
  GLOBAL LAYER STACK (same order in every .tmx file)
════════════════════════════════════════════════════════════

  Layer 0 — Ground        (always visible, no collision)
  Layer 1 — Water         (river, lake, pond tiles)
  Layer 2 — Decor         (flowers, grass tufts, rocks)
  Layer 3 — Objects       (walls, trees, buildings — solid=true property)
  Layer 4 — Collision     (Object layer type, invisible rectangles)
  Layer 5 — Triggers      (Object layer — region_change, quest, shop zones)
  Layer 6 — Overhead      (roof tiles, tree canopies — drawn above entities)

════════════════════════════════════════════════════════════
  TILESET: village_atlas.png  (exported from TexturePacker)
════════════════════════════════════════════════════════════

  Ground tiles (row 0):
    g00 = Dry soil          g01 = Fertile soil
    g02 = Grass short       g03 = Grass long
    g04 = Sand              g05 = Mud
    g06 = Stone path        g07 = Brick road
    g08 = Highway asphalt   g09 = Dirt road

  Water tiles (row 1, animated 3-frame):
    w00–w02 = Deep water    w03–w05 = Shallow water
    w06–w08 = Riverbank     w09 = Waterfall top

  Buildings (rows 2–5, 2×2 and 3×3 tile objects):
    House (small)      2×3 tiles   — roof layer included
    House (large)      3×4 tiles
    Temple             5×6 tiles   — elaborate roof
    School             4×3 tiles
    Market stall       2×2 tiles
    Panchayat office   4×4 tiles
    Bus stand          3×2 tiles
    Barn               3×4 tiles
    Well               1×1 tile
    Boat dock          2×3 tiles
    Gang hideout       4×4 tiles   — forest variant

  Nature (rows 6–8):
    Coconut palm       1×3 (trunk+canopy separate layers)
    Mango tree         2×3
    Banyan tree        3×4
    Bush               1×1
    Flower cluster     1×1 (4 colour variants)
    Sugar cane field   1×2 (4 growth stages)
    Paddy field        1×1 (5 growth stages)
    Tall grass         1×1 (animated 2-frame)
    Rock large         2×2
    Rock small         1×1

════════════════════════════════════════════════════════════
  REGION: main_village.tmx   (128×128 tiles = 4096×4096 px)
════════════════════════════════════════════════════════════

  Top-left:    Temple complex (5×6) at tile (10,10)
  Centre:      Panchayat office (4×4) at tile (55,60)
               Village well at (58,70)
  Right area:  Market row (6× stalls 2×2) at tiles (80,40)–(92,40)
  Bottom:      Bus stand (3×2) at tile (60,100)
               School (4×3) at tile (25,90)
  Scattered:   12× houses across map
               Coconut palms lining roads
               Flower gardens near temple
  Roads:       Brick road from bus stand → market → temple (L-shaped)
               Dirt road connecting farms

  SPAWN POINT (player):  tile (50,60) = pixel (1600, 1920)

  TRIGGERS:
    (0,0)–(128,10)      = NORTH edge → region_change: forest
    (118,0)–(128,128)   = EAST edge  → region_change: highway
    (0,115)–(128,128)   = SOUTH edge → region_change: farmlands
    (0,0)–(10,128)      = WEST edge  → region_change: lake
    Rect at market stall 1 → shop: village_general
    Rect at panchayat door → quest_trigger: m05_first_theft
    Rect at temple door    → quest_trigger: m07_temple_robbery

════════════════════════════════════════════════════════════
  REGION: farmlands.tmx   (96×96 tiles)
════════════════════════════════════════════════════════════

  Rows of paddy fields alternating with irrigation channels
  6 farm plot zones (each 3×3 tiles, marked with farmplot_N object)
  Barn in top-right corner
  Dirt path grid pattern
  Bullock cart track running N-S through centre

  TRIGGERS:
    North edge  → region_change: main_village
    South edge  → region_change: highway

════════════════════════════════════════════════════════════
  REGION: forest.tmx   (160×160 tiles)
════════════════════════════════════════════════════════════

  Dense tree coverage (mango + banyan) — 60% coverage
  Winding path from south entrance to north clearing
  Hidden clearing at (80,30): smuggler camp (4×4 object group)
  3× buried treasure chests (marked as chest_N objects)
  Old temple ruins at (40,40): 3×3 ruin tile cluster
  Dark undergrowth tiles between trees
  Forest officer post near south entrance

  TRIGGERS:
    South edge  → region_change: main_village
    North edge  → region_change: highway
    Smuggler camp rect → quest_trigger: m04_strange_smoke
    Chest rects → chest interaction objects

  NOTE: Night ambient light = 0.10 in this region (very dark)

════════════════════════════════════════════════════════════
  REGION: lake.tmx   (128×96 tiles)
════════════════════════════════════════════════════════════

  60% water surface (animated deep-water tiles)
  Dockside village on east shore (6× houses, fishing sheds)
  Boat dock at (20,48) — boat vehicle spawn point
  Fishing zone markers as invisible trigger objects
  Shallow reed-beds along north and south shores
  Rocky island at centre (20×15 tiles) — treasure location
  Waterfall feature at north edge

  TRIGGERS:
    East edge   → region_change: main_village
    Island rect → quest_trigger: s12_old_map (treasure)
    Dock rect   → shop: lake_shop
    5× fishing_zone rects → activate FishingGame

════════════════════════════════════════════════════════════
  REGION: town.tmx   (192×160 tiles)
════════════════════════════════════════════════════════════

  Urban grid layout with brick roads
  Market district (top area): 12× large shop stalls
  Residential area (left): 20× houses, 2-3 storeys implied by overlay
  Industrial area (right): Factory building 8×6, vehicle dealer 6×4
  Police station 5×4 at (150,80)
  Town hall 6×6 at (96,20)
  2× large banyan trees at town square

  TRIGGERS:
    West edge    → region_change: main_village
    South edge   → region_change: highway
    Market rects → shop: town_market (×12 stalls)
    Dealer rect  → shop: vehicle_dealer
    Station rect → quest_trigger: m17_police_station

════════════════════════════════════════════════════════════
  REGION: highway.tmx   (256×64 tiles = 8192×2048 px)
════════════════════════════════════════════════════════════

  Long horizontal strip map
  4-lane highway (asphalt tiles) running full width
  Roadside: petrol bunk, tea stall, 3× milestone markers
  Bus stop zones (×4 evenly spaced) with covered shelters
  Police checkpoint at tile (128,32)
  Gang blockade spawns at (180,30) during m21

  TRIGGERS:
    West edge   → region_change: main_village
    East edge   → region_change: town (or loop)
    North fringe→ region_change: forest
    South fringe→ region_change: farmlands
    Bus stop rects → bus_travel interaction

════════════════════════════════════════════════════════════
  INTERIOR MAPS (small, loaded on door trigger)
════════════════════════════════════════════════════════════

  panchayat_interior.tmx  (24×18 tiles)
    — Desks, filing cabinets, council table
    — Transition trigger at south wall → main_village

  temple_interior.tmx  (30×20 tiles)
    — Idol platform, prayer lamps, pillars
    — Contains temple_idol object (quest item spawn)

  house_interior.tmx  (16×12 tiles)
    — Generic interior used for all NPC houses
    — Bed, shelf, trunk

  school_interior.tmx  (28×18 tiles)
    — Classroom rows, blackboard, teacher's desk

════════════════════════════════════════════════════════════
  OBJECT PROPERTY CONVENTIONS
════════════════════════════════════════════════════════════

  On Collision layer rectangles:
    (no extra properties needed — all are solid)

  On Triggers layer objects:
    type     = "region_change" | "quest_trigger" | "shop" |
               "chest" | "fishing_zone" | "bus_travel" |
               "farm_plot" | "npc_spawn"
    target   = region ID | quest ID | shop ID | item ID
    one_shot = true (trigger fires only once per session)
    requires_flag = game flag that must be set (optional)

  On Objects layer tiles:
    blocked  = true     (marks tile as solid for collision)
    overhead = true     (tile renders on Overhead layer pass)
    water    = true     (boat-accessible, player walks around)

════════════════════════════════════════════════════════════
  RECOMMENDED TILED EXPORT SETTINGS
════════════════════════════════════════════════════════════

  Format:     TMX (XML)
  Encoding:   Base64, zlib compressed
  Tile size:  32 × 32
  Map size:   See per-region specs above
  Output dir: android/assets/maps/

  Tilesets must be embedded (use "Embed in Map" option) OR
  exported as individual TSX files in android/assets/maps/tilesets/

  Ensure all tileset image paths are RELATIVE in the TMX file.
