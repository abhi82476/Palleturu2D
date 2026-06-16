# Village Legends 2D — Sprite & Art Asset Requirements
# Format: all sprites packed into TexturePacker atlases
# Style:  16×16 or 32×32 pixel art, bright South Indian colour palette

════════════════════════════════════════════════════════════
  ATLAS 1: characters.atlas
════════════════════════════════════════════════════════════

PLAYER CHARACTER (male villager, 16×24px per frame)
  Idle:     4 frames × 4 directions = 16 frames
  Walk:     6 frames × 4 directions = 24 frames
  Run:      6 frames × 4 directions = 24 frames
  Attack:   4 frames × 4 directions = 16 frames  (stick swing)
  Dodge:    3 frames × 4 directions = 12 frames
  Farming:  4 frames (hoeing, watering, harvesting, carrying)
  Fishing:  4 frames (cast, wait, reel, carry)
  Riding:   2 frames × 4 directions = 8 frames
  Hurt:     2 frames
  Victory:  3 frames

NPC TYPES (12×20px per frame, simpler animation)
  Farmer:      Idle 2f, Walk 4f (×4 dirs), Work 4f
  Merchant:    Idle 2f, Walk 4f, Sell 3f
  Student:     Idle 2f, Walk 4f, Study 3f
  Elder:       Idle 3f, Walk 3f, Talk 4f
  Guard:       Idle 2f, Walk 4f, Alert 2f
  Gang Thug:   Idle 2f, Walk 4f, Attack 4f, Hurt 2f
  Smuggler:    Idle 2f, Walk 4f, Attack 4f
  Official:    Idle 2f, Walk 3f, Talk 3f
  Fisherman:   Idle 2f, Walk 3f, Fish 4f
  Priest:      Idle 3f, Walk 3f, Pray 4f
  Teacher:     Idle 2f, Walk 3f, Teach 4f
  Bus Driver:  Idle 2f, Walk 3f
  Generic Villager: Idle 2f, Walk 4f (×4 variants: male/female × 2 skin tones)

BOSS CHARACTERS (24×32px, more detailed)
  Vikram Reddy:   Idle 3f, Walk 4f, Attack 5f, Phase2 2f
  Narayana Rao:   Idle 3f, Walk 4f, Attack 5f, Enrage 3f, Defeat 4f

════════════════════════════════════════════════════════════
  ATLAS 2: vehicles.atlas
════════════════════════════════════════════════════════════

Each vehicle has 8-direction sprites and animated wheels/propeller
  BICYCLE      (20×32px): 8 directions × 2 frames = 16 frames
  MOTORCYCLE   (24×40px): 8 directions × 3 frames = 24 frames + exhaust 3f
  TRACTOR      (40×52px): 4 directions × 2 frames + plow 2f
  BULLOCK CART (36×44px): 4 directions × 2 frames + bull walk 4f
  BOAT         (32×48px): 8 directions × 3 frames (water wake 3f separate)
  BUS          (48×80px): 4 directions × 2 frames

════════════════════════════════════════════════════════════
  ATLAS 3: tiles.atlas   (tileset exported separately as TSX)
════════════════════════════════════════════════════════════

Ground tiles (32×32):
  dry_soil, fertile_soil, grass_short, grass_long (4 variants)
  sand, mud, stone_path, brick_road, highway, dirt_road
  puddle_01, puddle_02

Water tiles (32×32, 3-frame animation at 0.3s):
  deep_water_00..02, shallow_water_00..02
  riverbank_top, riverbank_left, riverbank_right, riverbank_bottom
  waterfall_top (3f), waterfall_mid (3f), waterfall_base (3f)

Building tiles (32×32 components, assembled into multi-tile objects):
  wall_stone, wall_mud, wall_brick (3 variants each)
  roof_red (terracotta), roof_blue (mangalore), roof_thatch
  door_wood, door_metal, window_open, window_closed
  temple_pillar, temple_gopuram_tile (5 ornate variants)
  market_awning_red, market_awning_blue, market_awning_green
  school_blackboard, panchayat_notice
  police_sign, shop_sign (4 variants)

