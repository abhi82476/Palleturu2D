package com.villagelegends.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.ui.MobileControls;

/**
 * KiteFlyingGame — Sankranti festival mini-game.
 *
 * Player controls their kite using the joystick, trying to cut rival
 * kite strings (manja fight). The rival kites drift unpredictably.
 *
 * Win:  Cut 3 rival kites within 90 seconds.
 * Lose: Your kite string gets cut first, or time runs out.
 *
 * Controls:
 *   Joystick X/Y = Steer kite
 *   SPRINT held  = Tighten string (pull kite toward cut position)
 *   DODGE        = Release string (kite floats freely for 2s)
 *
 * Scoring:
 *   Each rival kite cut = 100 points + festival rep
 *   Time bonus if all 3 cut before 45 seconds
 */
public class KiteFlyingGame {

    public enum Phase { INACTIVE, INTRO, FLYING, RESULT }

    private static final float VW     = GameConstants.VIRTUAL_WIDTH;
    private static final float VH     = GameConstants.VIRTUAL_HEIGHT;
    private static final int   RIVALS  = 3;
    private static final float TIME_LIMIT = 90f;
    private static final float STRING_LEN = 200f;

    // ── Player kite ───────────────────────────────────────────
    private float  kiteX, kiteY;
    private float  kiteVX, kiteVY;
    private float  windX  = 20f, windY = 10f;   // current wind direction
    private float  windTimer = 0f;
    private boolean tight  = false;              // string tightened
    private float  stringAngle = 90f;            // degrees (90=straight up)

    // Anchor point (player's hands, bottom-centre)
    private float  anchorX = VW / 2f, anchorY = 80f;

    // ── Rival kites ───────────────────────────────────────────
    private float[] rivX   = new float[RIVALS];
    private float[] rivY   = new float[RIVALS];
    private float[] rivVX  = new float[RIVALS];
    private float[] rivVY  = new float[RIVALS];
    private boolean[] rivCut = new boolean[RIVALS];
    private Color[]  rivColors = {Color.RED, Color.BLUE, Color.GREEN};

    // ── Game state ────────────────────────────────────────────
    private Phase  phase      = Phase.INACTIVE;
    private float  timeLeft   = TIME_LIMIT;
    private int    cutCount   = 0;
    private int    score      = 0;
    private float  introTimer = 2f;
    private float  flashTimer = 0f;
    private String flashText  = "";
    private Color  flashColor = Color.WHITE;
    private float  kiteAnim   = 0f;

    private final VillageLegends game;
    private final BitmapFont     font;

