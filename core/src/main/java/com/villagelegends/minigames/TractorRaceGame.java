package com.villagelegends.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.ui.MobileControls;
import com.villagelegends.systems.GameEventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * TractorRaceGame — 3-lap oval race against 2 AI opponents.
 *
 * Track: Simple oval defined by waypoints in virtual screen space.
 * Controls:
 *   Sprint = Accelerate
 *   Dodge  = Brake
 *   Joystick X = Steer
 *
 * Winning: Complete 3 laps before any AI opponent.
 * Rewards on win: ₹500 + 30 rep + quest progress for s06_tractor_race.
 */
public class TractorRaceGame {

    public enum Phase { INACTIVE, COUNTDOWN, RACING, FINISHED }

    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // ── Track waypoints (oval) ────────────────────────────────
    private static final float[][] WAYPOINTS = {
        {VW * 0.20f, VH * 0.70f},
        {VW * 0.20f, VH * 0.30f},
        {VW * 0.35f, VH * 0.15f},
        {VW * 0.65f, VH * 0.15f},
        {VW * 0.80f, VH * 0.30f},
        {VW * 0.80f, VH * 0.70f},
        {VW * 0.65f, VH * 0.85f},
        {VW * 0.35f, VH * 0.85f},
    };

    private static final int TOTAL_LAPS = 3;
    private static final float TRACK_HALF_W = 38f;

    // ── Player racer ──────────────────────────────────────────
    private float   pX, pY, pAngle, pSpeed;
    private int     pWaypoint = 0;
    private int     pLap      = 0;
    private float   pRaceTime = 0f;
    private boolean pFinished = false;

    // ── AI racers ─────────────────────────────────────────────
    private static final int AI_COUNT = 2;
    private float[] aiX     = new float[AI_COUNT];
    private float[] aiY     = new float[AI_COUNT];
    private float[] aiSpeed = new float[AI_COUNT];
    private float[] aiAngle = new float[AI_COUNT];
    private int[]   aiWp    = new int[AI_COUNT];
    private int[]   aiLap   = new int[AI_COUNT];
    private boolean[] aiDone= new boolean[AI_COUNT];
    private static final float[] AI_TARGET_SPEEDS = {95f, 105f};

    // ── Race state ────────────────────────────────────────────
    private Phase  phase       = Phase.INACTIVE;
    private float  countTimer  = 3f;
    private int    finishPlace = 0;    // 1=first, 2=second etc.
    private float  resultTimer = 0f;

    private final VillageLegends game;
    private final BitmapFont     font;

    // ── Visual trail ──────────────────────────────────────────
    private final List<float[]> pTrail = new ArrayList<>();

