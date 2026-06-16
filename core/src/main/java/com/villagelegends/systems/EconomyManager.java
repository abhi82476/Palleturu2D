package com.villagelegends.systems;

import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.data.Item;
import com.villagelegends.data.PlayerData;
import com.villagelegends.entities.Player;

import java.util.*;

/**
 * EconomyManager tracks the village economy:
 *  - Item base prices with daily fluctuation (±15%)
 *  - Shop inventories per region/vendor
 *  - Buy / sell transactions including tax and reputation bonuses
 *  - Vehicle dealer listings
 *  - Loan / debt system (optional late-game feature)
 *
 * Prices shift each in-game day based on simple supply/demand model.
 */
public class EconomyManager {

    private final VillageLegends game;

    // ── Price table: itemId → current price ───────────────────
    private final Map<String, Integer> prices     = new HashMap<>();
    private final Map<String, Integer> basePrices = new HashMap<>();

    // ── Shop inventories: shopId → list of available items ────
    private final Map<String, List<Item>> shopInventories = new HashMap<>();

    // ── Vehicle dealer: vehicleId → price ─────────────────────
    private final Map<String, Integer> vehiclePrices = new HashMap<>();

    // ── Player-owned vehicles ─────────────────────────────────
    private final Set<String> ownedVehicleIds = new HashSet<>();

    // ── Daily price drift ─────────────────────────────────────
    private float driftTimer = 0f;

    // ─────────────────────────────────────────────────────────
    public EconomyManager(VillageLegends game) {
        this.game = game;
        buildPriceTable();
        buildShopInventories();
        buildVehicleDealership();
        subscribeToDayChange();
    }

    // ── Price database ────────────────────────────────────────
    private void buildPriceTable() {
        // Seeds
        setPrice("rice_seed",      20);
        setPrice("groundnut_seed", 25);
        setPrice("cotton_seed",    35);
        setPrice("sugarcane_seed", 15);
        setPrice("tomato_seed",    18);
        setPrice("brinjal_seed",   16);
        setPrice("chilli_seed",    22);
        setPrice("onion_seed",     18);

        // Harvested produce
        setPrice("rice_bag",       45);
        setPrice("groundnut_bag",  60);
        setPrice("cotton_bag",     80);
        setPrice("sugarcane_bag",  35);
        setPrice("tomato_bag",     30);
        setPrice("brinjal_bag",    25);
        setPrice("chilli_bag",     55);
        setPrice("onion_bag",      40);
        setPrice("any_fish",       25);
        setPrice("legendary_catfish", 500);

        // Tools & consumables
        setPrice("watering_can",   80);
        setPrice("water_bucket",   30);
        setPrice("pesticide",      45);
        setPrice("fertiliser",     55);
        setPrice("hoe",            60);
        setPrice("sickle",         75);
        setPrice("fishing_rod",    120);
        setPrice("fishing_bait",   10);

        // Weapons (non-lethal)
        setPrice("stick",          20);
        setPrice("slingshot",      85);
        setPrice("farming_hoe",    60);

        // Fuel
        setPrice("petrol_can",     90);
        setPrice("bullock_fodder", 40);

        // Special items
        setPrice("medicine",       150);
        setPrice("milk_can",       30);
        setPrice("jaggery",        20);
        setPrice("bribe_ledger",   0);    // quest item, not sold
        setPrice("evidence_crate", 0);
    }

    private void setPrice(String id, int price) {
        basePrices.put(id, price);
        prices.put(id, price);
    }

    // ── Shop inventories ──────────────────────────────────────
    private void buildShopInventories() {
        // Village general store
        buildShop("village_general",
            "rice_seed","groundnut_seed","tomato_seed","brinjal_seed","onion_seed",
            "watering_can","water_bucket","pesticide","fertiliser","hoe","sickle",
            "stick","fishing_rod","fishing_bait","petrol_can","medicine");

        // Town market (larger selection)
        buildShop("town_market",
            "rice_seed","groundnut_seed","cotton_seed","sugarcane_seed","chilli_seed",
            "watering_can","water_bucket","pesticide","fertiliser","hoe","sickle",
            "stick","slingshot","farming_hoe","fishing_rod","fishing_bait",
            "petrol_can","bullock_fodder","medicine","jaggery");

        // Fishing supplies shop at lake
        buildShop("lake_shop",
            "fishing_rod","fishing_bait","water_bucket","medicine","petrol_can");
    }

    private void buildShop(String shopId, String... itemIds) {
        List<Item> inv = new ArrayList<>();
        for (String id : itemIds) {
            Item i = Item.createShopItem(id, prices.getOrDefault(id, 10));
            inv.add(i);
        }
        shopInventories.put(shopId, inv);
    }

    // ── Vehicle dealership ────────────────────────────────────
    private void buildVehicleDealership() {
        vehiclePrices.put("bicycle",      800);
        vehiclePrices.put("motorcycle",   8500);
        vehiclePrices.put("tractor",      22000);
        vehiclePrices.put("bullock_cart", 5000);
        vehiclePrices.put("boat",         12000);
        vehiclePrices.put("bus",          75000);  // story-locked
    }

