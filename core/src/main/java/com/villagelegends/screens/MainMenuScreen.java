package com.villagelegends.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.VillageLegends;
import com.villagelegends.GameConstants;
import com.villagelegends.data.GameSaveData;

/**
 * Main Menu screen.
 *
 * Shows title, three save-slot thumbnails, settings, and language buttons.
 * Touch input is handled directly via Gdx.input since LibGDX Scene2D Skin
 * requires compiled atlas assets; in production swap to Scene2D Stage+Skin.
 */
public class MainMenuScreen extends BaseScreen {

    // Button rects (in virtual resolution space)
    private final Rectangle btnNewGame   = new Rectangle(490, 380, 300, 60);
    private final Rectangle btnContinue  = new Rectangle(490, 300, 300, 60);
    private final Rectangle btnSettings  = new Rectangle(490, 220, 300, 60);
    private final Rectangle btnQuit      = new Rectangle(490, 140, 300, 60);
    private final Rectangle btnLangTE    = new Rectangle(20, 20, 60, 36);
    private final Rectangle btnLangEN    = new Rectangle(90, 20, 60, 36);
    private final Rectangle btnLangHI    = new Rectangle(160, 20, 60, 36);

    private BitmapFont titleFont;
    private BitmapFont btnFont;
    private float      animTimer = 0;
    private boolean    hasSave   = false;

    public MainMenuScreen(VillageLegends game) {
        super(game);
    }

    @Override
    public void show() {
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.5f);

        btnFont = new BitmapFont();
        btnFont.getData().setScale(1.8f);

        // Check whether any save exists
        for (int i = 0; i < GameConstants.SAVE_SLOTS; i++) {
            if (game.saveManager.slotHasSave(i)) { hasSave = true; break; }
        }

        game.audioManager.playMenuMusic();
    }

    @Override
    public void render(float delta) {
        animTimer += delta;
        clearScreen(0.06f, 0.04f, 0.10f);
        handleInput();

        viewport.apply();
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        drawBackground();
        drawTitle();
        drawButton(btnNewGame,  "New Game",   Color.GOLD);
        drawButton(btnContinue, "Continue",   hasSave ? Color.GREEN : Color.GRAY);
        drawButton(btnSettings, "Settings",   Color.CYAN);
        drawButton(btnQuit,     "Quit",       Color.FIREBRICK);
        drawLangButtons();
        drawVersion();

        game.batch.end();
    }

    private void drawBackground() {
        // Pulsing gradient effect via tinted fills
        float pulse = 0.5f + 0.5f * (float) Math.sin(animTimer * 0.5);
        game.batch.setColor(0.1f + pulse * 0.05f, 0.06f, 0.15f, 1f);
        // In production: draw scrolling village BG texture here
        game.batch.setColor(Color.WHITE);
    }

    private void drawTitle() {
        String line1 = "Village Legends";
        String line2 = "2D";
        titleFont.setColor(1f, 0.85f, 0.2f, 1f);
        GlyphLayout gl = new GlyphLayout(titleFont, line1);
        titleFont.draw(game.batch, line1,
                GameConstants.VIRTUAL_WIDTH / 2f - gl.width / 2f,
                620f);
        gl = new GlyphLayout(titleFont, line2);
        titleFont.setColor(0.9f, 0.5f, 0.1f, 1f);
        titleFont.draw(game.batch, line2,
                GameConstants.VIRTUAL_WIDTH / 2f - gl.width / 2f,
                560f);

        // Telugu subtitle
        btnFont.setColor(0.8f, 0.8f, 0.8f, 0.8f);
        btnFont.getData().setScale(1.0f);
        btnFont.draw(game.batch, "విలేజ్ లెజెండ్స్", 550f, 530f);
        btnFont.getData().setScale(1.8f);
    }

    private void drawButton(Rectangle r, String label, Color color) {
        // Box – end the SpriteBatch so ShapeRenderer can take over
        game.batch.end();

        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(color.r * 0.2f, color.g * 0.2f, color.b * 0.2f, 0.85f);
        game.shapeRenderer.rect(r.x, r.y, r.width, r.height);
        game.shapeRenderer.end();

        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
        game.shapeRenderer.setColor(color);
        game.shapeRenderer.rect(r.x, r.y, r.width, r.height);
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        btnFont.setColor(color);
        GlyphLayout gl = new GlyphLayout(btnFont, label);
        btnFont.draw(game.batch, label,
                r.x + r.width / 2f - gl.width / 2f,
                r.y + r.height / 2f + gl.height / 2f);
    }

    private void drawLangButtons() {
        String[] langs  = {"TE", "EN", "HI"};
        Rectangle[] rects = {btnLangTE, btnLangEN, btnLangHI};
        for (int i = 0; i < 3; i++) {
            boolean active = (langs[i].toLowerCase().equals(game.language));
            drawButton(rects[i], langs[i], active ? Color.YELLOW : Color.LIGHT_GRAY);
        }
    }

    private void drawVersion() {
        btnFont.getData().setScale(0.8f);
        btnFont.setColor(0.4f, 0.4f, 0.4f, 1f);
        btnFont.draw(game.batch, "v1.0.0  •  Android 8+",
                GameConstants.VIRTUAL_WIDTH - 220f, 24f);
        btnFont.getData().setScale(1.8f);
    }

    // ── Touch Input ──────────────────────────────────────────
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;

        // Unproject touch coordinates to virtual screen space
        float tx = Gdx.input.getX();
        float ty = Gdx.input.getY();
        // Convert to viewport coords
        com.badlogic.gdx.math.Vector3 v = new com.badlogic.gdx.math.Vector3(tx, ty, 0);
        uiCamera.unproject(v);
        float vx = v.x, vy = v.y;

        if (btnNewGame.contains(vx, vy)) {
            game.newGame(0);
        } else if (btnContinue.contains(vx, vy) && hasSave) {
            showSaveSlotPicker();
        } else if (btnSettings.contains(vx, vy)) {
            game.setScreen(new SettingsScreen(game));
        } else if (btnQuit.contains(vx, vy)) {
            Gdx.app.exit();
        } else if (btnLangTE.contains(vx, vy)) {
            game.language = "te";
        } else if (btnLangEN.contains(vx, vy)) {
            game.language = "en";
        } else if (btnLangHI.contains(vx, vy)) {
            game.language = "hi";
        }
    }

    private void showSaveSlotPicker() {
        // Quick-load slot 0 for now; production shows a slot picker overlay
        game.loadGame(0);
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        btnFont.dispose();
    }
}