    // ─────────────────────────────────────────────────────────
    public TractorRaceGame(VillageLegends game) {
        this.game = game;
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    // ── Start ─────────────────────────────────────────────────
    public void start() {
        if (phase != Phase.INACTIVE) return;

        // Place player at start waypoint
        pX = WAYPOINTS[0][0]; pY = WAYPOINTS[0][1];
        pAngle = 90f; pSpeed = 0f; pWaypoint = 1; pLap = 0;
        pFinished = false; pRaceTime = 0f; pTrail.clear();

        // Place AI racers with offset
        for (int i = 0; i < AI_COUNT; i++) {
            aiX[i]    = WAYPOINTS[0][0] + (i + 1) * 48f;
            aiY[i]    = WAYPOINTS[0][1];
            aiAngle[i]= 90f;
            aiSpeed[i]= 0f;
            aiWp[i]   = 1;
            aiLap[i]  = 0;
            aiDone[i] = false;
        }

        countTimer  = 3f;
        finishPlace = 0;
        phase       = Phase.COUNTDOWN;
        game.audioManager.playSfx("engine_start");
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, MobileControls ctrl) {
        switch (phase) {
            case COUNTDOWN:
                countTimer -= delta;
                if (countTimer <= 0) { phase = Phase.RACING; countTimer = 0; }
                break;

            case RACING:
                pRaceTime += delta;
                updatePlayer(delta, ctrl);
                for (int i = 0; i < AI_COUNT; i++) updateAI(delta, i);
                if (pFinished && finishPlace == 0) {
                    finishPlace = 1;
                    for (int i = 0; i < AI_COUNT; i++) if (aiDone[i]) finishPlace++;
                    phase       = Phase.FINISHED;
                    resultTimer = 4f;
                    handleRaceResult();
                }
                break;

            case FINISHED:
                resultTimer -= delta;
                if (resultTimer <= 0) phase = Phase.INACTIVE;
                break;
        }
    }

    // ── Player driving ────────────────────────────────────────
    private void updatePlayer(float delta, MobileControls ctrl) {
        float steer = ctrl.getJoystickX();
        boolean accel = ctrl.isSprinting();
        boolean brake = ctrl.isBlocking();

        float targetSpeed = accel ? GameConstants.TRACTOR_SPEED : brake ? 0f : pSpeed * 0.85f;
        pSpeed = lerp(pSpeed, targetSpeed, 3f * delta);
        pAngle -= steer * 90f * delta * (pSpeed / GameConstants.TRACTOR_SPEED);

        float rad = pAngle * MathUtils.degreesToRadians;
        pX += MathUtils.cos(rad) * pSpeed * delta;
        pY += MathUtils.sin(rad) * pSpeed * delta;
        pX = MathUtils.clamp(pX, 20, VW - 20);
        pY = MathUtils.clamp(pY, 20, VH - 20);

        // Add to trail
        if (pTrail.size() == 0 || distSq(pX, pY, pTrail.get(pTrail.size()-1)[0], pTrail.get(pTrail.size()-1)[1]) > 400f) {
            pTrail.add(new float[]{pX, pY});
            if (pTrail.size() > 80) pTrail.remove(0);
        }

        // Waypoint progression
        checkWaypoint(pX, pY, pWaypoint, false);
    }

    // ── AI driving ────────────────────────────────────────────
    private void updateAI(float delta, int idx) {
        if (aiDone[idx]) return;
        float[] wp = WAYPOINTS[aiWp[idx] % WAYPOINTS.length];
        float dx  = wp[0] - aiX[idx];
        float dy  = wp[1] - aiY[idx];
        float dist= (float)Math.sqrt(dx*dx + dy*dy);

        // Steer toward waypoint
        float targetAngle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float diff        = targetAngle - aiAngle[idx];
        while (diff >  180) diff -= 360;
        while (diff < -180) diff += 360;
        aiAngle[idx] += diff * 2.5f * delta;

        // Speed with slight variance per opponent
        float tSpeed = AI_TARGET_SPEEDS[idx] * (0.9f + 0.1f * MathUtils.sin(aiRaceTime(idx)));
        aiSpeed[idx] = lerp(aiSpeed[idx], tSpeed, 2.5f * delta);

        float rad = aiAngle[idx] * MathUtils.degreesToRadians;
        aiX[idx] += MathUtils.cos(rad) * aiSpeed[idx] * delta;
        aiY[idx] += MathUtils.sin(rad) * aiSpeed[idx] * delta;

        if (dist < 40f) {
            aiWp[idx] = (aiWp[idx] + 1) % WAYPOINTS.length;
            if (aiWp[idx] == 0) { aiLap[idx]++; if (aiLap[idx] >= TOTAL_LAPS) aiDone[idx] = true; }
        }
    }

    private float aiRaceTime(int i) { return (float) System.currentTimeMillis() * 0.001f + i * 1.3f; }

    private void checkWaypoint(float x, float y, int wp, boolean ai) {
        float[] w = WAYPOINTS[wp % WAYPOINTS.length];
        if (distSq(x, y, w[0], w[1]) < 1600f) {
            if (!ai) {
                pWaypoint = (pWaypoint + 1) % WAYPOINTS.length;
                if (pWaypoint == 0) { pLap++; if (pLap >= TOTAL_LAPS) pFinished = true; }
            }
        }
    }

    // ── Result handling ───────────────────────────────────────
    private void handleRaceResult() {
        if (finishPlace == 1) {
            grantRaceReward(500, 30);
            game.questManager.advanceObjective("s06_tractor_race", "win_tractor_race", 1);
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Race won! ₹500 + 30 rep!"));
        } else {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Finished " + finishPlace + place(finishPlace)));
        }
        game.audioManager.playSfx("quest_complete");
    }

    /** Grant rewards directly through EventBus / save data (no Player ref needed) */
    private void grantRaceReward(int money, int rep) {
        if (game.activeSave != null) {
            game.activeSave.playerData.money      += money;
            game.activeSave.playerData.reputation += rep;
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.MONEY_CHANGED, money));
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.REPUTATION_CHANGED, rep));
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

        // Draw track (oval path)
        sr.setColor(0.3f, 0.25f, 0.15f, 0.9f);
        drawTrackShape(sr);

        // Track centre (grass)
        sr.setColor(0.25f, 0.55f, 0.2f, 0.9f);
        drawTrackInner(sr);

        // Player trail
        sr.setColor(0.8f, 0.7f, 0.2f, 0.4f);
        for (float[] pt : pTrail) sr.circle(pt[0], pt[1], 4, 6);

        // AI tractors
        float[][] aiColors = {{0.8f,0.2f,0.2f,1f},{0.2f,0.4f,0.9f,1f}};
        for (int i = 0; i < AI_COUNT; i++) {
            if (aiDone[i]) continue;
            sr.setColor(aiColors[i][0], aiColors[i][1], aiColors[i][2], aiColors[i][3]);
            drawTractor(sr, aiX[i], aiY[i], aiAngle[i]);
        }

        // Player tractor (yellow)
        sr.setColor(Color.YELLOW);
        drawTractor(sr, pX, pY, pAngle);

        // Waypoint marker
        if (!pFinished) {
            float[] nextWp = WAYPOINTS[pWaypoint % WAYPOINTS.length];
            sr.setColor(1f, 1f, 0f, 0.5f);
            sr.circle(nextWp[0], nextWp[1], 14, 12);
        }

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawHUD(batch);
    }

    private void drawTractor(ShapeRenderer sr, float x, float y, float angleDeg) {
        float rad = angleDeg * MathUtils.degreesToRadians;
        // Body rectangle (simplified)
        sr.rect(x - 14, y - 10, 28, 20);
        // Direction arrow
        sr.setColor(Color.WHITE);
        sr.triangle(x + MathUtils.cos(rad) * 14, y + MathUtils.sin(rad) * 14,
                    x + MathUtils.cos(rad + 2.5f) * 8, y + MathUtils.sin(rad + 2.5f) * 8,
                    x + MathUtils.cos(rad - 2.5f) * 8, y + MathUtils.sin(rad - 2.5f) * 8);
    }

    private void drawTrackShape(ShapeRenderer sr) {
        for (int i = 0; i < WAYPOINTS.length; i++) {
            float[] a = WAYPOINTS[i], b = WAYPOINTS[(i+1) % WAYPOINTS.length];
            // Draw thick segment
            float dx = b[0]-a[0], dy = b[1]-a[1];
            float len = (float)Math.sqrt(dx*dx+dy*dy);
            float nx = -dy/len * TRACK_HALF_W, ny = dx/len * TRACK_HALF_W;
            sr.triangle(a[0]+nx, a[1]+ny, a[0]-nx, a[1]-ny, b[0]+nx, b[1]+ny);
            sr.triangle(b[0]+nx, b[1]+ny, b[0]-nx, b[1]-ny, a[0]-nx, a[1]-ny);
        }
    }

    private void drawTrackInner(ShapeRenderer sr) {
        // Fill inside of oval with grass
        for (int i = 0; i < WAYPOINTS.length - 2; i++) {
            sr.triangle(WAYPOINTS[0][0], WAYPOINTS[0][1],
                        WAYPOINTS[i+1][0], WAYPOINTS[i+1][1],
                        WAYPOINTS[i+2][0], WAYPOINTS[i+2][1]);
        }
    }

    private void drawHUD(SpriteBatch batch) {
        // Countdown
        if (phase == Phase.COUNTDOWN && countTimer > 0) {
            font.getData().setScale(4f);
            font.setColor(Color.RED);
            String cnt = countTimer > 2 ? "3" : countTimer > 1 ? "2" : "GO!";
            font.draw(batch, cnt, VW/2f - 30, VH/2f + 40);
            font.getData().setScale(1.6f);
            return;
        }

        // Race HUD
        font.setColor(Color.WHITE);
        font.draw(batch, "Lap " + Math.min(pLap + 1, TOTAL_LAPS) + "/" + TOTAL_LAPS,
                20, VH - 20);
        font.draw(batch, String.format("%.1fs", pRaceTime), 20, VH - 46);

        // AI lap indicators
        font.getData().setScale(0.9f);
        font.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < AI_COUNT; i++) {
            font.draw(batch, "AI " + (i+1) + ": Lap " + Math.min(aiLap[i]+1, TOTAL_LAPS),
                    VW - 160, VH - 20 - i * 22);
        }
        font.getData().setScale(1.6f);

        // Finish
        if (phase == Phase.FINISHED) {
            font.getData().setScale(2.5f);
            font.setColor(finishPlace == 1 ? Color.LIME : Color.YELLOW);
            font.draw(batch, finishPlace == 1 ? "🏆 1st PLACE!" : finishPlace + place(finishPlace) + " Place",
                    VW/2f - 120, VH/2f + 20);
            font.getData().setScale(1.6f);
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private float distSq(float x1, float y1, float x2, float y2) {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }
    private float lerp(float a, float b, float t) { return a + (b-a)*Math.min(1,t); }
    private String place(int p) { return p==1?"st":p==2?"nd":p==3?"rd":"th"; }

    // ── Getters ───────────────────────────────────────────────
    public boolean isActive() { return phase != Phase.INACTIVE; }
    public void    dispose()  { font.dispose(); }
}
