package com.villagelegends.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;

/**
 * MobileControls renders and polls an on-screen virtual gamepad.
 *
 * Layout (landscape):
 *  LEFT side  – Joystick (move / steer)
 *  RIGHT side – ① Attack  ② Interact/Action  ③ Sprint  ④ Dodge
 *               ⑤ Inventory  ⑥ Pause
 *
 * Supports up to 5 simultaneous touch pointers (LibGDX multi-touch).
 * The joystick tracks its initial touch-down position so the thumb
 * never has to re-centre before moving.
 *
 * All coordinates are in virtual-screen space (1280 × 720).
 */
public class MobileControls {

    // ── Virtual screen dimensions ─────────────────────────────
    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // ── Joystick config ───────────────────────────────────────
    private static final float JS_OUTER_R   = 80f;
    private static final float JS_INNER_R   = 36f;
    private static final float JS_BASE_X    = 140f;
    private static final float JS_BASE_Y    = 140f;

    // Joystick runtime state
    private float jsBaseX = JS_BASE_X, jsBaseY = JS_BASE_Y;
    private float jsKnobX = JS_BASE_X, jsKnobY = JS_BASE_Y;
    private int   jsPointer = -1;
    private boolean joystickActive = false;

    // Derived joystick axis (-1..1)
    private float axisX = 0, axisY = 0;

    // ── Button positions (centre x, centre y, radius) ─────────
    private static final float BTN_R = 42f;
    // Right cluster
    private final float[] btnAttack    = {VW - 120, 130, BTN_R};
    private final float[] btnInteract  = {VW - 210, 100, BTN_R};
    private final float[] btnSprint    = {VW - 310, 100, BTN_R};
    private final float[] btnDodge     = {VW - 210, 195, BTN_R};
    // Top-right utility
    private final float[] btnInventory = {VW - 60,  VH - 60, 32f};
    private final float[] btnPause     = {VW - 130, VH - 60, 32f};
    private final float[] btnMap       = {VW - 200, VH - 60, 32f};
    private final float[] btnQuest     = {VW - 270, VH - 60, 32f};

    // ── One-frame pulse flags ─────────────────────────────────
    private boolean justAttacked   = false;
    private boolean justInteracted = false;
    private boolean justDodged     = false;
    private boolean justPaused     = false;
    private boolean justInventory  = false;
    private boolean justMap        = false;
    private boolean justQuest      = false;
    private boolean sprinting      = false;
    private boolean blocking       = false;

    // ── Accelerator / brake for vehicles ─────────────────────
    private boolean accelerating   = false;
    private boolean braking        = false;