Farm tiles (32×32):
  farm_empty, farm_plowed, farm_waterlogged
  rice_01..05 (growth stages), groundnut_01..05, cotton_01..05
  sugarcane_01..05, tomato_01..05, brinjal_01..05
  chilli_01..05, onion_01..05
  dead_crop (brown wilted)

Nature tiles (32×32, multi-tile objects where noted):
  coconut_trunk, coconut_canopy (2×2 overhead)
  mango_trunk, mango_canopy (3×3 overhead)
  banyan_trunk, banyan_aerial_root, banyan_canopy (4×4 overhead)
  bush_01..04 (4 colours), flower_01..06
  tall_grass_00..01 (2f anim), reeds_01..03
  rock_small, rock_medium, rock_large (2×2)
  fence_horiz, fence_vert, fence_corner

════════════════════════════════════════════════════════════
  ATLAS 4: ui.atlas
════════════════════════════════════════════════════════════

HUD elements:
  heart_full, heart_half, heart_empty (16×16)
  stamina_bar_bg, stamina_bar_fill (192×12)
  money_icon (coin, 16×16)
  reputation_star (16×16, 5 variants: red→gold)
  minimap_bg, minimap_border (160×160)
  minimap_player_dot (8×8), minimap_npc_dot (6×6), minimap_enemy_dot (6×6)
  compass_rose (32×32)

Mobile controls:
  joystick_bg (outer ring, 160×160, semi-transparent)
  joystick_knob (72×72)
  btn_attack (84×84)
  btn_action (84×84)
  btn_sprint (84×84)
  btn_dodge (84×84)
  btn_inventory (64×64)
  btn_map (64×64)
  btn_pause (64×64)
  btn_quest (64×64)

Inventory UI:
  slot_bg (70×70), slot_selected (70×70), slot_hover (70×70)
  item_category_seed (12×12 icon)
  item_category_crop, tool, weapon, consumable, quest, fish, fuel (12×12 each)

Dialogue UI:
  dialogue_box_bg (1240×220, 9-slice)
  dialogue_choice_btn (560×44, 9-slice)
  dialogue_portrait_frame (80×80)
  speech_bubble_arrow (24×16)

Menu screens:
  main_menu_bg (1280×720, parallax scroll — 3 layers)
  menu_btn_normal (300×60, 9-slice)
  menu_btn_hover (300×60)
  menu_btn_pressed (300×60)
  logo_village_legends (400×120)
  settings_panel (600×500, 9-slice)
  save_slot_bg (360×80, 9-slice)

Quest & notification:
  quest_marker_yellow (exclamation, 20×28)
  quest_marker_complete (checkmark, 20×28)
  quest_log_bg (800×600, 9-slice)
  notification_bg (480×44, 9-slice)

════════════════════════════════════════════════════════════
  ATLAS 5: effects.atlas
════════════════════════════════════════════════════════════

Combat:
  hit_spark (5 frames, 24×24)
  knockout_stars (6 frames, 32×32)
  dust_cloud (5 frames, 28×28)
  dodge_trail (4 frames, 32×16)
  block_shield (4 frames, 32×32)
  sling_projectile (8×8, spinning 4f)

Farming:
  water_splash (4 frames, 20×20)
  plant_grow (5 frames, 16×24)
  harvest_sparkle (5 frames, 24×24)
  pest_bug (4 frames, 12×12, animated crawl)
  pesticide_spray (4 frames, 32×20)

Weather:
  raindrop_streak (4×12px)
  fog_patch (128×128, 3 frames)
  dust_mote (8×8, 3 frames)

