package com.villagelegends.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.entities.Player;
import com.villagelegends.systems.GameEventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World — wraps a LibGDX TiledMap and exposes typed layer accessors,
 * collision detection, and region-transition triggers.
 *
 * Map files are stored in android/assets/maps/ as .tmx files.
 * Each map has layers:
 *   Ground, Water, Decor, Objects (collision=true property), Triggers, Overhead
 *
 * Collision detection uses the "Objects" layer with a "blocked" boolean property,
 * falling back to a full rectangle scan for performance.
 */
public class World {

    private final VillageLegends game;

    private TiledMap map;
    private String   currentRegionId;

    // ── Layers ────────────────────────────────────────────────
    private TiledMapTileLayer groundLayer;
    private TiledMapTileLayer waterLayer;
    private TiledMapTileLayer decorLayer;
    private TiledMapTileLayer objectsLayer;   // solid tiles
    private TiledMapTileLayer overheadLayer;
    private MapLayer           triggersLayer;

    // ── Collision data ────────────────────────────────────────
    private final List<Rectangle> staticColliders = new ArrayList<>();

    // ── Triggers (region transitions, events) ────────────────
    private final List<WorldTrigger> triggers = new ArrayList<>();

    // ── World size ────────────────────────────────────────────
    private int widthTiles, heightTiles;
    private float widthPx, heightPx;

    // ─────────────────────────────────────────────────────────
    public World(VillageLegends game) {
        this.game = game;
    }

    /** Load a map by region ID.  Falls back to a default if TMX missing. */
    public void loadRegion(String regionId) {
        if (map != null) map.dispose();
        this.currentRegionId = regionId;
        staticColliders.clear();
        triggers.clear();

        String mapFile = "maps/" + regionId + ".tmx";
        boolean exists = Gdx.files.internal(mapFile).exists();

        if (exists) {
            map = new TmxMapLoader().load(mapFile);
            parseMapLayers();
            parseCollisionObjects();
            parseTriggers();
        } else {
            Gdx.app.log("World", "Map file not found: " + mapFile + " — using placeholder");
            map = buildPlaceholderMap(regionId);
        }

        widthTiles  = getMapWidth();
        heightTiles = getMapHeight();
        widthPx  = widthTiles  * GameConstants.TILE_SIZE;
        heightPx = heightTiles * GameConstants.TILE_SIZE;
    }

    private void parseMapLayers() {
        groundLayer  = (TiledMapTileLayer) map.getLayers().get("Ground");
        waterLayer   = (TiledMapTileLayer) map.getLayers().get("Water");
        decorLayer   = (TiledMapTileLayer) map.getLayers().get("Decor");
        objectsLayer = (TiledMapTileLayer) map.getLayers().get("Objects");
        overheadLayer= (TiledMapTileLayer) map.getLayers().get("Overhead");
        triggersLayer= map.getLayers().get("Triggers");
    }

