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
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.ui.MobileControls;

/**
 * CricketGame — 6-ball over batting mini-game.
 *
 * Flow per ball:
 *   BOWLING  – ball travels from top of screen toward batsman at bottom.
 *   SWING    – player taps ATTACK at the right moment to hit.
 *
 *   Hit zones (based on tap timing):
 *     Perfect (±0.06s of sweet spot): 4 or 6 runs  (random)
 *     Good    (±0.12s):               1, 2, or 3 runs
 *     Miss    (outside window):       WICKET
 *
 * Game ends after 6 balls or 3 wickets.
 * Win condition: score ≥ 12 runs in the over.
 * Reward: ₹350, 25 rep, quest progress for s09_cricket.
 */
public class CricketGame {

    public enum Phase { INACTIVE, READY, BOWLING, HIT_RESULT, OVER }

    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // Bowl path
    private static final float BALL_START_Y  = VH - 60f;
    private static final float BALL_END_Y    = 80f;
    private static final float BALL_X        = VW / 2f;
    private static final float SWEET_SPOT_Y  = VH - 160f;   // where to hit
    private static final float PERFECT_WIN   = 0.06f;
    private static final float GOOD_WIN      = 0.14f;
    private static final float BALL_SPEED    = 380f;          // px/sec

    // ── Game state ────────────────────────────────────────────
    private Phase  phase       = Phase.INACTIVE;
    private int    ballsLeft   = 6;
    private int    wicketsLeft = 3;
    private int    totalRuns   = 0;
    private float  ballY       = BALL_START_Y;
    private float  ballTimer   = 0f;
    private float  readyTimer  = 0f;
    private boolean swung      = false;

    // Last ball result display
    private String lastResultText = "";
    private Color  lastResultColor = Color.WHITE;
    private float  resultTimer = 0f;

    // Ball trajectory angle (±10° variation)
    private float ballXOffset = 0f;

    // Bowler animation flicker
    private float bowlerAnim = 0f;

    private final VillageLegends game;
    private final BitmapFont     font;

    // ─────────────────────────────────────────────────────────
    public CricketGame(VillageLegends game) {
        this.game = game;
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    // ── Start ─────────────────────────────────────────────────
    public void start() {
        if (phase != Phase.INACTIVE) return;
        ballsLeft   = 6;
        wicketsLeft = 3;
        totalRuns   = 0;
        phase       = Phase.READY;
        readyTimer  = 1.5f;
        game.audioManager.playSfx("ui_open");
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, MobileControls ctrl) {
        bowlerAnim += delta;

        switch (phase) {
            case READY:
                readyTimer -= delta;
                if (readyTimer <= 0) launchBall();
                break;

            case BOWLING:
                ballTimer += delta;
                ballY = BALL_START_Y - ballTimer * BALL_SPEED;
                ballXOffset = MathUtils.sin(ballTimer * 3f) * 18f;

                // Auto-miss if ball passes batsman without swing
                if (ballY < BALL_END_Y) {
                    if (!swung) onMiss();
                    else        nextBall();
                }

                // Player swings
                if (ctrl.justAttacked() && !swung) {
                    swung = true;
                    resolveSwing();
                }
                break;

            case HIT_RESULT:
                resultTimer -= delta;
                if (resultTimer <= 0) nextBall();
                break;

            case OVER:
                resultTimer -= delta;
                if (resultTimer <= 0) phase = Phase.INACTIVE;
                break;
        }
    }

    // ── Ball logic ────────────────────────────────────────────
    private void launchBall() {
        ballY       = BALL_START_Y;
        ballTimer   = 0f;
        swung       = false;
        ballXOffset = 0f;
        phase       = Phase.BOWLING;
    }

    private void resolveSwing() {
        // Measure timing: how close to the sweet spot?
        float sweetTime = (BALL_START_Y - SWEET_SPOT_Y) / BALL_SPEED;
        float diff = Math.abs(ballTimer - sweetTime);

        int runs = 0;
        if (diff <= PERFECT_WIN) {
            runs = MathUtils.random(1) == 0 ? 4 : 6;
            lastResultText  = runs + " RUNS!  🏏 PERFECT!";
            lastResultColor = Color.LIME;
            game.audioManager.playSfx("harvest");   // use as cheer
        } else if (diff <= GOOD_WIN) {
            runs = 1 + MathUtils.random(2);
            lastResultText  = runs + " " + (runs == 1 ? "Run" : "Runs") + "!";
            lastResultColor = Color.YELLOW;
        } else {
            onMiss();
            return;
        }

        totalRuns   += runs;
        phase        = Phase.HIT_RESULT;
        resultTimer  = 1.2f;
        // Update mini-game score
        int prev = game.activeSave.miniGameScores.getOrDefault("cricket_runs", 0);
        game.activeSave.miniGameScores.put("cricket_runs", Math.max(prev, totalRuns));
    }

    private void onMiss() {
        wicketsLeft--;
        lastResultText  = "WICKET!  💥";
        lastResultColor = Color.RED;
        game.audioManager.playSfx("player_hurt");
        phase       = Phase.HIT_RESULT;
        resultTimer = 1.4f;
        if (wicketsLeft <= 0) {
            phase       = Phase.OVER;
            resultTimer = 3.5f;
            endMatch();
        }
    }

    private void nextBall() {
        ballsLeft--;
        if (ballsLeft <= 0 || wicketsLeft <= 0) {
            phase       = Phase.OVER;
            resultTimer = 3.5f;
            endMatch();
        } else {
            phase      = Phase.READY;
            readyTimer = 1.0f;
        }
    }

    private void endMatch() {
        boolean won = totalRuns >= 12 && wicketsLeft > 0;
        if (won) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Cricket over! " + totalRuns + " runs — Victory!"));
            game.questManager.advanceObjective("s09_cricket", "win_cricket_match", 1);
            // money/rep grant via questManager rewards
        } else {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Cricket over. " + totalRuns + " runs scored."));
        }
    }