    // ─────────────────────────────────────────────────────────
    public KiteFlyingGame(VillageLegends game) {
        this.game = game;
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    // ── Start ─────────────────────────────────────────────────
    public void start() {
        if (phase != Phase.INACTIVE) return;

        // Player kite starts above anchor
        kiteX = anchorX;
        kiteY = anchorY + STRING_LEN;
        kiteVX = kiteVY = 0;
        stringAngle = 90f;
        tight = false;
        timeLeft = TIME_LIMIT;
        cutCount = 0;
        score    = 0;
        introTimer = 2.5f;
        phase  = Phase.INTRO;

        // Place rival kites at spread positions
        for (int i = 0; i < RIVALS; i++) {
            rivX[i]   = 150f + i * 300f;
            rivY[i]   = 350f + MathUtils.random(80f);
            rivVX[i]  = (MathUtils.random() - 0.5f) * 60f;
            rivVY[i]  = (MathUtils.random() - 0.5f) * 30f;
            rivCut[i] = false;
        }

        game.audioManager.playSfx("ui_open");
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, MobileControls ctrl) {
        kiteAnim += delta * 2f;

        switch (phase) {
            case INTRO:
                introTimer -= delta;
                if (introTimer <= 0) phase = Phase.FLYING;
                break;

            case FLYING:
                timeLeft -= delta;
                flashTimer = Math.max(0, flashTimer - delta);

                updateWind(delta);
                updatePlayerKite(delta, ctrl);
                updateRivals(delta);
                checkCuts();

                if (cutCount >= RIVALS) {
                    // Win!
                    score += (int)(timeLeft * 5);
                    onResult(true);
                } else if (timeLeft <= 0) {
                    onResult(false);
                }
                break;

            case RESULT:
                flashTimer -= delta;
                if (flashTimer <= 0) phase = Phase.INACTIVE;
                break;
        }
    }

    private void updateWind(float delta) {
        windTimer += delta;
        if (windTimer > 4f) {
            windTimer = 0;
            windX = (MathUtils.random() - 0.5f) * 60f;
            windY = (MathUtils.random() - 0.5f) * 25f;
        }
    }

    private void updatePlayerKite(float delta, MobileControls ctrl) {
        float jx = ctrl.getJoystickX();
        float jy = ctrl.getJoystickY();
        tight = ctrl.isSprinting();

        // Kite physics: wind + player input
        float inputForce = 120f;
        kiteVX += (jx * inputForce + windX) * delta;
        kiteVY += (jy * inputForce + windY) * delta;

        // String constraint: kite can't go further than STRING_LEN from anchor
        kiteVX *= 0.92f;
        kiteVY *= 0.92f;

        float nx = kiteX + kiteVX * delta;
        float ny = kiteY + kiteVY * delta;

        float dx = nx - anchorX, dy = ny - anchorY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (tight && dist > STRING_LEN * 0.6f) {
            // Pull toward anchor
            float pullF = 200f * delta;
            kiteVX -= (dx / dist) * pullF;
            kiteVY -= (dy / dist) * pullF;
        }

        if (dist > STRING_LEN) {
            // Clamp to string length
            kiteX = anchorX + dx / dist * STRING_LEN;
            kiteY = anchorY + dy / dist * STRING_LEN;
            kiteVX *= 0.5f; kiteVY *= 0.5f;
        } else {
            kiteX = nx; kiteY = ny;
        }

        // Keep kite on screen
        kiteX = MathUtils.clamp(kiteX, 30, VW - 30);
        kiteY = MathUtils.clamp(kiteY, 100, VH - 40);
    }

    private void updateRivals(float delta) {
        for (int i = 0; i < RIVALS; i++) {
            if (rivCut[i]) continue;
            rivX[i] += rivVX[i] * delta;
            rivY[i] += rivVY[i] * delta;
            rivVX[i] += (windX * 0.7f + (MathUtils.random()-0.5f)*15f) * delta;
            rivVY[i] += (windY * 0.5f + (MathUtils.random()-0.5f)*10f) * delta;
            rivVX[i] *= 0.94f; rivVY[i] *= 0.94f;
            if (rivX[i] < 30 || rivX[i] > VW-30) rivVX[i] *= -1;
            if (rivY[i] < 100 || rivY[i] > VH-40) rivVY[i] *= -1;
        }
    }

    private void checkCuts() {
        for (int i = 0; i < RIVALS; i++) {
            if (rivCut[i]) continue;
            float dx = rivX[i] - kiteX, dy = rivY[i] - kiteY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 28f && tight) {
                rivCut[i] = true;
                cutCount++;
                score += 100;
                flashText  = "CUT! ✂";
                flashColor = Color.LIME;
                flashTimer = 1.2f;
                game.audioManager.playSfx("attack_swing");
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION,
                        "Kite cut! " + cutCount + "/" + RIVALS));
            }
        }
    }

    private void onResult(boolean won) {
        phase      = Phase.RESULT;
        flashTimer = 3.5f;
        if (won) {
            flashText  = "YOU WIN! Score: " + score;
            flashColor = Color.LIME;
            game.questManager.advanceObjective("s17_kite_contest", "win_kite_contest", 1);
            if (game.activeSave != null) {
                int prev = game.activeSave.miniGameScores.getOrDefault("kite_contest_score", 0);
                game.activeSave.miniGameScores.put("kite_contest_score", Math.max(prev, score));
            }
        } else {
            flashText  = "Time Up! Cut: " + cutCount + "/" + RIVALS;
            flashColor = Color.RED;
        }
    }

    // ── Draw ─────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (phase == Phase.INACTIVE) return;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Sky background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.4f, 0.65f, 0.9f, 0.9f);
        sr.rect(0, 0, VW, VH);

        // Ground strip
        sr.setColor(0.45f, 0.7f, 0.3f, 1f);
        sr.rect(0, 0, VW, 70f);

        // Clouds (white blobs)
        sr.setColor(1f, 1f, 1f, 0.7f);
        float[] cx = {100, 350, 700, 1000};
        float[] cy = {VH - 80, VH - 120, VH - 60, VH - 100};
        for (int i = 0; i < 4; i++) {
            float cloud_x = ((cx[i] + kiteAnim * 8f) % (VW + 100)) - 50;
            sr.ellipse(cloud_x, cy[i], 100, 40);
            sr.ellipse(cloud_x + 40, cy[i] + 10, 80, 35);
        }

        // Rival kite strings + kites
        for (int i = 0; i < RIVALS; i++) {
            if (rivCut[i]) continue;
            sr.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            // String (simple line approximation via thin rect)
            drawDashedLine(sr, anchorX + 60 * (i - 1), 80f, rivX[i], rivY[i]);
            drawKite(sr, rivX[i], rivY[i], rivColors[i], kiteAnim + i);
        }

        // Player string
        sr.setColor(tight ? Color.YELLOW : new Color(0.6f, 0.5f, 0.3f, 0.8f));
        drawDashedLine(sr, anchorX, anchorY, kiteX, kiteY);

        // Player kite (always yellow/gold)
        drawKite(sr, kiteX, kiteY, Color.GOLD, kiteAnim);

        // Player figure (stick figure at anchor)
        sr.setColor(0.6f, 0.4f, 0.2f, 1f);
        sr.circle(anchorX, anchorY + 28, 12, 10);
        sr.rect(anchorX - 8, anchorY, 16, 28);
        sr.rect(anchorX - 20, anchorY + 10, 40, 6); // arms

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawHUD(batch);
    }

    private void drawKite(ShapeRenderer sr, float x, float y, Color col, float anim) {
        float sway = MathUtils.sin(anim * 2.5f) * 5f;
        // Diamond shape
        sr.setColor(col);
        sr.triangle(x, y + 20 + sway, x - 14, y + sway, x, y - 18 + sway);
        sr.triangle(x, y + 20 + sway, x + 14, y + sway, x, y - 18 + sway);
        // Tail
        sr.setColor(col.r * 0.7f, col.g * 0.7f, col.b * 0.7f, 0.8f);
        for (int t = 0; t < 4; t++) {
            float tx = x + MathUtils.sin(anim * 3f + t) * 8f;
            float ty = y - 18 - t * 12 + sway;
            sr.circle(tx, ty, 3, 6);
        }
    }

    private void drawDashedLine(ShapeRenderer sr, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float steps = len / 12f;
        for (int i = 0; i < steps; i += 2) {
            float t1 = i / steps, t2 = (i + 1) / steps;
            sr.rectLine(x1 + dx * t1, y1 + dy * t1, x1 + dx * t2, y1 + dy * t2, 1.5f);
        }
    }

    private void drawHUD(SpriteBatch batch) {
        if (phase == Phase.INTRO) {
            font.getData().setScale(2.2f);
            font.setColor(Color.YELLOW);
            font.draw(batch, "Sankranti Kite Fight!", VW / 2f - 180, VH / 2f + 20);
            font.setColor(Color.WHITE);
            font.getData().setScale(1.2f);
            font.draw(batch, "Cut 3 rival kites to win!  Tight string = SPRINT",
                    VW / 2f - 200, VH / 2f - 20);
            font.getData().setScale(1.6f);
            return;
        }

        // Timer
        Color timerCol = timeLeft > 30 ? Color.WHITE : Color.RED;
        font.setColor(timerCol);
        font.draw(batch, String.format("%.0f s", timeLeft), VW / 2f - 30, VH - 22);

        // Score + cuts
        font.setColor(Color.GOLD);
        font.draw(batch, "Score: " + score, 20, VH - 22);
        font.draw(batch, "Cuts: " + cutCount + "/" + RIVALS, 20, VH - 48);

        // Wind indicator
        font.getData().setScale(0.9f);
        font.setColor(Color.CYAN);
        font.draw(batch, String.format("Wind: %.0f°", MathUtils.atan2(windY, windX)
                * MathUtils.radiansToDegrees), VW - 180, VH - 22);
        font.getData().setScale(1.6f);

        // String indicator
        font.setColor(tight ? Color.YELLOW : Color.LIGHT_GRAY);
        font.draw(batch, tight ? "STRING: TIGHT" : "STRING: LOOSE", VW / 2f - 80, 50f);

        // Flash result
        if (flashTimer > 0) {
            font.getData().setScale(phase == Phase.RESULT ? 2.2f : 1.8f);
            font.setColor(flashColor);
            GlyphLayout gl = new GlyphLayout(font, flashText);
            font.draw(batch, flashText, VW / 2f - gl.width / 2f, VH / 2f + 40);
            font.getData().setScale(1.6f);
        }
    }

    public boolean isActive() { return phase != Phase.INACTIVE; }
    public void    dispose()  { font.dispose(); }
}
