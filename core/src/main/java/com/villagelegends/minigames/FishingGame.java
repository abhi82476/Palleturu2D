package com.villagelegends.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Item;
import com.villagelegends.entities.Player;
import com.villagelegends.systems.GameEventBus;

/**
 * FishingGame — tap-timing mini-game.
 *
 * Flow:
 *  1. CASTING  – meter fills while holding Attack button; release to cast.
 *  2. WAITING  – bobber animation; random bite delay.
 *  3. BITING   – "TAP NOW!" window; if player taps in time → REELING.
 *  4. REELING  – keep a moving bar in a shrinking target zone; hold Attack.
 *  5. CAUGHT / MISSED result.
 *
 * Fish type determined by time-of-day, luck, and zone.
 * Legendary catfish: dawn (5-7 AM), deep lake zone, low probability.
 */
public class FishingGame {

    public enum State { INACTIVE, CASTING, WAITING, BITING, REELING, RESULT }

    // ── Constants ─────────────────────────────────────────────
    private static final float CAST_MAX_POWER = 1.0f;
    private static final float BITE_WINDOW    = 1.2f;    // seconds to react
    private static final float REEL_DURATION  = 4.0f;
    private static final float REEL_BAR_SPEED = 0.35f;   // 0..1 per second
    private static final float TARGET_WIDTH   = 0.28f;   // fraction of bar
    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // ── State ─────────────────────────────────────────────────
    private State  state         = State.INACTIVE;
    private float  castPower     = 0f;
    private boolean castHeld     = false;
    private float  waitTimer     = 0f;
    private float  biteTimer     = 0f;
    private float  reelPos       = 0f;     // 0..1 position of bar
    private float  reelDir       = 1f;
    private float  reelTarget    = 0.36f;  // centre of target zone 0..1
    private float  reelTimer     = 0f;
    private boolean reelPressed  = false;
    private float  reelHoldPower = 0f;
    private boolean resultCaught = false;

    private float bobberAnim     = 0f;

    // ── References ────────────────────────────────────────────
    private final VillageLegends game;
    private final Player          player;

    // ── Fonts ─────────────────────────────────────────────────
    private final BitmapFont font;

    // ─────────────────────────────────────────────────────────
    public FishingGame(VillageLegends game, Player player) {
        this.game   = game;
        this.player = player;
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    // ── Start ─────────────────────────────────────────────────
    public void start() {
        if (state != State.INACTIVE) return;
        if (!player.hasItem("fishing_rod")) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Need a fishing rod!"));
            return;
        }
        state     = State.CASTING;
        castPower = 0f;
        castHeld  = false;
        game.audioManager.playSfx("ui_open");
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, com.villagelegends.ui.MobileControls controls) {
        bobberAnim += delta * 2f;

        switch (state) {
            case CASTING:  updateCasting(delta, controls);  break;
            case WAITING:  updateWaiting(delta, controls);  break;
            case BITING:   updateBiting(delta, controls);   break;
            case REELING:  updateReeling(delta, controls);  break;
            case RESULT:
                waitTimer += delta;
                if (waitTimer > 2.5f) { state = State.INACTIVE; }
                break;
            default: break;
        }
    }

    private void updateCasting(float delta, com.villagelegends.ui.MobileControls ctrl) {
        if (ctrl.isSprinting()) {
            castHeld  = true;
            castPower = Math.min(CAST_MAX_POWER, castPower + delta * 1.4f);
        } else if (castHeld) {
            // Released – cast!
            state     = State.WAITING;
            waitTimer = 2f + MathUtils.random(5f);   // random bite wait
            game.audioManager.playSfx("water_crops"); // splash sound
        }
    }

    private void updateWaiting(float delta, com.villagelegends.ui.MobileControls ctrl) {
        waitTimer -= delta;
        if (waitTimer <= 0) {
            state      = State.BITING;
            biteTimer  = BITE_WINDOW;
            game.audioManager.playSfx("ui_click");
        }
    }

