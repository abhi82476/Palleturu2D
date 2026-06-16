package com.villagelegends.data;

/**
 * FarmPlot – the live/persisted state of one farm tile.
 *
 * State machine:
 *   EMPTY → PLOWED → PLANTED → GROWING → MATURE → (back to EMPTY after harvest)
 *   Any growing state can transition → DEAD (drought/pests).
 */
public class FarmPlot {

    public enum State { EMPTY, PLOWED, PLANTED, GROWING, MATURE, DEAD }

    // Persisted fields
    public int    plotId;
    public float  worldX;
    public float  worldY;
    public State  state        = State.EMPTY;
    public String cropId       = null;    // null when empty/plowed
    public float  growthHours  = 0f;     // accumulated growth
    public float  waterLevel   = 1f;     // 0..1
    public boolean hasPest     = false;
    public float  pestDamage   = 0f;     // 0..1 – plot dies at 1
    public float  dehydrationTimer = 0f;

    // Runtime only (not serialised)
    private transient boolean highlightThisFrame = false;

    public FarmPlot() {}

    public FarmPlot(int id, float x, float y) {
        this.plotId = id;
        this.worldX = x;
        this.worldY = y;
    }

    /** 0..1 composite health factor used to scale harvest yield */
    public float getHealthFactor() {
        float hydration = waterLevel;
        float pestFactor = hasPest ? (1f - pestDamage) : 1f;
        return Math.max(0f, Math.min(1f, hydration * pestFactor));
    }

    /** Reset to EMPTY state after harvesting or clearing a dead crop */
    public void reset() {
        state              = State.EMPTY;
        cropId             = null;
        growthHours        = 0f;
        waterLevel         = 1f;
        hasPest            = false;
        pestDamage         = 0f;
        dehydrationTimer   = 0f;
    }

    /** Growth progress as 0..1 for a given crop's total hours */
    public float getGrowthProgress(int totalHours) {
        return totalHours > 0 ? Math.min(1f, growthHours / totalHours) : 0f;
    }

    public void setHighlight(boolean h)  { highlightThisFrame = h; }
    public boolean isHighlighted()       { return highlightThisFrame; }
}
