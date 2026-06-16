package com.villagelegends.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All persistent player-specific data serialised into a save slot.
 */
public class PlayerData {

    // Position & region
    public float  worldX               = 400f;
    public float  worldY               = 300f;
    public String currentRegion        = "main_village";

    // Vital stats
    public int    health               = 100;
    public int    maxHealth            = 100;
    public float  stamina              = 100f;
    public int    day                  = 1;

    // Economy
    public int    money                = 500;
    public int    reputation           = 0;

    // Inventory
    public List<Item>   inventoryItems    = new ArrayList<>();
    public List<String> ownedVehicleIds   = new ArrayList<>();
    public int          hotbarSlot        = 0;

    // Lifetime stats
    public int    totalCropsHarvested     = 0;
    public int    totalFishCaught         = 0;
    public int    totalEnemiesDefeated    = 0;
    public int    totalQuestsComplete     = 0;
    public float  totalDistanceTravelled  = 0f;
    public int    highestReputationEver   = 0;

    // Exploration
    public Set<String>  visitedRegions    = new HashSet<>();

    // Farming
    public int    farmPlotsOwned          = 6;

    public PlayerData() {
        visitedRegions.add("main_village");
    }
}