    // ── Draw ─────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (phase == Phase.INACTIVE) return;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Cricket pitch background
        sr.setColor(0.4f, 0.7f, 0.3f, 0.9f);  sr.rect(0, 0, VW, VH);
        sr.setColor(0.55f, 0.8f, 0.4f, 0.9f);
        sr.rect(VW/2f - 120, 60, 240, VH - 120);    // pitch strip
        sr.setColor(0.9f, 0.9f, 0.9f, 0.8f);         // crease lines
        sr.rect(VW/2f - 80, SWEET_SPOT_Y - 4, 160, 4);  // batting crease
        sr.rect(VW/2f - 80, VH - 90, 160, 4);           // bowling crease

        // Wickets (3 vertical stumps)
        sr.setColor(0.8f, 0.6f, 0.2f, 1f);
        for (int i = -1; i <= 1; i++) {
            sr.rect(BALL_X + i * 14 - 3, SWEET_SPOT_Y + 8, 6, 44);
        }

        // Ball
        if (phase == Phase.BOWLING) {
            float drawX = BALL_X + ballXOffset;
            // Shadow
            sr.setColor(0f, 0f, 0f, 0.25f); sr.ellipse(drawX - 10, ballY - 8, 20, 10);
            // Ball
            sr.setColor(0.85f, 0.15f, 0.1f, 1f); sr.circle(drawX, ballY, 12, 16);
            sr.setColor(1f, 0.9f, 0.8f, 0.5f); sr.circle(drawX - 3, ballY + 3, 4, 8); // highlight
        }

        // Bowler (top of pitch)
        float bx = BALL_X, by = VH - 75f;
        float bowlerBob = MathUtils.sin(bowlerAnim * 4f) * 3f;
        sr.setColor(0.3f, 0.5f, 0.8f, 1f);
        sr.circle(bx, by + 24 + bowlerBob, 12, 10);       // head
        sr.rect(bx - 10, by + bowlerBob, 20, 24);          // body

        // Batsman (bottom of pitch)
        float batY = SWEET_SPOT_Y - 24;
        sr.setColor(0.8f, 0.5f, 0.2f, 1f);
        sr.circle(BALL_X, batY + 36, 12, 10);
        sr.rect(BALL_X - 10, batY, 20, 36);
        // Bat
        sr.setColor(0.7f, 0.55f, 0.3f, 1f);
        sr.rect(BALL_X + 8, batY + 10, 6, 38);

        // Sweet spot indicator
        float pct = phase == Phase.BOWLING
                ? 1f - Math.abs(ballY - SWEET_SPOT_Y) / (BALL_START_Y - SWEET_SPOT_Y) : 0;
        sr.setColor(0.2f, 0.9f, 0.2f, pct * 0.5f);
        sr.rect(BALL_X - 80, SWEET_SPOT_Y - 20, 160, 40);

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawHUD(batch);
    }

    private void drawHUD(SpriteBatch batch) {
        // Scoreboard
        font.setColor(Color.WHITE);
        font.draw(batch, "Runs: " + totalRuns, 20, VH - 20);
        font.setColor(Color.RED);
        font.draw(batch, "Wickets: " + (3 - wicketsLeft) + "/3", 20, VH - 46);
        font.setColor(Color.YELLOW);
        font.draw(batch, "Balls left: " + ballsLeft, 20, VH - 72);

        // Win target
        font.getData().setScale(0.9f);
        font.setColor(Color.CYAN);
        font.draw(batch, "Target: 12 runs", VW - 210, VH - 20);
        font.getData().setScale(1.6f);

        // Swing prompt
        if (phase == Phase.BOWLING) {
            float timeToBall = (SWEET_SPOT_Y - ballY) / BALL_SPEED;
            if (timeToBall < 0.3f && timeToBall > 0) {
                float flash = MathUtils.sin(bowlerAnim * 16f) > 0 ? 1f : 0.3f;
                font.setColor(1f, flash, 0f, 1f);
                font.getData().setScale(1.8f);
                font.draw(batch, "TAP ATTACK!", VW/2f - 100, VH/2f);
                font.getData().setScale(1.6f);
            }
        }

        // Result flash
        if ((phase == Phase.HIT_RESULT) && resultTimer > 0) {
            font.getData().setScale(2.2f);
            font.setColor(lastResultColor);
            font.draw(batch, lastResultText, VW/2f - 120, VH/2f + 40);
            font.getData().setScale(1.6f);
        }

        // Over result
        if (phase == Phase.OVER) {
            boolean won = totalRuns >= 12;
            font.getData().setScale(2.4f);
            font.setColor(won ? Color.LIME : Color.RED);
            font.draw(batch, won ? "GREAT OVER!  🏏" : "OVER DONE.",
                    VW/2f - 140, VH/2f + 30);
            font.getData().setScale(1.6f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Total: " + totalRuns + " runs", VW/2f - 80, VH/2f - 20);
        }

        // Ready
        if (phase == Phase.READY) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Get ready…", VW/2f - 70, VH/2f);
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean isActive()   { return phase != Phase.INACTIVE; }
    public int     getTotalRuns(){ return totalRuns; }
    public void    dispose()    { font.dispose(); }
}
