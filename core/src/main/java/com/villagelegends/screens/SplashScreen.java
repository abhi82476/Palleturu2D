package com.villagelegends.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.villagelegends.VillageLegends;
import com.villagelegends.GameConstants;

/**
 * Splash screen: fades in studio title, then transitions to MainMenuScreen.
 * Also starts background asset loading in parallel.
 */
public class SplashScreen extends BaseScreen {

    private static final float FADE_IN   = 1.2f;
    private static final float HOLD_TIME = 1.5f;
    private static final float FADE_OUT  = 0.8f;
    private static final float TOTAL     = FADE_IN + HOLD_TIME + FADE_OUT;

    private float    timer  = 0;
    private float    alpha  = 0;
    private BitmapFont font;

    // Would load an actual logo texture in production
    // private Texture logoTexture;

    public SplashScreen(VillageLegends game) {
        super(game);
    }

    @Override
    public void show() {
        font = new BitmapFont();
        font.getData().setScale(2f);
        // Start background loading of all assets
        queueAssets();
    }

    private void queueAssets() {
        // Queue all textures/sounds; they load while splash plays
        // game.assetManager.load("textures/ui/menu_bg.png", Texture.class);
        // … (full asset list added here in production build)
    }

    @Override
    public void render(float delta) {
        clearScreen(0.05f, 0.05f, 0.08f);
        game.assetManager.update();   // advance loading

        timer += delta;

        // Compute alpha
        if (timer < FADE_IN) {
            alpha = timer / FADE_IN;
        } else if (timer < FADE_IN + HOLD_TIME) {
            alpha = 1f;
        } else {
            alpha = 1f - (timer - FADE_IN - HOLD_TIME) / FADE_OUT;
        }
        alpha = Math.max(0, Math.min(1, alpha));

        // Draw studio name centred
        viewport.apply();
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();
        font.setColor(0.9f, 0.7f, 0.2f, alpha);
        String title  = "Village Legends 2D";
        String studio = "Pixel South Studios";
        GlyphLayout gl = new GlyphLayout(font, title);
        float cx = GameConstants.VIRTUAL_WIDTH  / 2f - gl.width  / 2f;
        float cy = GameConstants.VIRTUAL_HEIGHT / 2f + gl.height / 2f + 20;
        font.draw(game.batch, title,  cx, cy);
        font.setColor(0.6f, 0.6f, 0.6f, alpha * 0.8f);
        font.getData().setScale(1.2f);
        GlyphLayout gl2 = new GlyphLayout(font, studio);
        font.draw(game.batch, studio,
                GameConstants.VIRTUAL_WIDTH  / 2f - gl2.width / 2f,
                GameConstants.VIRTUAL_HEIGHT / 2f - 20);
        game.batch.end();

        if (timer >= TOTAL) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
