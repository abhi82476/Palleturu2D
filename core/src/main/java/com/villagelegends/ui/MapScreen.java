package com.villagelegends.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.utils.LocalizationManager;

/**
 * MapScreen — full-screen world map overlay.
 *
 * Shows all 6 regions as coloured blocks with connecting roads.
 * Tapping a region shows its name and fast-travel option (if unlocked).
 * The player's current region is highlighted with an animated pulse.
 *
 * Map layout (top-down view):
 *
 *    [Forest]      [Highway]
 *       |              |
 *    [Village]    [Town]
 *       |              |
 *    [Lake]      [Farmlands]
 */
public class MapScreen {

    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // ── Region map positions (screen space) ───────────────────
    private static final RegionNode[] REGIONS = {
        new RegionNode("main_village", "Main Village",  new Color(0.3f,0.7f,0.25f,1f), 380,340, 220,120),
        new RegionNode("farmlands",    "Farmlands",     new Color(0.7f,0.6f,0.2f,1f),  660,220, 180,100),
        new RegionNode("forest",       "Forest",        new Color(0.1f,0.4f,0.1f,1f),  200,480, 180,120),
        new RegionNode("lake",         "Lake",          new Color(0.2f,0.45f,0.8f,1f), 160,260, 160,120),
        new RegionNode("town",         "Town",          new Color(0.5f,0.5f,0.6f,1f),  680,400, 200,120),
        new RegionNode("highway",      "Highway",       new Color(0.4f,0.4f,0.35f,1f), 260,530, 540, 60),
    };

    // ── Road connections ──────────────────────────────────────
    private static final int[][] ROADS = {
        {0,1},{0,2},{0,3},{0,4},{1,5},{4,5}   // index pairs from REGIONS
    };

    private final VillageLegends game;
    private final BitmapFont      fontTitle;
    private final BitmapFont      fontBody;
    private final GlyphLayout     gl = new GlyphLayout();

    private boolean open        = false;
    private int     selected    = -1;
    private float   pulseTimer  = 0f;
    private final Rectangle btnClose = new Rectangle(VW - 100, VH - 58, 84, 42);

    // ─────────────────────────────────────────────────────────
    public MapScreen(VillageLegends game) {
        this.game  = game;
        fontTitle  = new BitmapFont(); fontTitle.getData().setScale(1.8f);
        fontBody   = new BitmapFont(); fontBody.getData().setScale(1.1f);
    }

    // ── Toggle ────────────────────────────────────────────────
    public void toggle() {
        open = !open;
        if (open) { selected = -1; game.audioManager.playSfx("ui_open"); }
    }

    public boolean isOpen() { return open; }