    private void parseCollisionObjects() {
        MapLayer collisionLayer = map.getLayers().get("Collision");
        if (collisionLayer == null) {
            // Fall back: mark tiles with property "blocked=true" as colliders
            if (objectsLayer != null) {
                for (int row = 0; row < objectsLayer.getHeight(); row++) {
                    for (int col = 0; col < objectsLayer.getWidth(); col++) {
                        TiledMapTileLayer.Cell cell = objectsLayer.getCell(col, row);
                        if (cell != null && cell.getTile() != null) {
                            Object blocked = cell.getTile().getProperties().get("blocked");
                            if (Boolean.TRUE.equals(blocked)) {
                                staticColliders.add(new Rectangle(
                                        col * GameConstants.TILE_SIZE,
                                        row * GameConstants.TILE_SIZE,
                                        GameConstants.TILE_SIZE,
                                        GameConstants.TILE_SIZE));
                            }
                        }
                    }
                }
            }
            return;
        }

        MapObjects objects = collisionLayer.getObjects();
        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                staticColliders.add(new Rectangle(
                        rect.x * GameConstants.UNIT_SCALE,
                        rect.y * GameConstants.UNIT_SCALE,
                        rect.width  * GameConstants.UNIT_SCALE,
                        rect.height * GameConstants.UNIT_SCALE));
            }
        }
    }

    private void parseTriggers() {
        if (triggersLayer == null) return;
        MapObjects objects = triggersLayer.getObjects();
        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject)obj).getRectangle();
                String type     = (String) obj.getProperties().get("type");
                String target   = (String) obj.getProperties().get("target");
                triggers.add(new WorldTrigger(
                        new Rectangle(rect.x, rect.y, rect.width, rect.height),
                        type, target));
            }
        }
    }

    /** Placeholder grid map when TMX file is absent. */
    private TiledMap buildPlaceholderMap(String regionId) {
        // Returns an empty TiledMap; renderer handles missing layers gracefully.
        return new TiledMap();
    }

    // ── Collision ─────────────────────────────────────────────
    public boolean collidesWithMap(Rectangle entityBounds) {
        // Check world bounds
        if (entityBounds.x < 0 || entityBounds.y < 0
                || entityBounds.x + entityBounds.width  > widthPx
                || entityBounds.y + entityBounds.height > heightPx) {
            return true;
        }
        // Check static colliders (spatial optimisation in production: quadtree)
        for (Rectangle col : staticColliders) {
            if (col.overlaps(entityBounds)) return true;
        }
        return false;
    }

    // ── Triggers ─────────────────────────────────────────────
    public void checkTriggers(Player player) {
        Rectangle pb = player.getBounds();
        for (WorldTrigger t : triggers) {
            if (t.rect.overlaps(pb) && !t.triggered) {
                t.triggered = true;
                handleTrigger(t, player);
            }
            // Reset when player leaves
            if (!t.rect.overlaps(pb)) t.triggered = false;
        }
    }

    private void handleTrigger(WorldTrigger t, Player player) {
        if ("region_change".equals(t.type)) {
            game.activeSave.playerData.currentRegion = t.target;
            loadRegion(t.target);
            game.npcManager.spawnNPCsForRegion(this, t.target);
            game.audioManager.playRegionMusic(t.target);
        } else if ("quest_trigger".equals(t.type)) {
            game.questManager.triggerByFlag(t.target);
        } else if ("shop".equals(t.type)) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.OPEN_SHOP, t.target));
        }
    }

    // ── Interaction ──────────────────────────────────────────
    public void interact(Player player, VillageLegends game) {
        // Find interactive objects near player
        float px = player.centerX();
        float py = player.centerY();
        float r  = GameConstants.INTERACTION_RADIUS;

        // Check farm plots
        if (game.farmingManager.tryInteractAt(px, py, r, player)) return;

        // Check objects layer for chests, doors…
        checkTriggers(player);
    }

    // ── Size helpers ──────────────────────────────────────────
    private int getMapWidth() {
        if (map != null && map.getLayers().getCount() > 0
                && map.getLayers().get(0) instanceof TiledMapTileLayer) {
            return ((TiledMapTileLayer) map.getLayers().get(0)).getWidth();
        }
        return GameConstants.WORLD_WIDTH_TILES;
    }

    private int getMapHeight() {
        if (map != null && map.getLayers().getCount() > 0
                && map.getLayers().get(0) instanceof TiledMapTileLayer) {
            return ((TiledMapTileLayer) map.getLayers().get(0)).getHeight();
        }
        return GameConstants.WORLD_HEIGHT_TILES;
    }

    // ── Getters ───────────────────────────────────────────────
    public TiledMap             getTiledMap()       { return map; }
    public String               getCurrentRegionId(){ return currentRegionId; }
    public TiledMapTileLayer    getGroundLayer()    { return groundLayer; }
    public TiledMapTileLayer    getWaterLayer()     { return waterLayer; }
    public TiledMapTileLayer    getDecorLayer()     { return decorLayer; }
    public TiledMapTileLayer    getObjectsLayer()   { return objectsLayer; }
    public TiledMapTileLayer    getOverheadLayer()  { return overheadLayer; }
    public float                getWidthPixels()    { return widthPx; }
    public float                getHeightPixels()   { return heightPx; }
    public List<Rectangle>      getStaticColliders(){ return staticColliders; }

    public void dispose() { if (map != null) map.dispose(); }

    // ── Inner trigger record ──────────────────────────────────
    private static class WorldTrigger {
        final Rectangle rect;
        final String    type;
        final String    target;
        boolean         triggered = false;
        WorldTrigger(Rectangle r, String type, String target) {
            this.rect = r; this.type = type; this.target = target;
        }
    }

    // NOTE: PlaceholderMapBuilder integration – add to World.loadRegion():
    // When map == null or placeholder, instantiate PlaceholderMapBuilder.build(regionId)

    // ── Placeholder rendering ─────────────────────────────────
    /** Returns true when no TMX map is loaded (placeholder mode). */
    public boolean hasPlaceholder() {
        return map != null && map.getLayers().getCount() == 0;
    }

    /** Draw a simple coloured-tile grid as placeholder world. */
    public void drawPlaceholder(ShapeRenderer sr) {
        int cols = GameConstants.WORLD_WIDTH_TILES;
        int rows = GameConstants.WORLD_HEIGHT_TILES;
        float ts = GameConstants.TILE_SIZE;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Simple checkerboard: green grass / grey buildings
                if ((col + row) % 7 == 0) {
                    sr.setColor(Color.GRAY);
                } else if ((col + row) % 5 == 0) {
                    sr.setColor(Color.BLUE);
                } else {
                    sr.setColor(Color.FOREST);
                }
                sr.rect(col * ts, row * ts, ts, ts);
            }
        }
        sr.end();
    }
}
