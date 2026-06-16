package com.villagelegends.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.world.DayNightCycle;

import java.util.*;

/**
 * FestivalManager tracks the in-game calendar and activates
 * one of the four major South Indian festivals each "season".
 *
 * Festival months (in-game day milestones):
 *   Sankranti   Day 14  (Jan – harvest festival)
 *   Ugadi       Day 30  (Apr – New Year)
 *   Dasara      Day 60  (Oct – victory festival)
 *   Deepavali   Day 75  (Nov – festival of lights)
 *
 * Active festivals:
 *  - Add world decorations (colour tint, particle confetti)
 *  - Unlock festival-specific quests and mini-games
 *  - Apply bonus prices at the market (±20%)
 *  - Play festival music
 */
public class FestivalManager {

    public enum Festival { NONE, SANKRANTI, UGADI, DASARA, DEEPAVALI }

    private final VillageLegends game;
    private Festival currentFestival = Festival.NONE;
    private int      lastDay         = 0;
    private float    festivalTimer   = 0f;     // how long festival has been active
    private float    confettiTimer   = 0f;

    // Confetti particles for Deepavali / Dasara
    private final List<Confetti> particles = new ArrayList<>();
    private static final int MAX_CONFETTI  = 80;

    // Festival day triggers (in-game day numbers, loop mod 90)
    private static final int DAY_SANKRANTI = 14;
    private static final int DAY_UGADI     = 30;
    private static final int DAY_DASARA    = 60;
    private static final int DAY_DEEPAVALI = 75;
    private static final int FESTIVAL_DURATION_DAYS = 3;

    // ─────────────────────────────────────────────────────────
    public FestivalManager(VillageLegends game) {
        this.game = game;
        subscribeToEvents();
        initConfetti();
    }

