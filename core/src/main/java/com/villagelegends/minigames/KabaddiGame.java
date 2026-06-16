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
 * KabaddiGame — raid-and-tag turn-based mini-game.
 *
 * Rules (simplified):
 *  - Player is the raider; crosses into opponent half while chanting "Kabaddi".
 *  - Touch as many opponents as possible before running out of breath.
 *  - Opponents try to tackle (touch) the raider to trap them.
 *  - Score: 1 point per opponent tagged, +2 bonus for tagging all 7.
 *
 * Controls:
 *  - Joystick: move raider
 *  - ATTACK: attempt tag on nearby opponent
 *  - SPRINT: use stamina to evade tackle
 *
 * The mini-game runs for 3 rounds. Highest score wins.
 */
public class KabaddiGame {

    private static final float VW    = GameConstants.VIRTUAL_WIDTH;
    private static final float VH    = GameConstants.VIRTUAL_HEIGHT;
    private static final int   OPPONENTS = 7;
    private static final float FIELD_W   = VW * 0.80f;
    private static final float FIELD_H   = VH * 0.65f;
    private static final float FIELD_X   = (VW - FIELD_W) / 2f;
    private static final float FIELD_Y   = (VH - FIELD_H) / 2f;

    public enum Phase { INACTIVE, BRIEFING, RAIDING, DEFENDING, ROUND_END, GAME_OVER }

    private Phase   phase        = Phase.INACTIVE;
    private int     round        = 0;
    private int     playerScore  = 0;
    private int     aiScore      = 0;
    private int     maxRounds    = 3;
    private float   breath       = 1.0f;         // raider's breath 0..1
    private float   phaseTimer   = 0f;

    // Raider position (player)
    private float   raiderX, raiderY;
    private boolean raiderInOpponentHalf = false;

    // Opponents (simple rectangles)
    private float[]  oppX = new float[OPPONENTS];
    private float[]  oppY = new float[OPPONENTS];
    private boolean[] oppTagged = new boolean[OPPONENTS];
    private boolean[] oppActive = new boolean[OPPONENTS];

    private final VillageLegends game;
    private final BitmapFont     font;

    public KabaddiGame(VillageLegends game) {
        this.game = game;
        font      = new BitmapFont();
        font.getData().setScale(1.4f);
    }

    public void start() {
        phase       = Phase.BRIEFING;
        round       = 1;
        playerScore = 0;
        aiScore     = 0;
        phaseTimer  = 2.5f;
        setupRound();
    }

    private void setupRound() {
        // Raider starts at centre-left
        raiderX = FIELD_X + FIELD_W * 0.25f;
        raiderY = FIELD_Y + FIELD_H / 2f;
        breath  = 1.0f;

        // Place opponents in right half
        for (int i = 0; i < OPPONENTS; i++) {
            oppX[i]      = FIELD_X + FIELD_W * 0.58f + MathUtils.random(FIELD_W * 0.35f);
            oppY[i]      = FIELD_Y + 40 + (i * (FIELD_H - 80) / OPPONENTS);
            oppTagged[i] = false;
            oppActive[i] = true;
        }
    }

    public void update(float delta, com.villagelegends.ui.MobileControls ctrl) {
        phaseTimer -= delta;
        switch (phase) {
            case BRIEFING:
                if (phaseTimer <= 0) phase = Phase.RAIDING;
                break;
            case RAIDING:
                updateRaiding(delta, ctrl);
                break;
            case ROUND_END:
                if (phaseTimer <= 0) {
                    round++;
                    if (round > maxRounds) { phase = Phase.GAME_OVER; break; }
                    setupRound(); phase = Phase.RAIDING;
                }
                break;
        }
    }

