package com.villagelegends.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.villagelegends.entities.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * AnimationManager loads all sprite atlases and builds named Animation<TextureRegion>
 * objects that entities can query by state + direction.
 *
 * Atlas files expected at runtime:
 *   android/assets/textures/characters.atlas
 *   android/assets/textures/vehicles.atlas
 *   android/assets/textures/effects.atlas
 *   android/assets/textures/items.atlas
 *   android/assets/textures/tiles.atlas
 *   android/assets/textures/ui.atlas
 *
 * If an atlas file is missing (during development) the manager returns a
 * 1×1 white pixel TextureRegion placeholder so the game still runs.
 *
 * Naming convention in the atlas:
 *   player_walk_down_00, player_walk_down_01 … player_walk_down_05
 *   player_run_up_00 …
 *   npc_farmer_idle_left_00 …
 *   vehicle_motorcycle_right
 *   effect_hit_spark_00 …
 */
public class AnimationManager implements Disposable {

    // ── Atlas cache ───────────────────────────────────────────
    private final Map<String, TextureAtlas>                  atlases    = new HashMap<>();
    private final Map<String, Animation<TextureRegion>>      animations = new HashMap<>();
    private TextureRegion                                    placeholder;

    // ── Frame durations (seconds) ─────────────────────────────
    private static final float FPS_WALK    = 1f / 8f;
    private static final float FPS_RUN     = 1f / 10f;
    private static final float FPS_ATTACK  = 1f / 12f;
    private static final float FPS_IDLE    = 1f / 4f;
    private static final float FPS_EFFECT  = 1f / 14f;

    // ── Singleton-per-game-instance ───────────────────────────
    public AnimationManager() {
        loadAllAtlases();
        buildAnimations();
    }

    // ── Loading ───────────────────────────────────────────────
    private void loadAllAtlases() {
        loadAtlas("characters");
        loadAtlas("vehicles");
        loadAtlas("effects");
        loadAtlas("items");
        loadAtlas("ui");
    }

    private void loadAtlas(String name) {
        String path = "textures/" + name + ".atlas";
        if (Gdx.files.internal(path).exists()) {
            try {
                atlases.put(name, new TextureAtlas(Gdx.files.internal(path)));
                Gdx.app.log("AnimationManager", "Loaded atlas: " + name);
            } catch (Exception e) {
                Gdx.app.log("AnimationManager", "Could not load atlas: " + name);
            }
        } else {
            Gdx.app.log("AnimationManager", "Atlas not found (placeholder mode): " + name);
        }
    }

    /** Build animation clips from loaded atlases */
    private void buildAnimations() {
        if (atlases.isEmpty()) return;
        TextureAtlas chars = atlases.get("characters");
        if (chars == null) return;

        // Player animations
        String[] dirs = {"down", "up", "left", "right"};
        for (String dir : dirs) {
            buildAnim("player_idle_"   + dir, "characters", "player_idle_"   + dir, FPS_IDLE,   Animation.PlayMode.LOOP);
            buildAnim("player_walk_"   + dir, "characters", "player_walk_"   + dir, FPS_WALK,   Animation.PlayMode.LOOP);
            buildAnim("player_run_"    + dir, "characters", "player_run_"    + dir, FPS_RUN,    Animation.PlayMode.LOOP);
            buildAnim("player_attack_" + dir, "characters", "player_attack_" + dir, FPS_ATTACK, Animation.PlayMode.NORMAL);
            buildAnim("player_dodge_"  + dir, "characters", "player_dodge_"  + dir, FPS_ATTACK, Animation.PlayMode.NORMAL);
        }
        buildAnim("player_hurt",    "characters", "player_hurt",    FPS_ATTACK, Animation.PlayMode.NORMAL);
        buildAnim("player_fish",    "characters", "player_fish",    FPS_IDLE,   Animation.PlayMode.LOOP);
        buildAnim("player_farm",    "characters", "player_farm",    FPS_WALK,   Animation.PlayMode.LOOP);

        // NPC type animations
        String[] npcTypes = {"farmer", "merchant", "student", "elder", "guard",
                             "gang_thug", "smuggler", "official", "fisherman",
                             "priest", "teacher", "bus_driver", "villager"};
        for (String npc : npcTypes) {
            for (String dir : dirs) {
                buildAnim("npc_" + npc + "_walk_" + dir, "characters",
                        "npc_" + npc + "_walk_" + dir, FPS_WALK, Animation.PlayMode.LOOP);
            }
            buildAnim("npc_" + npc + "_idle", "characters", "npc_" + npc + "_idle", FPS_IDLE, Animation.PlayMode.LOOP);
        }

        // Effect animations
        TextureAtlas fx = atlases.get("effects");
        if (fx != null) {
            buildAnim("hit_spark",     "effects", "hit_spark",     FPS_EFFECT, Animation.PlayMode.NORMAL);
            buildAnim("knockout_stars","effects", "knockout_stars", FPS_EFFECT, Animation.PlayMode.NORMAL);
            buildAnim("dust_cloud",    "effects", "dust_cloud",     FPS_EFFECT, Animation.PlayMode.NORMAL);
            buildAnim("water_splash",  "effects", "water_splash",   FPS_EFFECT, Animation.PlayMode.NORMAL);
            buildAnim("harvest_spark", "effects", "harvest_spark",  FPS_EFFECT, Animation.PlayMode.NORMAL);
        }
    }

