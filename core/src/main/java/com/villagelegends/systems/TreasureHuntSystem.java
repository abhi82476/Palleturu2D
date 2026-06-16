package com.villagelegends.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Item;
import com.villagelegends.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * TreasureHuntSystem manages all buried treasure locations.
 *
 * Each TreasureCache has:
 *   - World coordinates (region + x/y)
 *   - Discovery radius (how close player must get to find it)
 *   - A clue string revealed by certain NPCs or items
 *   - Loot table (random selection from predefined items)
 *   - A found flag (persisted in save)
 *
 * The player interacts with a cache by walking near it and pressing
 * the ACTION button. A dig animation plays and items appear.
 *
 * Known treasures (total 8):
 *   T01-T03: Forest region (linked to s12 quest)
 *   T04-T05: Lake island (linked to s13 quest)
 *   T06-T08: Random locations revealed by NPCs
 */
public class TreasureHuntSystem {

    // ── Treasure definition ───────────────────────────────────
    public static class TreasureCache {
        public final String  cacheId;
        public final String  region;
        public final float   worldX, worldY;
        public final float   discoverRadius;
        public final String  clue;            // displayed when player has map fragment
        public final String  questId;         // null if standalone
        public final String  objectiveId;
        public final LootEntry[] loot;

        public boolean discovered = false;
        public boolean dug        = false;

        // Visual state
        public float digAnim  = 0f;
        public boolean showing = false;

        public TreasureCache(String id, String region, float x, float y, float radius,
                             String clue, String questId, String objId, LootEntry... loot) {
            this.cacheId        = id;
            this.region         = region;
            this.worldX         = x;
            this.worldY         = y;
            this.discoverRadius = radius;
            this.clue           = clue;
            this.questId        = questId;
            this.objectiveId    = objId;
            this.loot           = loot;
        }
    }

    // ── Loot entry ────────────────────────────────────────────
    public static class LootEntry {
        public final String  itemId;
        public final String  itemName;
        public final Item.Category category;
        public final int     quantity;
        public final int     value;
        public final float   probability;   // 0..1

        public LootEntry(String id, String name, Item.Category cat,
                         int qty, int val, float prob) {
            this.itemId      = id;
            this.itemName    = name;
            this.category    = cat;
            this.quantity    = qty;
            this.value       = val;
            this.probability = prob;
        }
    }

    // ─────────────────────────────────────────────────────────
    private final VillageLegends     game;
    private final List<TreasureCache> caches = new ArrayList<>();
    private TreasureCache             activeDig = null;
    private float                     digTimer  = 0f;
    private boolean                   showResult = false;
    private String                    resultText = "";
    private float                     resultTimer = 0f;
    private final BitmapFont          font;

    // ─────────────────────────────────────────────────────────
    public TreasureHuntSystem(VillageLegends game) {
        this.game = game;
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        buildTreasureCaches();
    }

    // ── Cache definitions ─────────────────────────────────────
    private void buildTreasureCaches() {
        float T = GameConstants.TILE_SIZE;

        // Forest treasures (s12 quest chain)
        caches.add(new TreasureCache(
            "t01_forest_north", "forest", 30*T, 120*T, 48f,
            "Near the old banyan tree north of the clearing",
            "s12_old_map", "find_ancient_treasure_1",
            loot("groundnut_bag", "Groundnut Bags", Item.Category.CROP,   5,  60, 1.0f),
            loot("petrol_can",    "Petrol Can",      Item.Category.FUEL,   2,  90, 0.7f),
            loot("medicine",      "Medicine",        Item.Category.CONSUMABLE, 1, 150, 0.5f)
        ));

        caches.add(new TreasureCache(
            "t02_forest_ruins", "forest", 42*T, 42*T, 52f,
            "Inside the crumbling east wall of the ancient temple ruins",
            "s12_old_map", "find_ancient_treasure_2",
            loot("cotton_bag",    "Cotton Bags",     Item.Category.CROP,   3,  80, 1.0f),
            loot("slingshot",     "Slingshot",       Item.Category.WEAPON, 1,  85, 0.6f),
            loot("medicine",      "Medicine",        Item.Category.CONSUMABLE, 2, 150, 0.4f)
        ));

        caches.add(new TreasureCache(
            "t03_forest_gold",  "forest", 80*T, 30*T, 40f,
            "Buried at the foot of the tallest coconut palm, 50 steps east",
            "s12_old_map", "dig_final_treasure",
            loot("zamindar_gold","Zamindar's Gold",  Item.Category.MISC,   1, 2000, 1.0f),
            loot("cotton_bag",   "Cotton Bags",      Item.Category.CROP,   8,   80, 1.0f),
            loot("medicine",     "Medicine",         Item.Category.CONSUMABLE, 3, 150, 0.8f)
        ));

        // Lake island (s13 quest)
        caches.add(new TreasureCache(
            "t04_island_east",  "lake", 55*T, 38*T, 44f,
            "Beneath the eastern root cluster of the island's old fig tree",
            "s13_ruins", "explore_temple_ruin",
            loot("ancient_vase",  "Ancient Vase",   Item.Category.MISC, 1, 450, 1.0f),
            loot("groundnut_bag", "Groundnut Bags", Item.Category.CROP, 4,  60, 0.8f)
        ));

        caches.add(new TreasureCache(
            "t05_island_centre","lake", 50*T, 43*T, 44f,
            "Under the central stone platform of the old island temple",
            "s13_ruins", "collect_artefact",
            loot("bronze_idol",   "Bronze Idol",    Item.Category.MISC,  1, 800, 1.0f),
            loot("chilli_bag",    "Chilli Bags",    Item.Category.CROP,  5,  55, 0.9f),
            loot("medicine",      "Medicine",       Item.Category.CONSUMABLE, 2, 150, 0.6f)
        ));

        // Standalone secret treasures (revealed by NPCs)
        caches.add(new TreasureCache(
            "t06_village_well",  "main_village", 58*T, 70*T, 36f,
            "Elder Ramu whispered: Something is hidden near the old well",
            null, null,
            loot("fertiliser",   "Fertiliser",     Item.Category.CONSUMABLE, 3,  55, 1.0f),
            loot("onion_bag",    "Onion Bags",     Item.Category.CROP,       4,  40, 0.9f)
        ));

        caches.add(new TreasureCache(
            "t07_farm_east",     "farmlands", 75*T, 20*T, 40f,
            "The old farmer mentioned something buried under the eastern irrigation channel",
            null, null,
            loot("sugarcane_bag","Sugarcane Bags", Item.Category.CROP,   6,  35, 1.0f),
            loot("watering_can", "Watering Can",   Item.Category.TOOL,   1,  80, 0.7f)
        ));

        caches.add(new TreasureCache(
            "t08_highway_edge",  "highway", 200*T, 8*T, 40f,
            "A trucker hid emergency supplies behind kilometre stone 12",
            null, null,
            loot("petrol_can",   "Petrol Can",     Item.Category.FUEL,       3,  90, 1.0f),
            loot("medicine",     "Medicine",       Item.Category.CONSUMABLE, 2, 150, 0.8f),
            loot("fishing_bait", "Fishing Bait",   Item.Category.TOOL,       5,  10, 0.6f)
        ));
    }

