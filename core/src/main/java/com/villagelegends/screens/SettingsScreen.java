package com.villagelegends.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.VillageLegends;
import com.villagelegends.GameConstants;

/** Settings: music volume, SFX volume, language, control size. */
public class SettingsScreen extends BaseScreen {

    private BitmapFont font;
    private final Rectangle btnBack  = new Rectangle(30, 30, 160, 50);
    private final Rectangle btnMusicUp   = new Rectangle(700, 500, 60, 40);
    private final Rectangle btnMusicDown = new Rectangle(620, 500, 60, 40);
    private final Rectangle btnSfxUp     = new Rectangle(700, 430, 60, 40);
    private final Rectangle btnSfxDown   = new Rectangle(620, 430, 60, 40);

    public SettingsScreen(VillageLegends game) { super(game); }

    @Override public void show() {
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    @Override
    public void render(float delta) {
        clearScreen(0.05f, 0.05f, 0.1f);
        handleInput();
        viewport.apply();
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        font.setColor(Color.YELLOW);
        font.getData().setScale(2.2f);
        font.draw(game.batch, "Settings", 540f, 660f);
        font.getData().setScale(1.6f);

        font.setColor(Color.WHITE);
        font.draw(game.batch, "Music Volume:  " + (int)(game.audioManager.getMusicVol()*100) + "%", 300f, 520f);
        font.draw(game.batch, "SFX Volume:    " + (int)(game.audioManager.getSfxVol()*100)   + "%", 300f, 450f);
        font.draw(game.batch, "Language:      " + game.language.toUpperCase(), 300f, 380f);
        font.draw(game.batch, "Controls:      Standard", 300f, 310f);

        font.setColor(Color.CYAN);
        font.draw(game.batch, "[ - ]", 625f, 510f);
        font.draw(game.batch, "[ + ]", 705f, 510f);
        font.draw(game.batch, "[ - ]", 625f, 440f);
        font.draw(game.batch, "[ + ]", 705f, 440f);

        font.setColor(Color.CORAL);
        font.draw(game.batch, "<  Back", 35f, 70f);

        game.batch.end();
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        com.badlogic.gdx.math.Vector3 v = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        uiCamera.unproject(v);
        if (btnBack.contains(v.x, v.y))      game.setScreen(new MainMenuScreen(game));
        if (btnMusicUp.contains(v.x, v.y))   game.audioManager.adjustMusicVol(0.1f);
        if (btnMusicDown.contains(v.x, v.y)) game.audioManager.adjustMusicVol(-0.1f);
        if (btnSfxUp.contains(v.x, v.y))     game.audioManager.adjustSfxVol(0.1f);
        if (btnSfxDown.contains(v.x, v.y))   game.audioManager.adjustSfxVol(-0.1f);
    }

    @Override public void dispose() { font.dispose(); }
}
