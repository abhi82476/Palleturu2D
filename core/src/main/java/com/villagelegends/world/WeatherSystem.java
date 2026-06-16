package com.villagelegends.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.VillageLegends;

import java.util.ArrayList;
import java.util.List;

/**
 * WeatherSystem manages weather states and renders screen-space
 * particle effects (rain streaks, fog overlay, dust).
 *
 * Transitions are probability-driven per in-game hour so the world
 * feels alive without scripted weather.
 *
 * Weather types:
 *   CLEAR, CLOUDY, LIGHT_RAIN, HEAVY_RAIN, FOG, DUST_STORM, FESTIVAL_FIREWORKS
 */
public class WeatherSystem {

    public enum Weather {
        CLEAR, CLOUDY, LIGHT_RAIN, HEAVY_RAIN, FOG, DUST_STORM, FESTIVAL_FIREWORKS
    }

    private Weather current    = Weather.CLEAR;
    private Weather target     = Weather.CLEAR;
    private float   transition = 0f;     // 0..1 blend to target
    private float   timer      = 0f;     // time until next change check
    private float   intensity  = 0f;     // 0..1

    // ── Rain Particles ────────────────────────────────────────
    private static final int MAX_DROPS = 300;
    private final List<RainDrop> drops = new ArrayList<>();

    // ── Fog overlay alpha ─────────────────────────────────────
    private float fogAlpha = 0f;

    private VillageLegends game;

    // ─────────────────────────────────────────────────────────
    public WeatherSystem() {
        for (int i = 0; i < MAX_DROPS; i++) {
            drops.add(new RainDrop(false));
        }
    }

    public void update(float delta, float gameHour) {
        timer -= delta;
        if (timer <= 0) {
            rollWeatherChange(gameHour);
            timer = 60f + MathUtils.random(120f);   // check every 1-3 game minutes
        }

        // Blend transition
        if (current != target) {
            transition = Math.min(1f, transition + delta * 0.5f);
            if (transition >= 1f) { current = target; transition = 0f; }
        }

        updateIntensity(delta);

        // Update rain particles
        boolean raining = (current == Weather.LIGHT_RAIN || current == Weather.HEAVY_RAIN);
        if (raining) {
            for (RainDrop d : drops) d.update(delta, intensity);
        }

        // Fog alpha
        float targetFog = (current == Weather.FOG) ? 0.35f :
                          (current == Weather.CLOUDY) ? 0.10f : 0f;
        fogAlpha = lerp(fogAlpha, targetFog, delta * 2f);
    }

    private void rollWeatherChange(float gameHour) {
        // Higher rain chance at dawn and evening
        float rainChance = 0.12f;
        if (gameHour >= 4  && gameHour < 8)  rainChance = 0.20f;
        if (gameHour >= 17 && gameHour < 22) rainChance = 0.25f;

        float r = MathUtils.random();
        if      (r < rainChance * 0.5f)  target = Weather.HEAVY_RAIN;
        else if (r < rainChance)          target = Weather.LIGHT_RAIN;
        else if (r < rainChance + 0.08f)  target = Weather.FOG;
        else if (r < rainChance + 0.15f)  target = Weather.CLOUDY;
        else                              target = Weather.CLEAR;
    }

    private void updateIntensity(float delta) {
        float targetIntensity;
        if      (current == Weather.HEAVY_RAIN)  targetIntensity = 1.0f;
        else if (current == Weather.LIGHT_RAIN)  targetIntensity = 0.5f;
        else if (current == Weather.FOG)          targetIntensity = 0.8f;
        else if (current == Weather.CLOUDY)       targetIntensity = 0.4f;
        else if (current == Weather.DUST_STORM)   targetIntensity = 0.9f;
        else                                      targetIntensity = 0f;
        intensity = lerp(intensity, targetIntensity, delta * 1.5f);
    }

    public void setWeather(Weather w) {
        target = w;
    }

    // ── Draw (screen-space overlay) ───────────────────────────
    public void draw(SpriteBatch batch, OrthographicCamera cam) {
        if (intensity < 0.01f && fogAlpha < 0.01f) return;

        // Fog / cloud overlay drawn in screen space
        if (fogAlpha > 0.01f) {
            batch.end();
            ShapeRenderer sr = VillageLegends.get().shapeRenderer;
            sr.setProjectionMatrix(cam.combined);
            com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.8f, 0.85f, 0.9f, fogAlpha);
            sr.rect(cam.position.x - cam.viewportWidth / 2f,
                    cam.position.y - cam.viewportHeight / 2f,
                    cam.viewportWidth, cam.viewportHeight);
            sr.end();
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            batch.begin();
        }

        // Rain drops
        boolean raining = (current == Weather.LIGHT_RAIN || current == Weather.HEAVY_RAIN);
        if (raining && intensity > 0.05f) {
            batch.end();
            ShapeRenderer sr = VillageLegends.get().shapeRenderer;
            sr.setProjectionMatrix(cam.combined);
            com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            sr.begin(ShapeRenderer.ShapeType.Line);
            float alpha = intensity * 0.55f;
            sr.setColor(0.6f, 0.75f, 0.9f, alpha);
            float offX = cam.position.x - cam.viewportWidth  / 2f;
            float offY = cam.position.y - cam.viewportHeight / 2f;
            int visibleDrops = (int)(drops.size() * intensity);
            for (int i = 0; i < visibleDrops; i++) {
                RainDrop d = drops.get(i);
                float wx = offX + d.x * cam.viewportWidth;
                float wy = offY + d.y * cam.viewportHeight;
                sr.line(wx, wy, wx + d.vx * 0.08f, wy + d.vy * 0.08f);
            }
            sr.end();
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            batch.begin();
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public Weather getWeather()   { return current; }
    public float   getIntensity() { return intensity; }
    public boolean isRaining()    { return current == Weather.LIGHT_RAIN
                                        || current == Weather.HEAVY_RAIN; }
    public boolean isFoggy()      { return current == Weather.FOG; }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    // ── Rain drop ─────────────────────────────────────────────
    private static class RainDrop {
        float x, y;      // 0..1 normalised screen coords
        float vx = -0.04f;
        float vy = -0.5f;

        RainDrop(boolean active) { reset(); }

        void reset() {
            x  = MathUtils.random();
            y  = MathUtils.random();
        }

        void update(float delta, float intensity) {
            float speed = 0.3f + intensity * 0.4f;
            x += vx * speed * delta;
            y += vy * speed * delta;
            if (y < 0 || x < 0) reset();
        }
    }
}