    private LootEntry loot(String id, String name, Item.Category cat,
                           int qty, int val, float prob) {
        return new LootEntry(id, name, cat, qty, val, prob);
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, Player player, String region) {
        resultTimer = Math.max(0, resultTimer - delta);

        // Active dig animation
        if (activeDig != null) {
            digTimer += delta;
            if (digTimer >= 1.5f) {
                finishDig(player);
                activeDig = null;
                digTimer  = 0f;
            }
            return;
        }

        // Proximity discovery
        for (TreasureCache cache : caches) {
            if (cache.dug || !cache.region.equals(region)) continue;

            float dx = player.centerX() - cache.worldX;
            float dy = player.centerY() - cache.worldY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= cache.discoverRadius && !cache.discovered) {
                cache.discovered = true;
                cache.showing    = true;
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION,
                        "Something is buried here! Press ACTION to dig."));
                game.audioManager.playSfx("ui_click");
            }
        }
    }

    public void tryDig(Player player, String region) {
        for (TreasureCache cache : caches) {
            if (cache.dug || !cache.region.equals(region) || !cache.discovered) continue;

            float dx = player.centerX() - cache.worldX;
            float dy = player.centerY() - cache.worldY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= cache.discoverRadius) {
                activeDig = cache;
                digTimer  = 0f;
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION, "Digging…"));
                game.audioManager.playSfx("plow_soil");
                return;
            }
        }
    }

    private void finishDig(Player player) {
        if (activeDig == null) return;
        activeDig.dug     = true;
        activeDig.showing = false;

        StringBuilder found = new StringBuilder("Found: ");
        for (LootEntry entry : activeDig.loot) {
            if (MathUtils.random() <= entry.probability) {
                for (int i = 0; i < entry.quantity; i++) {
                    Item item = new Item(entry.itemId, entry.itemName,
                                        entry.category, entry.value);
                    player.addItem(item);
                }
                found.append(entry.quantity).append("× ").append(entry.itemName).append("  ");
            }
        }

        resultText  = found.toString().trim();
        resultTimer = 3.5f;
        showResult  = true;

        // Quest progress
        if (activeDig.questId != null && activeDig.objectiveId != null) {
            game.questManager.advanceObjective(
                    activeDig.questId, activeDig.objectiveId, 1);
        }

        // Track total found
        if (game.activeSave != null) {
            int prev = game.activeSave.miniGameScores.getOrDefault("treasure_found", 0);
            game.activeSave.miniGameScores.put("treasure_found", prev + 1);
        }

        player.changeReputation(8);
        game.audioManager.playSfx("chest_open");
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION, resultText));
    }

    // ── Draw overlays ─────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta,
                     com.badlogic.gdx.graphics.OrthographicCamera gameCamera,
                     String currentRegion) {
        // Draw dig-spot markers in world space
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(gameCamera.combined);
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (TreasureCache cache : caches) {
            if (cache.dug || !cache.region.equals(currentRegion) || !cache.discovered) continue;
            float pulse = 0.5f + 0.5f * MathUtils.sin((float) System.currentTimeMillis() * 0.004f);
            sr.setColor(0.9f, 0.7f, 0.1f, 0.4f * pulse);
            sr.circle(cache.worldX, cache.worldY, cache.discoverRadius * 0.8f, 20);
        }

        // Dig animation
        if (activeDig != null) {
            float progress = digTimer / 1.5f;
            sr.setColor(0.6f, 0.4f, 0.2f, 0.9f);
            sr.circle(activeDig.worldX, activeDig.worldY, 20 * progress, 16);
        }
        sr.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();
    }

    // ── Getters ───────────────────────────────────────────────
    public List<TreasureCache> getCaches()      { return caches; }
    public boolean             isDigging()      { return activeDig != null; }
    public int                 getTotalFound()  {
        int n = 0;
        for (TreasureCache c : caches) if (c.dug) n++;
        return n;
    }

    public void dispose() { font.dispose(); }
}