    // ── Daily drift ───────────────────────────────────────────
    private void subscribeToDayChange() {
        game.eventBus.subscribe(GameEventBus.EventType.NEW_DAY, e -> applyDailyPriceDrift());
    }

    private void applyDailyPriceDrift() {
        Random rng = new Random();
        for (String id : prices.keySet()) {
            int base = basePrices.get(id);
            float drift = 1f + (rng.nextFloat() * 2f - 1f) * GameConstants.PRICE_FLUCTUATION;
            prices.put(id, Math.max(1, (int)(base * drift)));
        }
        // Sync shop inventories with new prices
        for (List<Item> inv : shopInventories.values()) {
            for (Item item : inv) {
                if (prices.containsKey(item.id)) {
                    item.price = prices.get(item.id);
                }
            }
        }
    }

    public void update(float delta) {
        driftTimer += delta;
        // Slow intra-day micro drift (barely noticeable)
        if (driftTimer >= 30f) {
            driftTimer = 0;
            Random rng = new Random();
            for (String id : prices.keySet()) {
                int base = basePrices.get(id);
                float micro = 1f + (rng.nextFloat() * 0.04f - 0.02f);
                prices.put(id, Math.max(1, (int)(prices.get(id) * micro)));
            }
        }
    }

    // ── Transactions ──────────────────────────────────────────
    /**
     * Player buys item from a shop.
     * @return true if transaction succeeds
     */
    public boolean buyItem(String shopId, String itemId,
                           com.villagelegends.entities.Player player) {
        if (!isMarketOpen()) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Market is closed!"));
            return false;
        }
        int cost = getBuyPrice(itemId, player.getReputation());
        if (!player.spendMoney(cost)) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Not enough money!"));
            return false;
        }
        Item item = Item.createShopItem(itemId, cost);
        player.addItem(item);
        game.eventBus.post(GameEventBus.EventType.PURCHASE_MADE, itemId);
        game.audioManager.playSfx("coin_jingle");
        return true;
    }

    /**
     * Player sells item from inventory.
     */
    public boolean sellItem(String itemId, com.villagelegends.entities.Player player) {
        if (!player.hasItem(itemId)) return false;
        if (!isMarketOpen()) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Market is closed!"));
            return false;
        }
        int earnAmt = getSellPrice(itemId, player.getReputation());
        player.removeItems(itemId, 1);
        player.addMoney(earnAmt);
        game.eventBus.post(GameEventBus.EventType.SALE_MADE, itemId);
        game.questManager.advanceObjective("s01_rice_harvest", "sell_rice_market", 1);
        game.questManager.advanceObjective("m02_first_harvest", "sell_market_stall", 1);
        game.audioManager.playSfx("coin_jingle");
        return true;
    }

    /**
     * Player buys a vehicle from the dealership.
     */
    public boolean buyVehicle(String vehicleTypeId, com.villagelegends.entities.Player player) {
        int cost = vehiclePrices.getOrDefault(vehicleTypeId, 999999);
        if (ownedVehicleIds.contains(vehicleTypeId)) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "You already own this vehicle!"));
            return false;
        }
        if (!player.spendMoney(cost)) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Need ₹" + cost + " to buy this vehicle."));
            return false;
        }
        ownedVehicleIds.add(vehicleTypeId);
        game.vehicleManager.spawnPlayerVehicle(vehicleTypeId, player.getX(), player.getY() - 80);
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                "Bought " + vehicleTypeId + "! Check outside."));
        return true;
    }

    // ── Pricing helpers ───────────────────────────────────────
    /** Buy price: base × 1.2 minus reputation discount. */
    public int getBuyPrice(String itemId, int reputation) {
        int base = prices.getOrDefault(itemId, 0);
        float repDiscount = Math.min(0.20f, reputation * 0.0005f);
        return Math.max(1, (int)(base * 1.2f * (1f - repDiscount)));
    }

    /** Sell price: base × 0.8 plus reputation bonus. */
    public int getSellPrice(String itemId, int reputation) {
        int base = prices.getOrDefault(itemId, 0);
        float repBonus = Math.min(0.15f, reputation * 0.0003f);
        return Math.max(1, (int)(base * 0.8f * (1f + repBonus)));
    }

    // ── Save / Load ───────────────────────────────────────────
    public void loadFromSave(PlayerData pd) {
        ownedVehicleIds.clear();
        if (pd.ownedVehicleIds != null) ownedVehicleIds.addAll(pd.ownedVehicleIds);
    }

    public void syncToSave(PlayerData pd) {
        pd.ownedVehicleIds = new ArrayList<>(ownedVehicleIds);
    }

    // ── Getters ───────────────────────────────────────────────
    /** True when market hours are active (07:00 – 21:00 in-game). */
    private boolean isMarketOpen() {
        if (game.activeSave == null) return true;
        float hour = game.activeSave.gameHour;
        return hour >= 7f && hour < 21f;
    }

        public int          getPrice(String id)         { return prices.getOrDefault(id, 0); }
    public List<Item>   getShopInventory(String id) { return shopInventories.getOrDefault(id, new ArrayList<>()); }
    public boolean      ownsVehicle(String id)      { return ownedVehicleIds.contains(id); }
    public Map<String,Integer> getVehiclePrices()   { return Collections.unmodifiableMap(vehiclePrices); }
}