    // ── Input pointer tracking ────────────────────────────────
    private static final int MAX_POINTERS = 5;
    private final int[]     btnPointers   = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};

    private final VillageLegends   game;
    private final InputAdapter     inputAdapter;

    // ── Viewport unproject helper ─────────────────────────────
    private final com.badlogic.gdx.math.Vector3 tmpVec = new com.badlogic.gdx.math.Vector3();
    private com.badlogic.gdx.utils.viewport.FitViewport uiViewport;

    // ─────────────────────────────────────────────────────────
    public MobileControls(VillageLegends game) {
        this.game = game;
        inputAdapter = buildInputAdapter();
    }

    // ── Input adapter ─────────────────────────────────────────
    private InputAdapter buildInputAdapter() {
        return new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 v = unproject(screenX, screenY);
                float vx = v.x, vy = v.y;

                // Joystick region (left third of screen)
                if (vx < VW * 0.35f && !joystickActive) {
                    joystickActive = true;
                    jsPointer = pointer;
                    jsBaseX   = vx;  jsBaseY = vy;
                    jsKnobX   = vx;  jsKnobY = vy;
                    return true;
                }

                // Button hits (right side)
                if (hitBtn(vx, vy, btnAttack))    { justAttacked   = true; btnPointers[0] = pointer; blocking = false; return true; }
                if (hitBtn(vx, vy, btnInteract))  { justInteracted = true; btnPointers[1] = pointer; return true; }
                if (hitBtn(vx, vy, btnDodge))     { justDodged     = true; btnPointers[2] = pointer; return true; }
                if (hitBtn(vx, vy, btnSprint))    { sprinting      = true; btnPointers[3] = pointer; return true; }
                if (hitBtn(vx, vy, btnInventory)) { justInventory  = true; return true; }
                if (hitBtn(vx, vy, btnPause))     { justPaused     = true; return true; }
                if (hitBtn(vx, vy, btnMap))       { justMap        = true; return true; }
                if (hitBtn(vx, vy, btnQuest))     { justQuest      = true; return true; }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (joystickActive && pointer == jsPointer) {
                    Vector2 v = unproject(screenX, screenY);
                    float dx = v.x - jsBaseX;
                    float dy = v.y - jsBaseY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > JS_OUTER_R) {
                        dx = (dx / dist) * JS_OUTER_R;
                        dy = (dy / dist) * JS_OUTER_R;
                    }
                    jsKnobX = jsBaseX + dx;
                    jsKnobY = jsBaseY + dy;
                    axisX   = dx / JS_OUTER_R;
                    axisY   = dy / JS_OUTER_R;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (joystickActive && pointer == jsPointer) {
                    joystickActive = false;
                    jsPointer = -1;
                    axisX = 0;  axisY = 0;
                    jsKnobX = JS_BASE_X; jsKnobY = JS_BASE_Y;
                    jsBaseX = JS_BASE_X; jsBaseY = JS_BASE_Y;
                    return true;
                }
                // Release sprint / block
                for (int i = 0; i < btnPointers.length; i++) {
                    if (btnPointers[i] == pointer) {
                        if (i == 3) sprinting = false;
                        btnPointers[i] = -1;
                    }
                }
                return false;
            }
        };
    }

    // ── Per-frame update (consume one-shot flags) ─────────────
    public void update(float delta) {
        // One-shot flags are consumed in getter; nothing needed here.
    }

    public void draw(SpriteBatch batch, float delta) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // ── Joystick outer ring ───────────────────────────────
        sr.setColor(1f, 1f, 1f, 0.18f);
        sr.circle(jsBaseX, jsBaseY, JS_OUTER_R, 32);

        // ── Joystick knob ─────────────────────────────────────
        sr.setColor(1f, 1f, 1f, joystickActive ? 0.55f : 0.28f);
        sr.circle(joystickActive ? jsKnobX : jsBaseX,
                  joystickActive ? jsKnobY : jsBaseY, JS_INNER_R, 24);

        // ── Action buttons ────────────────────────────────────
        drawBtn(sr, btnAttack,   new Color(0.9f,0.2f,0.2f,0.7f));   // Red  – Attack
        drawBtn(sr, btnInteract, new Color(0.2f,0.8f,0.2f,0.7f));   // Green – Action
        drawBtn(sr, btnSprint,   new Color(0.2f,0.6f,1.0f,0.7f));   // Blue – Sprint
        drawBtn(sr, btnDodge,    new Color(0.9f,0.7f,0.1f,0.7f));   // Yellow – Dodge

        // ── Utility buttons (top-right, smaller) ─────────────
        drawBtn(sr, btnInventory, new Color(0.6f,0.4f,0.9f,0.65f)); // Purple – Inv
        drawBtn(sr, btnPause,     new Color(0.5f,0.5f,0.5f,0.65f)); // Grey – Pause
        drawBtn(sr, btnMap,       new Color(0.2f,0.7f,0.8f,0.65f)); // Teal – Map
        drawBtn(sr, btnQuest,     new Color(1.0f,0.6f,0.0f,0.65f)); // Orange – Quest

        sr.end();

        // ── Button outlines ───────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(1f, 1f, 1f, 0.4f);
        sr.circle(jsBaseX, jsBaseY, JS_OUTER_R, 32);
        outlineBtn(sr, btnAttack);  outlineBtn(sr, btnInteract);
        outlineBtn(sr, btnSprint);  outlineBtn(sr, btnDodge);
        outlineBtn(sr, btnInventory); outlineBtn(sr, btnPause);
        outlineBtn(sr, btnMap);     outlineBtn(sr, btnQuest);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Labels
        batch.begin();
        drawLabels(batch);
    }

    private void drawBtn(ShapeRenderer sr, float[] btn, Color c) {
        sr.setColor(c);
        sr.circle(btn[0], btn[1], btn[2], 20);
    }

    private void outlineBtn(ShapeRenderer sr, float[] btn) {
        sr.circle(btn[0], btn[1], btn[2], 20);
    }

    private void drawLabels(SpriteBatch batch) {
        // In production: use BitmapFont; here just noted by position.
        // ⚔ A  ✋ B  🏃 S  🔄 D  🎒 I  ⏸ P  🗺 M  📋 Q
    }

    // ── Unproject screen coords to virtual coords ─────────────
    private final Vector2 tmpResult = new Vector2();
    private Vector2 unproject(int screenX, int screenY) {
        float scaleX = VW / (float) Gdx.graphics.getWidth();
        float scaleY = VH / (float) Gdx.graphics.getHeight();
        tmpResult.set(screenX * scaleX, (Gdx.graphics.getHeight() - screenY) * scaleY);
        return tmpResult;
    }

    private boolean hitBtn(float vx, float vy, float[] btn) {
        float dx = vx - btn[0], dy = vy - btn[1];
        return dx * dx + dy * dy <= btn[2] * btn[2];
    }

    // ── Getters – consumed as one-shot then reset ─────────────
    public float getJoystickX()      { return axisX; }
    public float getJoystickY()      { return axisY; }
    public boolean isSprinting()     { return sprinting; }
    public boolean isBlocking()      { return blocking; }
    public boolean isAccelerating()  { return sprinting; }   // reuse sprint for accel
    public boolean isBraking()       { return blocking; }

    public boolean justAttacked()   { boolean v = justAttacked;   justAttacked   = false; return v; }
    public boolean justInteracted() { boolean v = justInteracted; justInteracted = false; return v; }
    public boolean justDodged()     { boolean v = justDodged;     justDodged     = false; return v; }
    public boolean justPaused()     { boolean v = justPaused;     justPaused     = false; return v; }
    public boolean justInventory()  { boolean v = justInventory;  justInventory  = false; return v; }
    public boolean justMap()        { boolean v = justMap;        justMap        = false; return v; }
    public boolean justQuest()      { boolean v = justQuest;      justQuest      = false; return v; }

    public InputProcessor getInputProcessor() { return inputAdapter; }

    public void dispose() {}
}
