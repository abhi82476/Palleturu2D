package com.villagelegends.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.CropData;
import com.villagelegends.data.FarmPlot;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.entities.Player;
import com.villagelegends.world.DayNightCycle;

import java.util.*;

/**
 * FarmingManager handles up to MAX_FARM_PLOTS plots.
 *
 * Plot lifecycle:
 *   EMPTY → PLOWED → PLANTED → GROWING → MATURE → HARVESTED
 *
 * Each crop type has:
 *  - growth time (in game hours)
 *  - water requirement (per hour)
 *  - yield range
 *  - sell price at market
 *
 * The player interacts with a plot by walking up to it; the action
 * context button changes depending on plot state.
 *
 * Supported crops: RICE, GROUNDNUT, COTTON, SUGARCANE, VEGETABLES (5 types)
 */
public class FarmingManager {

    private final VillageLegends game;
    private final List<FarmPlot>  plots    = new ArrayList<>();
    private final Map<String, CropData> cropDB = new HashMap<>();

    // ─────────────────────────────────────────────────────────
    public FarmingManager(VillageLegends game) {
        this.game = game;
        buildCropDatabase();
        subscribeToEvents();
    }

    private void buildCropDatabase() {
        // id, displayName, growthHours, waterPerHour, yieldMin, yieldMax, pricePerBag
        addCrop("rice",       "Rice",        48, 0.10f, 8,  15, 45);
        addCrop("groundnut",  "Groundnut",   36, 0.08f, 6,  12, 60);
        addCrop("cotton",     "Cotton",      60, 0.06f, 5,  10, 80);
        addCrop("sugarcane",  "Sugarcane",   72, 0.12f, 10, 20, 35);
        addCrop("tomato",     "Tomatoes",    24, 0.15f, 12, 25, 30);
        addCrop("brinjal",    "Brinjal",     20, 0.12f, 10, 22, 25);
        addCrop("chilli",     "Chilli",      30, 0.09f, 8,  18, 55);
        addCrop("onion",      "Onion",       40, 0.08f, 6,  14, 40);
        addCrop("mango_sapling","Mango",     200,0.05f, 20, 40, 70);
    }

    private void addCrop(String id, String name, int growHours, float waterPerHr,
                         int yMin, int yMax, int price) {
        cropDB.put(id, new CropData(id, name, growHours, waterPerHr, yMin, yMax, price));
    }