    private void updateBiting(float delta, com.villagelegends.ui.MobileControls ctrl) {
        biteTimer -= delta;
        if (ctrl.justAttacked()) {
            // Player tapped in time!
            state      = State.REELING;
            reelPos    = 0.5f;
            reelDir    = 1f;
            reelTimer  = REEL_DURATION;
            reelTarget = 0.30f + MathUtils.random(0.30f);
        } else if (biteTimer <= 0) {
            // Missed the window
            resultCaught = false;
            state        = State.RESULT;
            waitTimer    = 0f;
        }
    }

    private void updateReeling(float delta, com.villagelegends.ui.MobileControls ctrl) {
        reelTimer -= delta;

        // Bar drifts on its own
        float drift = REEL_BAR_SPEED * reelDir * delta;
        // Player holding attack slows drift and pulls toward target
        boolean held = ctrl.isSprinting();
        if (held) drift *= 0.4f;

        reelPos += drift;
        if (reelPos >= 1f) { reelPos = 1f; reelDir = -1f; }
        if (reelPos <= 0f) { reelPos = 0f; reelDir =  1f; }

        // Player presses attack to "hold" fish
        reelHoldPower = held ? Math.min(1f, reelHoldPower + delta * 0.8f)
                             : Math.max(0f, reelHoldPower - delta * 1.2f);

        // Check if bar is in target zone
        boolean inZone = reelPos >= reelTarget - TARGET_WIDTH / 2f
                      && reelPos <= reelTarget + TARGET_WIDTH / 2f;

        if (reelHoldPower >= 0.95f && inZone) {
            resultCaught = true;
            state        = State.RESULT;
            waitTimer    = 0f;
            grantCatch();
        } else if (reelTimer <= 0) {
            resultCaught = false;
            state        = State.RESULT;
            waitTimer    = 0f;
        }
    }