    private void updateRaiding(float delta, com.villagelegends.ui.MobileControls ctrl) {
        float speed = 160f * delta;
        raiderX += ctrl.getJoystickX() * speed;
        raiderY += ctrl.getJoystickY() * speed;

        // Clamp to field
        raiderX = MathUtils.clamp(raiderX, FIELD_X, FIELD_X + FIELD_W);
        raiderY = MathUtils.clamp(raiderY, FIELD_Y, FIELD_Y + FIELD_H);

        raiderInOpponentHalf = raiderX > FIELD_X + FIELD_W / 2f;

        // Breath drains while in opponent half
        if (raiderInOpponentHalf) {
            breath -= delta * 0.12f;
            if (breath <= 0) { endRaid(false); return; }
        }

        // Tag check
        if (ctrl.justAttacked() && raiderInOpponentHalf) {
            for (int i = 0; i < OPPONENTS; i++) {
                if (!oppActive[i] || oppTagged[i]) continue;
                float dx = raiderX - oppX[i], dy = raiderY - oppY[i];
                if (dx * dx + dy * dy < 40 * 40) {
                    oppTagged[i] = true;
                    playerScore++;
                    game.audioManager.playSfx("hit_flesh");
                }
            }
        }

        // All tagged or returned to own half
        boolean allTagged = true;
        for (boolean t : oppTagged) if (!t) { allTagged = false; break; }
        if (allTagged) { if (raiderInOpponentHalf) playerScore += 2; endRaid(true); }
        if (!raiderInOpponentHalf && phase == Phase.RAIDING) endRaid(true);

        // Simple AI opponents close in
        for (int i = 0; i < OPPONENTS; i++) {
            if (!oppActive[i] || oppTagged[i]) continue;
            float dx = raiderX - oppX[i], dy = raiderY - oppY[i];
            float dist = (float)Math.sqrt(dx*dx+dy*dy);
            if (raiderInOpponentHalf && dist < 200f) {
                oppX[i] += (dx/dist) * 55f * delta;
                oppY[i] += (dy/dist) * 55f * delta;
                // Tackle
                if (dist < 20f) { endRaid(false); return; }
            }
        }
    }

    private void endRaid(boolean success) {
        phase      = Phase.ROUND_END;
        phaseTimer = 2.0f;
        if (!success) aiScore += 2;
        game.audioManager.playSfx(success ? "quest_complete" : "player_hurt");
    }

    public void draw(SpriteBatch batch, float delta) {
        if (phase == Phase.INACTIVE) return;
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Field
        sr.setColor(0.2f, 0.55f, 0.2f, 0.9f); sr.rect(FIELD_X, FIELD_Y, FIELD_W, FIELD_H);
        // Centre line
        sr.setColor(Color.WHITE); sr.rect(FIELD_X + FIELD_W/2f - 2, FIELD_Y, 4, FIELD_H);
        // Raider
        sr.setColor(Color.YELLOW); sr.circle(raiderX, raiderY, 14, 12);
        // Opponents
        for (int i = 0; i < OPPONENTS; i++) {
            sr.setColor(oppTagged[i] ? Color.GRAY : Color.RED);
            sr.circle(oppX[i], oppY[i], 12, 10);
        }
        // Breath bar
        sr.setColor(0.1f, 0.1f, 0.1f, 0.8f); sr.rect(FIELD_X, FIELD_Y + FIELD_H + 10, FIELD_W, 14);
        sr.setColor(breath > 0.4f ? Color.CYAN : Color.RED);
        sr.rect(FIELD_X, FIELD_Y + FIELD_H + 10, FIELD_W * breath, 14);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "KABADDI!  Round " + round + "/" + maxRounds, FIELD_X + 20, FIELD_Y + FIELD_H + 50);
        font.draw(batch, "You: " + playerScore + "    AI: " + aiScore, FIELD_X + 20, FIELD_Y + FIELD_H + 28);
        if (phase == Phase.GAME_OVER) {
            font.getData().setScale(2f);
            font.setColor(playerScore > aiScore ? Color.LIME : Color.RED);
            font.draw(batch, playerScore > aiScore ? "YOU WIN!" : "YOU LOSE!",
                    VW/2f - 80, VH/2f + 20);
            font.getData().setScale(1.4f);
            if (playerScore > aiScore) {
                game.questManager.advanceObjective("s08_kabaddi", "win_kabaddi_match", 1);
            }
        }
    }

    public boolean isActive() { return phase != Phase.INACTIVE; }
    public void    dispose()  { font.dispose(); }
}
