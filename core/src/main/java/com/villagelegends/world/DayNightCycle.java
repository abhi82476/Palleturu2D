package com.villagelegends.world;

import com.villagelegends.GameConstants;

/**
 * DayNightCycle advances an in-game clock and returns a colour tint
 * that GameScreen applies to the world SpriteBatch.
 *
 * Sky phases and their RGB tints:
 *   Dawn   05-07  warm orange
 *   Day    07-17  neutral bright
 *   Sunset 17-19  deep amber/red
 *   Dusk   19-21  purple-blue
 *   Night  21-05  dark navy
 */
public class DayNightCycle {

    private float gameHour;          // 0..24
    private int   day       = 1;
    private float realTimer = 0f;

    private float[] currentTint = new float[]{1f, 1f, 1f};

    // Named key frames  [hour, R, G, B]
    private static final float[][] KEY_FRAMES = {
        { 0f,  0.08f, 0.08f, 0.18f },   // midnight navy
        { 4f,  0.06f, 0.06f, 0.14f },   // pre-dawn dark
        { 5f,  0.55f, 0.35f, 0.20f },   // early dawn orange
        { 6f,  0.85f, 0.65f, 0.40f },   // sunrise warm
        { 7f,  1.00f, 0.96f, 0.88f },   // morning bright
        {12f,  1.00f, 1.00f, 1.00f },   // noon neutral
        {17f,  1.00f, 0.90f, 0.70f },   // afternoon golden
        {18f,  0.95f, 0.55f, 0.25f },   // sunset amber
        {19f,  0.65f, 0.35f, 0.45f },   // dusk purple
        {20f,  0.25f, 0.20f, 0.38f },   // early night
        {21f,  0.10f, 0.10f, 0.22f },   // night
        {24f,  0.08f, 0.08f, 0.18f },   // midnight (same as 0)
    };

    public DayNightCycle(float startHour) {
        this.gameHour = startHour;
        computeTint();
    }

    public void update(float delta) {
        realTimer += delta;
        // Advance game time
        float hoursPerSecond = GameConstants.HOURS_PER_DAY
                             / GameConstants.DAY_DURATION_SECONDS;
        gameHour += delta * hoursPerSecond;
        if (gameHour >= 24f) {
            gameHour -= 24f;
            day++;
        }
        computeTint();
    }

    private void computeTint() {
        // Find two surrounding key frames and lerp between them
        float h = gameHour;
        float[] from = KEY_FRAMES[0];
        float[] to   = KEY_FRAMES[KEY_FRAMES.length - 1];

        for (int i = 0; i < KEY_FRAMES.length - 1; i++) {
            if (h >= KEY_FRAMES[i][0] && h < KEY_FRAMES[i + 1][0]) {
                from = KEY_FRAMES[i];
                to   = KEY_FRAMES[i + 1];
                break;
            }
        }

        float t = (h - from[0]) / (to[0] - from[0]);
        currentTint[0] = lerp(from[1], to[1], t);
        currentTint[1] = lerp(from[2], to[2], t);
        currentTint[2] = lerp(from[3], to[3], t);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    // ── Time queries ─────────────────────────────────────────
    public boolean isDay()        { return gameHour >= GameConstants.SUNRISE_HOUR
                                        && gameHour <  GameConstants.SUNSET_HOUR; }
    public boolean isNight()      { return !isDay(); }
    public boolean isSunrise()    { return gameHour >= 5f  && gameHour < 7f; }
    public boolean isSunset()     { return gameHour >= 17f && gameHour < 20f; }
    public boolean isMarketOpen() { return gameHour >= 7f  && gameHour < 21f; }
    public boolean isSchoolTime() { return gameHour >= 7f  && gameHour < 14f; }
    public boolean isTempleTime() { return gameHour >= 18f && gameHour < 22f; }

    public String  getTimeString() {
        int h = (int) gameHour;
        int m = (int)((gameHour - h) * 60);
        String amPm = h < 12 ? "AM" : "PM";
        int h12 = h % 12; if (h12 == 0) h12 = 12;
        return String.format("%d:%02d %s", h12, m, amPm);
    }

    // ── Getters ───────────────────────────────────────────────
    public float   getHour()        { return gameHour; }
    public int     getDay()         { return day; }
    public float[] getWorldTint()   { return currentTint; }
    public float   getAmbientLight(){
        // 0=very dark night, 1=full noon
        float r = currentTint[0] * 0.299f
                + currentTint[1] * 0.587f
                + currentTint[2] * 0.114f;
        return Math.max(0.05f, Math.min(1f, r));
    }
}
