package com.villagelegends.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.GL20;
import com.villagelegends.VillageLegends;
import com.villagelegends.GameConstants;

/**
 * All screens extend BaseScreen which owns a shared FitViewport
 * and enforces the standard clear/resize pattern.
 */
public abstract class BaseScreen implements Screen {

    protected final VillageLegends game;
    protected OrthographicCamera   uiCamera;
    protected Viewport             viewport;

    public BaseScreen(VillageLegends game) {
        this.game   = game;
        uiCamera    = new OrthographicCamera();
        viewport    = new FitViewport(GameConstants.VIRTUAL_WIDTH,
                                      GameConstants.VIRTUAL_HEIGHT, uiCamera);
        uiCamera.setToOrtho(false, GameConstants.VIRTUAL_WIDTH,
                                   GameConstants.VIRTUAL_HEIGHT);
    }

    /** Clear with a solid colour and apply viewport. Call at start of render(). */
    protected void clearScreen(float r, float g, float b) {
        Gdx.gl.glClearColor(r, g, b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    // ── Default Screen callbacks ─────────────────────────────
    @Override public void show()   {}
    @Override public void hide()   {}
    @Override public void pause()  {}
    @Override public void resume() {}

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void dispose() {}
}