    private void grantCatch() {
        // Determine fish type
        float gameHour = game.activeSave.gameHour;
        boolean isDawn = gameHour >= 5f && gameHour < 7f;
        boolean isLegendary = isDawn && MathUtils.random() < 0.05f;

        String fishId = isLegendary ? "legendary_catfish" : pickFishType();
        Item fish = new Item(fishId,
                isLegendary ? "Legendary Catfish" : "Fish",
                Item.Category.FISH,
                game.economyManager.getPrice(fishId));

        player.addItem(fish);
        game.activeSave.playerData.totalFishCaught++;
        game.questManager.advanceObjective("s04_big_fish",  "fish_legendary_catfish", isLegendary ? 1 : 0);
        game.questManager.advanceObjective("s05_fish_supply","catch_any_fish", 1);
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                "Caught a " + fish.displayName + "! ₹" + fish.price));
        game.audioManager.playSfx("harvest");
    }

    private String pickFishType() {
        float r = MathUtils.random();
        if      (r < 0.35f) return "rohu_fish";
        else if (r < 0.60f) return "catla_fish";
        else if (r < 0.80f) return "tilapia_fish";
        else if (r < 0.92f) return "prawn";
        else                return "murrel_fish";
    }

    // ── Draw ─────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (state == State.INACTIVE) return;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Dark overlay
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0.05f, 0.1f, 0.72f);
        sr.rect(0, 0, VW, VH);
        sr.end();

        switch (state) {
            case CASTING:  drawCasting(sr);  break;
            case WAITING:  drawWaiting(sr);  break;
            case BITING:   drawBiting(sr);   break;
            case REELING:  drawReeling(sr);  break;
            case RESULT:   drawResult(sr);   break;
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        drawLabels(batch);
    }

    // ── Draw per-state ────────────────────────────────────────
    private void drawCasting(ShapeRenderer sr) {
        // Cast power bar
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.2f, 0.2f, 0.85f);
        sr.rect(VW / 2f - 120, VH / 2f - 20, 240, 30);
        sr.setColor(Color.LIME);
        sr.rect(VW / 2f - 120, VH / 2f - 20, 240 * castPower, 30);
        sr.end();
    }

    private void drawWaiting(ShapeRenderer sr) {
        // Animated bobber
        sr.begin(ShapeRenderer.ShapeType.Filled);
        float bx = VW / 2f, by = VH * 0.55f + MathUtils.sin(bobberAnim) * 6f;
        sr.setColor(Color.RED);
        sr.circle(bx, by, 10, 12);
        sr.setColor(Color.WHITE);
        sr.circle(bx, by + 4, 5, 8);
        sr.end();
    }

    private void drawBiting(ShapeRenderer sr) {
        // Frantic bobber + flash
        float flash = MathUtils.sin(bobberAnim * 8f) > 0 ? 1f : 0.3f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(1f, flash, 0f, 0.9f);
        sr.circle(VW / 2f, VH / 2f + MathUtils.sin(bobberAnim * 6f) * 12f, 14, 16);
        // Progress arc showing how much time remains
        float pct = biteTimer / BITE_WINDOW;
        sr.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        sr.rect(VW / 2f - 80, VH / 2f - 60, 160, 16);
        sr.setColor(pct > 0.5f ? Color.GREEN : Color.RED);
        sr.rect(VW / 2f - 80, VH / 2f - 60, 160 * pct, 16);
        sr.end();
    }

    private void drawReeling(ShapeRenderer sr) {
        // Reel bar
        float bx = VW / 2f - 180, by = VH / 2f - 14, bw = 360, bh = 28;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.15f, 0.15f, 0.9f); sr.rect(bx, by, bw, bh);
        // Target zone (green)
        float tStart = bx + (reelTarget - TARGET_WIDTH / 2f) * bw;
        float tEnd   = TARGET_WIDTH * bw;
        sr.setColor(0.2f, 0.8f, 0.2f, 0.75f); sr.rect(tStart, by, tEnd, bh);
        // Moving marker
        sr.setColor(Color.WHITE); sr.rect(bx + reelPos * bw - 6, by - 6, 12, bh + 12);
        // Hold-power bar
        sr.setColor(0.2f, 0.4f, 0.9f, 0.85f);
        sr.rect(VW / 2f - 80, by - 36, 160 * reelHoldPower, 14);
        sr.setColor(0.3f, 0.3f, 0.3f, 0.7f);
        sr.rect(VW / 2f - 80, by - 36, 160, 14);
        // Remaining time
        float timePct = reelTimer / REEL_DURATION;
        sr.setColor(timePct > 0.4f ? Color.CYAN : Color.RED);
        sr.rect(VW / 2f - 80, by + bh + 8, 160 * timePct, 10);
        sr.end();
    }

    private void drawResult(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(resultCaught ? Color.GREEN : Color.RED);
        sr.rect(VW / 2f - 150, VH / 2f - 40, 300, 80);
        sr.end();
    }

    private void drawLabels(SpriteBatch batch) {
        font.setColor(Color.WHITE);
        switch (state) {
            case CASTING:
                font.draw(batch, "Hold SPRINT to cast!", VW / 2f - 130, VH / 2f + 60);
                font.draw(batch, "CAST POWER", VW / 2f - 60, VH / 2f + 10);
                break;
            case WAITING:
                font.draw(batch, "Waiting for a bite…", VW / 2f - 110, VH / 2f + 80);
                break;
            case BITING:
                font.setColor(Color.YELLOW);
                font.getData().setScale(2.2f);
                font.draw(batch, "TAP ATTACK NOW!", VW / 2f - 160, VH / 2f + 40);
                font.getData().setScale(1.6f);
                break;
            case REELING:
                font.draw(batch, "Hold SPRINT to reel!", VW / 2f - 130, VH / 2f + 50);
                font.draw(batch, "Keep marker in green zone", VW / 2f - 160, VH / 2f - 50);
                break;
            case RESULT:
                font.getData().setScale(2.0f);
                font.setColor(resultCaught ? Color.LIME : Color.RED);
                font.draw(batch, resultCaught ? "CAUGHT!" : "GOT AWAY!",
                        VW / 2f - 90, VH / 2f + 24);
                font.getData().setScale(1.6f);
                break;
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean isActive() { return state != State.INACTIVE; }
    public void    dispose()  { font.dispose(); }
}
