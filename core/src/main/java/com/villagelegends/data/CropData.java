package com.villagelegends.data;

// ─────────────────────────────────────────────────────────────
//  CropData  –  static definition for each crop type
// ─────────────────────────────────────────────────────────────
public class CropData {

    public String id;
    public String displayName;
    public int    growthHours;     // in-game hours to reach MATURE
    public float  waterPerHour;    // waterLevel drained per game-hour
    public int    yieldMin;        // minimum bags at harvest (perfect health)
    public int    yieldMax;        // maximum bags
    public int    pricePerBag;     // base sell price

    // Growth stage sprite region names (used by AnimationManager)
    public String[] stageRegions = new String[5]; // EMPTY,PLANTED,GROWING,MATURE,DEAD

    public CropData() {}

    public CropData(String id, String name, int growHours, float waterPerHr,
                    int yMin, int yMax, int price) {
        this.id           = id;
        this.displayName  = name;
        this.growthHours  = growHours;
        this.waterPerHour = waterPerHr;
        this.yieldMin     = yMin;
        this.yieldMax     = yMax;
        this.pricePerBag  = price;
        for (int i = 0; i < 5; i++) stageRegions[i] = id + "_stage_" + i;
    }
}
