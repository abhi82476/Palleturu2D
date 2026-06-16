package com.villagelegends;

public final class GameConstants {

    private GameConstants() {}

    // ─── Screen ───────────────────────────────────────────────
    public static final int VIRTUAL_WIDTH    = 1280;
    public static final int VIRTUAL_HEIGHT   = 720;
    public static final float ASPECT_RATIO   = (float) VIRTUAL_WIDTH / VIRTUAL_HEIGHT;

    // ─── World / Tiles ────────────────────────────────────────
    public static final float TILE_SIZE          = 32f;
    public static final int   WORLD_WIDTH_TILES  = 256;
    public static final int   WORLD_HEIGHT_TILES = 256;
    public static final float UNIT_SCALE         = TILE_SIZE / 32f;

    // ─── Camera ───────────────────────────────────────────────
    public static final float CAMERA_ZOOM_DEFAULT = 1.0f;
    public static final float CAMERA_ZOOM_MIN     = 0.5f;
    public static final float CAMERA_ZOOM_MAX     = 2.0f;
    public static final float CAMERA_LERP_SPEED   = 5.0f;

    // ─── Player ───────────────────────────────────────────────
    public static final float PLAYER_SPEED           = 120f;
    public static final float PLAYER_RUN_SPEED       = 190f;
    public static final float PLAYER_SPRINT_BOOST    = 1.7f;
    public static final float PLAYER_STAMINA_MAX     = 100f;
    public static final float PLAYER_STAMINA_DRAIN   = 20f;
    public static final float PLAYER_STAMINA_REGEN   = 10f;
    public static final int   PLAYER_HEALTH_MAX      = 100;
    public static final float INVINCIBILITY_TIME     = 1.5f;
    public static final float INTERACTION_RADIUS     = 48f;
    public static final float ATTACK_RANGE           = 40f;
    public static final float ATTACK_COOLDOWN        = 0.4f;

    // ─── Economy ──────────────────────────────────────────────
    public static final int   STARTING_MONEY         = 500;
    public static final float PRICE_FLUCTUATION      = 0.15f;   // ±15%
    public static final int   MAX_INVENTORY_SLOTS    = 40;
    public static final int   MAX_HOTBAR_SLOTS        = 8;

    // ─── Time & Day/Night ─────────────────────────────────────
    /** Real seconds for one in-game day (5 min = 1 day) */
    public static final float DAY_DURATION_SECONDS   = 300f;
    public static final int   HOURS_PER_DAY          = 24;
    public static final float GAME_START_HOUR        = 6f;      // 6 AM
    public static final float SUNRISE_HOUR           = 6f;
    public static final float SUNSET_HOUR            = 18f;
    public static final float TWILIGHT_DURATION      = 1.0f;    // in game hours

    // ─── Farming ──────────────────────────────────────────────
    public static final float CROP_WATER_DRAIN       = 0.08f;   // per game hour
    public static final float CROP_PEST_CHANCE       = 0.05f;   // per harvest cycle
    public static final int   MAX_FARM_PLOTS         = 20;

    // ─── Combat ───────────────────────────────────────────────
    public static final int   STICK_DAMAGE           = 12;
    public static final int   SLING_DAMAGE           = 8;
    public static final int   TOOL_DAMAGE            = 10;
    public static final float DODGE_WINDOW           = 0.25f;
    public static final float BLOCK_DAMAGE_REDUCTION = 0.6f;

    // ─── NPC ──────────────────────────────────────────────────
    public static final float NPC_WALK_SPEED         = 60f;
    public static final float NPC_DETECT_RADIUS      = 80f;
    public static final float NPC_INTERACT_RADIUS    = 50f;
    public static final int   NPC_SCHEDULE_SLOTS     = 6;

    // ─── Vehicles ─────────────────────────────────────────────
    public static final float BICYCLE_SPEED          = 180f;
    public static final float MOTORCYCLE_SPEED       = 320f;
    public static final float TRACTOR_SPEED          = 100f;
    public static final float BULLOCK_CART_SPEED     = 80f;
    public static final float BOAT_SPEED             = 140f;
    public static final float BUS_SPEED              = 260f;
    public static final float VEHICLE_FUEL_MAX       = 100f;
    public static final float VEHICLE_FUEL_BURN      = 0.5f;    // per tile

    // ─── Reputation ───────────────────────────────────────────
    public static final int   REP_VILLAIN_MAX        = -100;
    public static final int   REP_NEUTRAL            = 0;
    public static final int   REP_HERO_MIN           = 100;
    public static final int   REP_LEGEND             = 300;

    // ─── Quest Flags ──────────────────────────────────────────
    public static final String FLAG_INTRO_DONE       = "intro_done";
    public static final String FLAG_GANG_KNOWN       = "gang_known";
    public static final String FLAG_FIRST_ARREST     = "first_arrest";
    public static final String FLAG_TEMPLE_SAVED     = "temple_saved";
    public static final String FLAG_CHIEF_EXPOSED    = "chief_exposed";
    public static final String FLAG_GAME_COMPLETE    = "game_complete";

    // ─── Layer Indices ────────────────────────────────────────
    public static final int LAYER_GROUND             = 0;
    public static final int LAYER_WATER              = 1;
    public static final int LAYER_DECOR              = 2;
    public static final int LAYER_OBJECTS            = 3;
    public static final int LAYER_COLLISION          = 4;
    public static final int LAYER_ENTITIES           = 5;
    public static final int LAYER_OVERHEAD           = 6;
    public static final int LAYER_UI                 = 7;

    // ─── Region IDs ───────────────────────────────────────────
    public static final String REGION_MAIN_VILLAGE   = "main_village";
    public static final String REGION_FOREST         = "forest";
    public static final String REGION_LAKE           = "lake";
    public static final String REGION_TOWN           = "town";
    public static final String REGION_HIGHWAY        = "highway";
    public static final String REGION_FARMLANDS      = "farmlands";
    public static final String REGION_PANCHAYAT      = "panchayat";

    // ─── Save Slots ───────────────────────────────────────────
    public static final int    SAVE_SLOTS            = 3;
    public static final String SAVE_PREFS_KEY        = "vl2d_save_";

    // ─── Audio ────────────────────────────────────────────────
    public static final float MUSIC_VOL_DEFAULT      = 0.7f;
    public static final float SFX_VOL_DEFAULT        = 1.0f;
}