    // ── Render ────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (!open) return;
        pulseTimer += delta;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Dark background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.03f, 0.06f, 0.04f, 0.92f);
        sr.rect(0, 0, VW, VH);
        sr.end();

        // Roads first (underneath regions)
        drawRoads(sr);

        // Region blocks
        String current = game.activeSave != null
                ? game.activeSave.playerData.currentRegion : "main_village";
        for (int i = 0; i < REGIONS.length; i++) {
            drawRegion(sr, REGIONS[i], i == selected, current.equals(REGIONS[i].id));
        }

        // Close button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.5f, 0.08f, 0.08f, 0.9f);
        sr.rect(btnClose.x, btnClose.y, btnClose.width, btnClose.height);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawLabels(batch, current);
        drawDetailPanel(batch);
        handleInput();
    }

    // ── Roads ─────────────────────────────────────────────────
    private void drawRoads(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.40f, 0.30f, 0.8f);
        for (int[] road : ROADS) {
            RegionNode a = REGIONS[road[0]], b = REGIONS[road[1]];
            float ax = a.rx + a.rw / 2f, ay = a.ry + a.rh / 2f;
            float bx = b.rx + b.rw / 2f, by = b.ry + b.rh / 2f;
            // Draw thick road as elongated rectangle approximation
            float dx = bx - ax, dy = by - ay;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = -dy / len * 8f, ny = dx / len * 8f;
            sr.triangle(ax + nx, ay + ny, ax - nx, ay - ny, bx + nx, by + ny);
            sr.triangle(bx + nx, by + ny, bx - nx, by - ny, ax - nx, ay - ny);
        }
        sr.end();
    }

    // ── Region blocks ─────────────────────────────────────────
    private void drawRegion(ShapeRenderer sr, RegionNode r, boolean sel, boolean isCurrent) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Pulse effect for current region
        float pulse = isCurrent ? 0.12f * MathUtils.sin(pulseTimer * 3f) : 0f;
        Color col = new Color(
                r.color.r + pulse, r.color.g + pulse, r.color.b + pulse,
                sel ? 1f : 0.85f);
        sr.setColor(col);
        sr.rect(r.rx - (sel ? 4 : 0), r.ry - (sel ? 4 : 0),
                r.rw + (sel ? 8 : 0), r.rh + (sel ? 8 : 0));

        // Current location marker
        if (isCurrent) {
            sr.setColor(1f, 1f, 0f, 0.9f);
            float cx = r.rx + r.rw / 2f, cy = r.ry + r.rh / 2f;
            sr.circle(cx, cy, 10 + MathUtils.sin(pulseTimer * 4f) * 3f, 16);
        }
        sr.end();

        // Border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(sel ? Color.WHITE : new Color(r.color.r * 1.5f, r.color.g * 1.5f, r.color.b * 1.5f, 1f));
        sr.rect(r.rx, r.ry, r.rw, r.rh);
        sr.end();
    }

    // ── Labels ────────────────────────────────────────────────
    private void drawLabels(SpriteBatch batch, String currentRegion) {
        // Title
        fontTitle.setColor(Color.CYAN);
        fontTitle.getData().setScale(2.0f);
        fontTitle.draw(batch, "World Map", 30f, VH - 22f);
        fontTitle.getData().setScale(1.8f);

        // Region name labels
        for (RegionNode r : REGIONS) {
            fontBody.setColor(currentRegion.equals(r.id) ? Color.YELLOW : Color.WHITE);
            fontBody.getData().setScale(0.95f);
            gl.setText(fontBody, r.name);
            fontBody.draw(batch, r.name,
                    r.rx + r.rw / 2f - gl.width / 2f,
                    r.ry + r.rh / 2f + gl.height / 2f);
            fontBody.getData().setScale(1.1f);
        }

        // Legend
        fontBody.setColor(Color.YELLOW);
        fontBody.getData().setScale(0.85f);
        fontBody.draw(batch, "● = Your location", 30f, 80f);
        fontBody.getData().setScale(1.1f);

        // Close label
        fontBody.setColor(Color.RED);
        fontBody.draw(batch, "CLOSE", btnClose.x + 8, btnClose.y + 28);
    }

    // ── Detail panel ─────────────────────────────────────────
    private void drawDetailPanel(SpriteBatch batch) {
        if (selected < 0 || selected >= REGIONS.length) return;
        RegionNode r = REGIONS[selected];
        String current = game.activeSave != null
                ? game.activeSave.playerData.currentRegion : "";
        boolean isCurrent = current.equals(r.id);
        boolean visited   = game.activeSave != null
                && game.activeSave.playerData.visitedRegions.contains(r.id);

        float px = 30f, py = 200f;
        fontTitle.setColor(new Color(r.color.r * 1.4f, r.color.g * 1.4f, r.color.b * 1.4f, 1f));
        fontTitle.getData().setScale(1.5f);
        fontTitle.draw(batch, r.name, px, py);
        fontTitle.getData().setScale(1.8f);
        py -= 30;

        fontBody.setColor(Color.LIGHT_GRAY);
        fontBody.draw(batch, isCurrent ? "▶ You are here" : (visited ? "✓ Visited" : "○ Undiscovered"),
                px, py);
        py -= 22;

        // Region description
        String desc = getRegionDesc(r.id);
        fontBody.setColor(new Color(0.8f,0.85f,0.8f,1f));
        String[] lines = desc.split("\n");
        for (String line : lines) { fontBody.draw(batch, line, px, py); py -= 18; }
        py -= 8;

        // Fast-travel hint (only to visited regions)
        if (visited && !isCurrent) {
            fontBody.setColor(Color.CYAN);
            fontBody.draw(batch, "[ Use bus stand or walk to travel ]", px, py);
        }
    }

    private String getRegionDesc(String id) {
        switch (id) {
            case "main_village": return "Home village.\nTemple, school, market,\npanchayat office.";
            case "farmlands":    return "Fertile croplands.\n6 farm plots, barn,\nirrigation channels.";
            case "forest":       return "Dense forest.\nSmuggler camps, ruins,\nhidden treasures.";
            case "lake":         return "Peaceful lake.\nFishing, boat travel,\nmystery island.";
            case "town":         return "Busy market town.\nVehicle dealer, police\nstation, large market.";
            case "highway":      return "National highway.\nBus travel, racing,\ngang blockades.";
            default:             return "Unknown region.";
        }
    }

    // ── Input ─────────────────────────────────────────────────
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float sx = Gdx.input.getX(), sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float vx = sx * VW / Gdx.graphics.getWidth();
        float vy = sy * VH / Gdx.graphics.getHeight();

        if (btnClose.contains(vx, vy)) { toggle(); return; }

        for (int i = 0; i < REGIONS.length; i++) {
            RegionNode r = REGIONS[i];
            if (vx >= r.rx && vx <= r.rx + r.rw && vy >= r.ry && vy <= r.ry + r.rh) {
                selected = (selected == i) ? -1 : i;
                game.audioManager.playSfx("ui_click");
                return;
            }
        }
        selected = -1;
    }

    public void dispose() { fontTitle.dispose(); fontBody.dispose(); }

    // ── Inner data record ─────────────────────────────────────
    private static class RegionNode {
        final String  id, name;
        final Color   color;
        final float   rx, ry, rw, rh;
        RegionNode(String id, String name, Color c, float x, float y, float w, float h) {
            this.id = id; this.name = name; this.color = c;
            this.rx = x; this.ry = y; this.rw = w; this.rh = h;
        }
    }
}
