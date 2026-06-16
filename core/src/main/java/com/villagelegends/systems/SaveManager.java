package com.villagelegends.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.data.PlayerData;

/**
 * SaveManager serialises/deserialises the full GameSaveData to
 * LibGDX Preferences (backed by SharedPreferences on Android).
 *
 * Three save slots (0-2).  Auto-save fires every 5 game-minutes.
 * Cloud sync is triggered via the EventBus (game.eventBus post
 * GAME_SAVED) – the Android back-end can listen and upload.
 */
public class SaveManager {

    private static final String PREF_FILE   = "vl2d_saves";
    private static final String SLOT_KEY    = "slot_";
    private static final String META_KEY    = "meta_";     // quick info (name/time)
    private static final float  AUTO_SAVE_INTERVAL = 300f; // real-seconds (5 min)

    private final VillageLegends game;
    private final Preferences    prefs;
    private final Json           json;
    private float autoSaveTimer = 0f;

    // ─────────────────────────────────────────────────────────
    public SaveManager(VillageLegends game) {
        this.game  = game;
        this.prefs = Gdx.app.getPreferences(PREF_FILE);
        this.json  = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
    }

    // ── Auto-save tick ────────────────────────────────────────
    public void tick(float delta) {
        autoSaveTimer += delta;
        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0;
            if (game.activeSave != null) saveGame(game.activeSave);
        }
    }

    // ── New game ──────────────────────────────────────────────
    public GameSaveData createNewSave(int slot) {
        GameSaveData save = new GameSaveData();
        save.slot           = slot;
        save.saveName       = "Save " + (slot + 1);
        save.gameHour       = GameConstants.GAME_START_HOUR;
        save.totalPlayTime  = 0;
        save.playerData     = new PlayerData();
        save.playerData.worldX          = 400;
        save.playerData.worldY          = 300;
        save.playerData.currentRegion   = GameConstants.REGION_MAIN_VILLAGE;
        save.playerData.money           = GameConstants.STARTING_MONEY;
        save.playerData.reputation      = 0;
        save.playerData.health          = GameConstants.PLAYER_HEALTH_MAX;

        // Starter inventory
        com.villagelegends.data.Item hoe  = new com.villagelegends.data.Item(
                "hoe", "Farmer's Hoe",
                com.villagelegends.data.Item.Category.TOOL, 60);
        com.villagelegends.data.Item rod  = new com.villagelegends.data.Item(
                "fishing_rod", "Fishing Rod",
                com.villagelegends.data.Item.Category.TOOL, 120);
        com.villagelegends.data.Item seed = new com.villagelegends.data.Item(
                "groundnut_seed", "Groundnut Seeds",
                com.villagelegends.data.Item.Category.SEED, 25);
        save.playerData.inventoryItems.add(hoe);
        save.playerData.inventoryItems.add(rod);
        save.playerData.inventoryItems.add(seed);

        saveGame(save);
        return save;
    }

    // ── Save ──────────────────────────────────────────────────
    public void saveGame(GameSaveData save) {
        // Sync sub-managers into save before writing
        if (game.farmingManager != null)  game.farmingManager.syncToSave(save);
        if (game.npcManager     != null)  game.npcManager.syncToSave(save);
        if (game.festivalManager != null) game.festivalManager.syncToSave(save);
        if (game.economyManager  != null) game.economyManager.syncToSave(save.playerData);

        save.lastSaveTimestamp = System.currentTimeMillis();

        try {
            String jsonStr = json.toJson(save, GameSaveData.class);
            prefs.putString(SLOT_KEY + save.slot, jsonStr);
            // Write a compact metadata entry for the menu slot preview
            String meta = save.saveName + "|" + formatTime(save.totalPlayTime)
                        + "|Day " + save.playerData.day
                        + "|₹" + save.playerData.money;
            prefs.putString(META_KEY + save.slot, meta);
            prefs.flush();
            game.eventBus.post(GameEventBus.EventType.GAME_SAVED, save.slot);
            Gdx.app.log("SaveManager", "Saved slot " + save.slot);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Save failed: " + e.getMessage());
        }
    }

    // ── Load ──────────────────────────────────────────────────
    public GameSaveData loadGame(int slot) {
        String jsonStr = prefs.getString(SLOT_KEY + slot, null);
        if (jsonStr == null || jsonStr.isEmpty()) {
            Gdx.app.log("SaveManager", "No save in slot " + slot);
            return null;
        }
        try {
            GameSaveData save = json.fromJson(GameSaveData.class, jsonStr);
            game.eventBus.post(GameEventBus.EventType.GAME_LOADED, slot);
            Gdx.app.log("SaveManager", "Loaded slot " + slot);
            return save;
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Load failed: " + e.getMessage());
            return null;
        }
    }

    // ── Slot query ────────────────────────────────────────────
    public boolean slotHasSave(int slot) {
        String s = prefs.getString(SLOT_KEY + slot, null);
        return s != null && !s.isEmpty();
    }

    public String getSlotMeta(int slot) {
        return prefs.getString(META_KEY + slot, "Empty Slot");
    }

    public void deleteSlot(int slot) {
        prefs.remove(SLOT_KEY + slot);
        prefs.remove(META_KEY + slot);
        prefs.flush();
    }

    // ── Helpers ───────────────────────────────────────────────
    private String formatTime(float seconds) {
        int h = (int)(seconds / 3600);
        int m = (int)((seconds % 3600) / 60);
        return h + "h " + m + "m";
    }
}