Festival:
  confetti_piece (6×8, 6 colour variants)
  firework_burst (8 frames, 64×64, 4 colour sets)
  lamp_diya (16×16, 3-frame flicker)
  rangoli_pattern (64×64, 4 Sankranti patterns)
  kite (24×16, 4 colours)
  kite_string (dotted line, procedural)

Fishing:
  bobber (12×16, 3-frame bob animation)
  fish_jump (5 frames, 32×24)
  water_ripple (4 frames, 32×32)
  fishing_line (dotted, procedural)

Vehicle:
  engine_exhaust (4 frames, 16×16)
  dust_trail (4 frames, 24×12)
  water_wake (4 frames, 32×16, boats only)
  tyre_marks (decal, 8×32)

════════════════════════════════════════════════════════════
  ATLAS 6: items.atlas
════════════════════════════════════════════════════════════

All item icons (24×24px):
  Seeds: rice_seed, groundnut_seed, cotton_seed, sugarcane_seed,
         tomato_seed, brinjal_seed, chilli_seed, onion_seed
  Bags:  rice_bag, groundnut_bag, cotton_bag, sugarcane_bag,
         tomato_bag, brinjal_bag, chilli_bag, onion_bag
  Tools: hoe, sickle, watering_can, water_bucket, fishing_rod,
         fishing_bait, pesticide, fertiliser
  Weapons: stick (lathi), slingshot, farming_hoe
  Consumables: medicine, jaggery
  Fuel: petrol_can, bullock_fodder
  Fish: rohu_fish, catla_fish, tilapia_fish, prawn, murrel_fish,
        legendary_catfish (32×24 — larger)
  Quest: bribe_ledger, evidence_crate, temple_idol, treasure_map,
         poison_sample, ancient_artefact
  Misc: milk_can, coin_bundle, key_iron

════════════════════════════════════════════════════════════
  FONT REQUIREMENTS
════════════════════════════════════════════════════════════

  NotoSansTelugu-Regular.ttf  — Telugu text (Unicode Telugu block U+0C00–U+0C7F)
  NotoSans-Regular.ttf        — English/Hindi body text
  PixelFont-Bold.ttf          — Title and HUD headings (pixel-art style)

  Render at sizes: 12, 16, 20, 24, 32, 48 pt
  Export with padding = 2, spread = 4 (for signed distance field if needed)

════════════════════════════════════════════════════════════
  COLOUR PALETTE (South Indian Village)
════════════════════════════════════════════════════════════

  Primary colours:
    Terracotta Red   #C0522A   (roof tiles, temple)
    Mango Yellow     #F5B800   (festivals, UI accents)
    Paddy Green      #5A8A32   (farm fields, nature)
    Coconut Brown    #8B5A2B   (tree trunks, mud walls)
    Sky Blue         #7EC8E3   (day sky, water)
    Night Navy       #1A1A3A   (night sky, deep water)
    Sari Magenta     #C0185C   (female NPC clothing)
    Dhoti White      #F0EDE0   (male NPC clothing)
    Gold Accent      #D4A017   (temple, festival, HUD gold)
    Soil Brown       #9B7653   (dry farmland)

  UI colours:
    Panel Dark       #0A0F14   (dialogue box background)
    Panel Border     #3A6B4A   (inventory cell border)
    Highlight Cyan   #00E5FF   (quest text, selected)
    Health Red       #E53935
    Stamina Blue     #42A5F5
    Money Gold       #FFD700

════════════════════════════════════════════════════════════
  TEXTURE PACKER SETTINGS (TexturePacker 6+)
════════════════════════════════════════════════════════════

  Algorithm:       MaxRects
  Max size:        2048×2048 per atlas
  Pixel format:    RGBA8888
  Filter min:      Nearest  (preserves pixel-art crispness)
  Filter mag:      Nearest
  Pot:             true (power-of-two sizes)
  Strip whitespace: true
  Padding:         2px
  Bleed:           true

  Output: android/assets/textures/{atlas_name}.atlas + .png