    private void subscribeToEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.NEW_DAY, e -> {
            // Pest chance each new day
            rollPestEvents();
        });
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, DayNightCycle dayNight) {
        float hoursDelta = delta * (GameConstants.HOURS_PER_DAY
                         / GameConstants.DAY_DURATION_SECONDS);

        for (FarmPlot plot : plots) {
            if (plot.state == FarmPlot.State.PLANTED || plot.state == FarmPlot.State.GROWING) {
                CropData crop = cropDB.get(plot.cropId);
                if (crop == null) continue;

                // Water drain
                plot.waterLevel -= crop.waterPerHour * hoursDelta;
                plot.waterLevel  = Math.max(0, plot.waterLevel);

                // Crop dies without water
                if (plot.waterLevel <= 0 && plot.state != FarmPlot.State.DEAD) {
                    plot.dehydrationTimer += hoursDelta;
                    if (plot.dehydrationTimer >= 4f) {   // dies after 4 hours dry
                        killCrop(plot);
                    }
                } else {
                    plot.dehydrationTimer = 0;
                }

                // Pest damage
                if (plot.hasPest) {
                    plot.pestDamage += 0.05f * hoursDelta;
                    if (plot.pestDamage >= 1.0f) killCrop(plot);
                }

                // Growth
                if (plot.waterLevel > 0 && !plot.hasPest) {
                    plot.growthHours += hoursDelta;
                    if (plot.growthHours >= crop.growthHours) {
                        plot.state = FarmPlot.State.MATURE;
                    } else {
                        plot.state = FarmPlot.State.GROWING;
                    }
                }
            }
        }
    }

    // ── Player interaction ────────────────────────────────────
    public boolean tryInteractAt(float px, float py, float radius, Player player) {
        for (FarmPlot plot : plots) {
            float dx = plot.worldX - px;
            float dy = plot.worldY - py;
            if (dx * dx + dy * dy <= radius * radius) {
                performFarmAction(plot, player);
                return true;
            }
        }
        return false;
    }

    private void performFarmAction(FarmPlot plot, Player player) {
        switch (plot.state) {
            case EMPTY:
                // Plow the plot if player has a hoe or bare hands
                plot.state = FarmPlot.State.PLOWED;
                game.audioManager.playSfx("plow_soil");
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION, "Plot plowed! Plant a seed."));
                break;

            case PLOWED:
                // Find a seed in player inventory and plant
                String seedId = findSeedInInventory(player);
                if (seedId != null) {
                    String cropId = seedId.replace("_seed", "");
                    if (cropDB.containsKey(cropId)) {
                        player.removeItems(seedId, 1);
                        plantCrop(plot, cropId);
                        game.audioManager.playSfx("plant_seed");
                    }
                } else {
                    game.eventBus.post(new GameEventBus.Event(
                            GameEventBus.EventType.SHOW_NOTIFICATION, "No seeds in inventory!"));
                }
                break;

            case PLANTED:
            case GROWING:
                // Water the crop
                waterCrop(plot, player);
                // Remove pest if player has pesticide
                if (plot.hasPest && player.hasItem("pesticide")) {
                    player.removeItems("pesticide", 1);
                    plot.hasPest    = false;
                    plot.pestDamage = 0;
                    game.audioManager.playSfx("spray");
                    game.eventBus.post(new GameEventBus.Event(
                            GameEventBus.EventType.SHOW_NOTIFICATION, "Pest removed!"));
                }
                break;

            case MATURE:
                harvestCrop(plot, player);
                break;

            case DEAD:
                // Clear dead crop
                plot.reset();
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION, "Dead crop removed."));
                break;

            default:
                break;
        }
    }

    private void plantCrop(FarmPlot plot, String cropId) {
        plot.cropId       = cropId;
        plot.growthHours  = 0;
        plot.waterLevel   = 1.0f;   // full at planting
        plot.state        = FarmPlot.State.PLANTED;
        plot.hasPest      = false;
        plot.pestDamage   = 0;
        game.eventBus.post(GameEventBus.EventType.CROP_PLANTED, cropId);
    }

    private void waterCrop(FarmPlot plot, Player player) {
        boolean hasBucket = player.hasItem("water_bucket") || player.hasItem("watering_can");
        if (!hasBucket) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Need a water bucket!"));
            return;
        }
        plot.waterLevel = Math.min(1.0f, plot.waterLevel + 0.6f);
        plot.dehydrationTimer = 0;
        game.audioManager.playSfx("water_crops");
        game.eventBus.post(GameEventBus.EventType.CROP_WATERED, plot.cropId);
    }

    private void harvestCrop(FarmPlot plot, Player player) {
        CropData crop = cropDB.get(plot.cropId);
        if (crop == null) return;

        int yieldAmt = crop.yieldMin
                + (int)((crop.yieldMax - crop.yieldMin) * plot.getHealthFactor());

        for (int i = 0; i < yieldAmt; i++) {
            com.villagelegends.data.Item bag = new com.villagelegends.data.Item(
                    plot.cropId + "_bag",
                    crop.displayName + " Bag",
                    com.villagelegends.data.Item.Category.CROP,
                    crop.pricePerBag
            );
            if (!player.addItem(bag)) break;   // inventory full
        }

        game.audioManager.playSfx("harvest");
        game.eventBus.post(GameEventBus.EventType.CROP_HARVESTED, plot.cropId);
        game.questManager.advanceObjective("m02_first_harvest", "harvest_groundnut", yieldAmt);

        plot.state = FarmPlot.State.EMPTY;
        plot.cropId = null;
        plot.growthHours = 0;

        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                "Harvested " + yieldAmt + " " + crop.displayName + " bags!"));
    }

    private void killCrop(FarmPlot plot) {
        plot.state = FarmPlot.State.DEAD;
        game.eventBus.post(GameEventBus.EventType.CROP_DIED, plot.cropId);
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                "Your " + plot.cropId + " crop has died!"));
    }

    private void rollPestEvents() {
        Random rng = new Random();
        for (FarmPlot plot : plots) {
            if ((plot.state == FarmPlot.State.GROWING || plot.state == FarmPlot.State.PLANTED)
                    && !plot.hasPest
                    && rng.nextFloat() < GameConstants.CROP_PEST_CHANCE) {
                plot.hasPest = true;
                game.eventBus.post(GameEventBus.EventType.PEST_ATTACK, plot.cropId);
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION,
                        "Pests on your " + plot.cropId + "! Use pesticide."));
            }
        }
    }

    // ── Inventory helpers ─────────────────────────────────────
    private String findSeedInInventory(Player player) {
        for (String cropId : cropDB.keySet()) {
            String seedId = cropId + "_seed";
            if (player.hasItem(seedId)) return seedId;
        }
        return null;
    }

    // ── Plot management ───────────────────────────────────────
    public FarmPlot addPlot(float worldX, float worldY) {
        if (plots.size() >= GameConstants.MAX_FARM_PLOTS) return null;
        FarmPlot p = new FarmPlot(plots.size(), worldX, worldY);
        plots.add(p);
        return p;
    }

    // ── Save / Load ───────────────────────────────────────────
    public void loadFromSave(GameSaveData save) {
        plots.clear();
        if (save.farmPlots != null) plots.addAll(save.farmPlots);
        // If no plots in save, create default farm plots
        if (plots.isEmpty()) createDefaultPlots();
    }

    private void createDefaultPlots() {
        // 6 default plots near farmlands region spawn
        float baseX = 160f, baseY = 160f;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                addPlot(baseX + col * 64f, baseY + row * 64f);
            }
        }
    }

    public void syncToSave(GameSaveData save) {
        save.farmPlots = new ArrayList<>(plots);
    }

    // ── Getters ───────────────────────────────────────────────
    public List<FarmPlot>       getPlots()        { return Collections.unmodifiableList(plots); }
    public CropData             getCropData(String id){ return cropDB.get(id); }
    public Map<String,CropData> getCropDatabase() { return Collections.unmodifiableMap(cropDB); }
}