    private void subscribeToEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.NEW_DAY, e -> {
            int day = e.intData() % 90;
            checkFestivalTrigger(day);
        });
    }

    private void initConfetti() {
        for (int i = 0; i < MAX_CONFETTI; i++) {
            particles.add(new Confetti());
        }
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, DayNightCycle dayNight) {
        if (currentFestival == Festival.NONE) return;

        festivalTimer += delta;
        confettiTimer += delta;

        // Update confetti
        if (currentFestival == Festival.DEEPAVALI || currentFestival == Festival.DASARA) {
            for (Confetti c : particles) c.update(delta);
        }

        // Festival ends after duration
        float festivalEndSecs = FESTIVAL_DURATION_DAYS * com.villagelegends.GameConstants.DAY_DURATION_SECONDS;
        if (festivalTimer >= festivalEndSecs) {
            endFestival();
        }
    }

    // ── Festival triggers ─────────────────────────────────────
    private void checkFestivalTrigger(int day) {
        if (day == DAY_SANKRANTI && currentFestival == Festival.NONE) {
            startFestival(Festival.SANKRANTI);
        } else if (day == DAY_UGADI && currentFestival == Festival.NONE) {
            startFestival(Festival.UGADI);
        } else if (day == DAY_DASARA && currentFestival == Festival.NONE) {
            startFestival(Festival.DASARA);
        } else if (day == DAY_DEEPAVALI && currentFestival == Festival.NONE) {
            startFestival(Festival.DEEPAVALI);
        }
    }

    private void startFestival(Festival f) {
        currentFestival = f;
        festivalTimer   = 0f;
        game.audioManager.playFestivalMusic(f.name().toLowerCase());
        unlockFestivalQuests(f);
        resetConfetti();
        game.eventBus.post(GameEventBus.EventType.FESTIVAL_START, f.name());
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                getFestivalGreeting(f)));
        Gdx.app.log("Festival", "Started: " + f);
    }

    private void endFestival() {
        game.eventBus.post(GameEventBus.EventType.FESTIVAL_END, currentFestival.name());
        currentFestival = Festival.NONE;
        festivalTimer   = 0f;
        game.audioManager.playRegionMusic(game.activeSave.playerData.currentRegion);
    }

    private void unlockFestivalQuests(Festival f) {
        switch (f) {
            case SANKRANTI:
                game.questManager.triggerByFlag("sankranti_active");
                break;
            case UGADI:
                game.questManager.triggerByFlag("ugadi_active");
                break;
            case DASARA:
                game.questManager.triggerByFlag("dasara_active");
                break;
            case DEEPAVALI:
                game.questManager.triggerByFlag("deepavali_active");
                break;
        }
    }

    private String getFestivalGreeting(Festival f) {
        switch (f) {
            case SANKRANTI:  return "Happy Sankranti! Pongal time! 🌾";
            case UGADI:      return "Ugadi Shubhakankshalu! New year begins! 🌸";
            case DASARA:     return "Dasara Subhakanksalu! Victory of good! 🎉";
            case DEEPAVALI:  return "Happy Deepavali! Festival of lights! 🪔";
            default:         return "";
        }
    }

    // ── Confetti ─────────────────────────────────────────────
    private void resetConfetti() {
        for (Confetti c : particles) c.reset();
    }

    // ── Draw overlays ─────────────────────────────────────────
    public void drawOverlay(SpriteBatch batch, float screenW, float screenH) {
        if (currentFestival == Festival.NONE) return;

        // Festival banner
        drawFestivalBanner(batch, screenW, screenH);

        // Confetti (Deepavali / Dasara)
        if (currentFestival == Festival.DEEPAVALI || currentFestival == Festival.DASARA) {
            drawConfetti(batch, screenW, screenH);
        }
    }

    private void drawFestivalBanner(SpriteBatch batch, float w, float h) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        Color bannerColor = getFestivalColor();
        sr.setColor(bannerColor.r, bannerColor.g, bannerColor.b, 0.25f);
        sr.rect(0, h - 50, w, 50);

        sr.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();
    }

    private Color getFestivalColor() {
        switch (currentFestival) {
            case SANKRANTI:  return new Color(1f, 0.8f, 0.1f, 1f);   // golden yellow
            case UGADI:      return new Color(0.4f, 0.9f, 0.4f, 1f); // spring green
            case DASARA:     return new Color(0.9f, 0.3f, 0.1f, 1f); // vermilion red
            case DEEPAVALI:  return new Color(1.0f, 0.6f, 0.0f, 1f); // lamp orange
            default:         return Color.WHITE;
        }
    }

    private void drawConfetti(SpriteBatch batch, float w, float h) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Confetti c : particles) {
            sr.setColor(c.color);
            sr.rect(c.x * w, c.y * h, 4, 6);
        }
        sr.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();
    }

    // ── World tint modifier ───────────────────────────────────
    public float[] applyFestivalTint(float[] worldTint) {
        if (currentFestival == Festival.NONE) return worldTint;
        Color fc  = getFestivalColor();
        float amt = 0.12f;
        return new float[]{
            worldTint[0] * (1 - amt) + fc.r * amt,
            worldTint[1] * (1 - amt) + fc.g * amt,
            worldTint[2] * (1 - amt) + fc.b * amt
        };
    }

    // ── Save / Load ───────────────────────────────────────────
    public void loadFromSave(GameSaveData save) {
        if (save.activeFestival != null && !save.activeFestival.isEmpty()) {
            try {
                currentFestival = Festival.valueOf(save.activeFestival);
                festivalTimer   = save.festivalTimer;
            } catch (Exception ignored) {}
        }
    }

    public void syncToSave(GameSaveData save) {
        save.activeFestival = currentFestival.name();
        save.festivalTimer  = festivalTimer;
    }

    // ── Getters ───────────────────────────────────────────────
    public Festival getCurrentFestival()     { return currentFestival; }
    public boolean  isFestivalActive()       { return currentFestival != Festival.NONE; }
    public String getFestivalName() {
        if (currentFestival == Festival.SANKRANTI) return "Sankranti";
        if (currentFestival == Festival.UGADI)     return "Ugadi";
        if (currentFestival == Festival.DASARA)    return "Dasara";
        if (currentFestival == Festival.DEEPAVALI) return "Deepavali";
        return "";
    }

    // ── Inner confetti particle ───────────────────────────────
    private static class Confetti {
        float x, y, vx, vy;
        Color color;
        void reset() {
            x     = MathUtils.random();
            y     = 1.0f + MathUtils.random(0.3f);
            vx    = MathUtils.random(-0.02f, 0.02f);
            vy    = -MathUtils.random(0.05f, 0.15f);
            color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 0.9f);
        }
        void update(float delta) {
            x += vx * delta;
            y += vy * delta;
            if (y < -0.05f) reset();
        }
    }
}