    private void buildAnim(String key, String atlasName, String regionPrefix,
                           float frameDuration, Animation.PlayMode mode) {
        TextureAtlas atlas = atlases.get(atlasName);
        if (atlas == null) return;
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(regionPrefix);
        if (regions.size == 0) return;
        Animation<TextureRegion> anim = new Animation<>(frameDuration,
                (Array<? extends TextureRegion>) (Array<?>) regions, mode);
        animations.put(key, anim);
    }

    // ── Public API ────────────────────────────────────────────
    /**
     * Get the current frame for an entity's state.
     * Returns a 1×1 white placeholder if the animation doesn't exist yet.
     */
    public TextureRegion getFrame(String animKey, float stateTime) {
        Animation<TextureRegion> anim = animations.get(animKey);
        if (anim != null) return anim.getKeyFrame(stateTime);
        return getPlaceholder();
    }

    /**
     * Convenience: build the animation key from entity state and direction.
     * e.g. buildKey("player", "walk", Direction.DOWN) → "player_walk_down"
     */
    public String buildKey(String entity, String state, Entity.Direction direction) {
        String dir;
        switch (direction) {
            case UP:    dir = "up";    break;
            case LEFT:  dir = "left";  break;
            case RIGHT: dir = "right"; break;
            default:    dir = "down";  break;
        }
        return entity + "_" + state + "_" + dir;
    }

    public TextureRegion getItemIcon(String itemId) {
        TextureAtlas items = atlases.get("items");
        if (items != null) {
            TextureAtlas.AtlasRegion r = items.findRegion("item_" + itemId);
            if (r != null) return r;
        }
        return getPlaceholder();
    }

    public TextureRegion getUIRegion(String regionName) {
        TextureAtlas ui = atlases.get("ui");
        if (ui != null) {
            TextureAtlas.AtlasRegion r = ui.findRegion(regionName);
            if (r != null) return r;
        }
        return getPlaceholder();
    }

    private TextureRegion getPlaceholder() {
        if (placeholder == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(
                    1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            pm.fill();
            placeholder = new TextureRegion(new com.badlogic.gdx.graphics.Texture(pm));
            pm.dispose();
        }
        return placeholder;
    }

    public boolean isAnimationFinished(String animKey, float stateTime) {
        Animation<TextureRegion> anim = animations.get(animKey);
        return anim == null || anim.isAnimationFinished(stateTime);
    }

    @Override
    public void dispose() {
        for (TextureAtlas atlas : atlases.values()) atlas.dispose();
        atlases.clear();
        animations.clear();
        if (placeholder != null) placeholder.getTexture().dispose();
    }
}
