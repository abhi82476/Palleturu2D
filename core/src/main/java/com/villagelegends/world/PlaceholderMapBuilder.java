package com.villagelegends.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaceholderMapBuilder — generates a renderable placeholder world
 * when TMX map files are not yet available (development builds).
 *
 * Draws coloured rectangles representing:
 *   Green  = grass / ground
 *   Blue   = water
 *   Brown  = dirt path
 *   Gray   = buildings
 *   Red    = gang / hostile zone
 *   Gold   = temple / sacred
 *   Tan    = farm plots
 *
 * Also seeds the static collision list used by World so the player
 * can still walk around and collide with placeholder buildings.
 */
public class PlaceholderMapBuilder {

    // ── Region visual data ─────────────────────────────────────
    public static class MapTile {
        public float x, y, w, h;
        public Color color;
        public boolean solid;
        MapTile(float x, float y, float w, float h, Color c, boolean solid) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.color = c; this.solid = solid;
        }
    }

    private final List<MapTile>   tiles      = new ArrayList<>();
    private final List<Rectangle> colliders  = new ArrayList<>();

    private final float T = GameConstants.TILE_SIZE;   // 32f

    // ─────────────────────────────────────────────────────────
    public void build(String regionId) {
        tiles.clear();
        colliders.clear();

        switch (regionId) {
            case "main_village": buildMainVillage(); break;
            case "farmlands":    buildFarmlands();   break;
            case "forest":       buildForest();      break;
            case "lake":         buildLake();        break;
            case "town":         buildTown();        break;
            case "highway":      buildHighway();     break;
            default:             buildDefault();     break;
        }
    }

    // ── Region builders ───────────────────────────────────────
    private void buildMainVillage() {
        // Ground base
        ground(0, 0, 128, 128, new Color(0.55f, 0.75f, 0.35f, 1f));

        // Dirt path (cross-shaped)
        path(50*T, 0, 8*T, 128*T);      // N-S road
        path(0, 60*T, 128*T, 8*T);      // E-W road

        // Temple complex (top-left, gold)
        building(8*T, 8*T, 10*T, 12*T, new Color(0.9f, 0.75f, 0.2f, 1f));    // temple
        building(10*T, 7*T, 6*T, 2*T, new Color(0.8f, 0.6f, 0.15f, 1f));     // gopuram

        // Panchayat office (centre)
        building(52*T, 56*T, 8*T, 8*T, new Color(0.6f, 0.6f, 0.75f, 1f));

        // Market row (right side)
        for (int i = 0; i < 6; i++) {
            building((78 + i*4)*T, 38*T, 3*T, 3*T, new Color(0.8f, 0.4f, 0.2f, 1f));
        }

        // School (bottom-left)
        building(22*T, 88*T, 8*T, 6*T, new Color(0.4f, 0.6f, 0.85f, 1f));

        // Bus stand
        building(56*T, 98*T, 6*T, 4*T, new Color(0.55f, 0.55f, 0.55f, 1f));

        // Houses (scattered)
        int[][] houses = {{15,15},{18,25},{32,14},{38,20},{70,15},{75,25},
                          {85,55},{90,70},{20,70},{35,75},{65,75},{80,85}};
        for (int[] h : houses) {
            building(h[0]*T, h[1]*T, 4*T, 4*T, new Color(0.75f, 0.45f, 0.25f, 1f));
        }

        // Well
        building(58*T, 70*T, 2*T, 2*T, new Color(0.4f, 0.4f, 0.5f, 1f));
    }

    private void buildFarmlands() {
        ground(0, 0, 96, 96, new Color(0.6f, 0.5f, 0.3f, 1f));  // brown soil base

        // Farm plots (Tan)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                addTile((8 + col*16)*T, (8 + row*20)*T, 12*T, 16*T,
                        new Color(0.7f, 0.8f, 0.4f, 1f), false);
            }
        }

        // Irrigation channels (blue)
        addTile(6*T, 0, 2*T, 96*T, new Color(0.3f, 0.5f, 0.8f, 0.7f), false);
        addTile(22*T, 0, 2*T, 96*T, new Color(0.3f, 0.5f, 0.8f, 0.7f), false);
        addTile(38*T, 0, 2*T, 96*T, new Color(0.3f, 0.5f, 0.8f, 0.7f), false);

        // Barn
        building(80*T, 10*T, 8*T, 10*T, new Color(0.6f, 0.35f, 0.15f, 1f));
    }

    private void buildForest() {
        ground(0, 0, 160, 160, new Color(0.2f, 0.45f, 0.15f, 1f));  // dark forest green

        // Tree clusters (solid dark green blobs)
        int[][] trees = {
            {5,5,20,20},{30,5,25,15},{60,8,20,20},{90,5,25,18},{120,5,20,25},
            {5,30,15,20},{150,30,10,20},{5,60,20,15},{145,55,15,20},
            {5,90,18,25},{150,85,10,20},{5,120,20,20},{140,110,20,25}
        };
        for (int[] t : trees) {
            addTile(t[0]*T, t[1]*T, t[2]*T, t[3]*T,
                    new Color(0.1f, 0.3f, 0.08f, 1f), true);
        }

        // Winding path (lighter green)
        path(15*T, 0, 6*T, 160*T);

        // Smuggler camp (red zone)
        building(78*T, 28*T, 8*T, 8*T, new Color(0.6f, 0.1f, 0.1f, 1f));

        // Ancient ruins (gray)
        building(38*T, 38*T, 6*T, 6*T, new Color(0.5f, 0.5f, 0.45f, 0.8f));
    }

    private void buildLake() {
        ground(0, 0, 128, 96, new Color(0.55f, 0.75f, 0.4f, 1f));

        // Main lake (blue)
        addTile(15*T, 10*T, 80*T, 75*T, new Color(0.25f, 0.5f, 0.85f, 0.9f), false);

        // Island
        addTile(45*T, 35*T, 18*T, 14*T, new Color(0.6f, 0.75f, 0.45f, 1f), false);
        building(50*T, 38*T, 4*T, 4*T, new Color(0.5f, 0.5f, 0.45f, 0.8f)); // ruins

        // Dock and fishing village (east shore)
        path(100*T, 0, 8*T, 96*T);   // shoreline road
        for (int i = 0; i < 5; i++) {
            building(102*T, (10+i*16)*T, 4*T, 4*T, new Color(0.65f, 0.4f, 0.2f, 1f));
        }
        building(100*T, 45*T, 5*T, 3*T, new Color(0.4f, 0.4f, 0.4f, 1f)); // dock
    }

    private void buildTown() {
        ground(0, 0, 192, 160, new Color(0.6f, 0.6f, 0.55f, 1f)); // gray urban

        // Grid roads
        for (int i = 0; i < 5; i++) {
            path(0, (30+i*30)*T, 192*T, 6*T);  // horizontal
            path((30+i*30)*T, 0, 6*T, 160*T);  // vertical
        }

        // Market blocks
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                building((10+c*40)*T, (8+r*20)*T, 18*T, 12*T,
                        new Color(0.7f, 0.45f, 0.2f, 1f));
            }
        }

        // Vehicle dealer
        building(150*T, 100*T, 24*T, 16*T, new Color(0.4f, 0.6f, 0.4f, 1f));

        // Police station (blue)
        building(148*T, 78*T, 16*T, 14*T, new Color(0.2f, 0.3f, 0.7f, 1f));

        // Town hall
        building(90*T, 10*T, 20*T, 18*T, new Color(0.55f, 0.55f, 0.7f, 1f));
    }

    private void buildHighway() {
        ground(0, 0, 256, 64, new Color(0.55f, 0.6f, 0.4f, 1f)); // roadside grass

        // Highway asphalt
        addTile(0, 20*T, 256*T, 24*T, new Color(0.35f, 0.35f, 0.38f, 1f), false);

        // Centre line markings (yellow dashes)
        for (int i = 0; i < 30; i++) {
            addTile(i*34*T + T, 31*T, 2*T, T, new Color(0.9f, 0.8f, 0.1f, 1f), false);
        }

        // Bus stops
        int[] busStops = {30, 90, 160, 220};
        for (int x : busStops) {
            building(x*T, 18*T, 8*T, 4*T, new Color(0.55f, 0.55f, 0.55f, 1f));
        }

        // Petrol bunk
        building(120*T, 10*T, 10*T, 8*T, new Color(0.8f, 0.3f, 0.1f, 1f));
    }

    private void buildDefault() {
        ground(0, 0, 64, 64, new Color(0.5f, 0.7f, 0.35f, 1f));
    }

    // ── Helpers ───────────────────────────────────────────────
    private void ground(float tx, float ty, float tw, float th, Color c) {
        addTile(tx*T, ty*T, tw*T, th*T, c, false);
    }

    private void path(float x, float y, float w, float h) {
        addTile(x, y, w, h, new Color(0.65f, 0.55f, 0.35f, 1f), false);
    }

    private void building(float x, float y, float w, float h, Color c) {
        addTile(x, y, w, h, c, true);
        colliders.add(new Rectangle(x, y, w, h));
    }

    private void addTile(float x, float y, float w, float h, Color c, boolean solid) {
        tiles.add(new MapTile(x, y, w, h, c, solid));
        if (solid) colliders.add(new Rectangle(x, y, w, h));
    }

    // ── Rendering ─────────────────────────────────────────────
    public void draw(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (MapTile t : tiles) {
            sr.setColor(t.color);
            sr.rect(t.x, t.y, t.w, t.h);
        }
        sr.end();

        // Grid overlay (subtle)
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0f, 0f, 0f, 0.06f);
        for (MapTile t : tiles) {
            if (!t.solid) sr.rect(t.x, t.y, t.w, t.h);
        }
        sr.end();
    }

    public void drawColliders(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(1f, 0f, 0f, 0.5f);
        for (Rectangle r : colliders) {
            sr.rect(r.x, r.y, r.width, r.height);
        }
        sr.end();
    }

    // ── Getters ───────────────────────────────────────────────
    public List<Rectangle> getColliders() { return colliders; }
    public List<MapTile>   getTiles()     { return tiles; }

    /** Get approximate world pixel dimensions from built tiles */
    public float getWidthPixels() {
        float max = GameConstants.WORLD_WIDTH_TILES * GameConstants.TILE_SIZE;
        for (MapTile t : tiles) max = Math.max(max, t.x + t.w);
        return max;
    }

    public float getHeightPixels() {
        float max = GameConstants.WORLD_HEIGHT_TILES * GameConstants.TILE_SIZE;
        for (MapTile t : tiles) max = Math.max(max, t.y + t.h);
        return max;
    }
}
