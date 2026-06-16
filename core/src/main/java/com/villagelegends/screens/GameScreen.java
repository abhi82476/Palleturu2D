package com.villagelegends.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.entities.Player;
import com.villagelegends.minigames.CricketGame;
import com.villagelegends.minigames.FishingGame;
import com.villagelegends.minigames.KabaddiGame;
import com.villagelegends.minigames.KiteFlyingGame;
import com.villagelegends.minigames.TractorRaceGame;
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.ui.DialogueUI;
import com.villagelegends.ui.HUD;
import com.villagelegends.ui.InventoryUI;
import com.villagelegends.ui.MapScreen;
import com.villagelegends.ui.MobileControls;
import com.villagelegends.ui.QuestLogUI;
import com.villagelegends.ui.ShopUI;
import com.villagelegends.world.DayNightCycle;
import com.villagelegends.world.WeatherSystem;
import com.villagelegends.world.World;

/**
 * GameScreen — main gameplay loop.
 *
 * Render passes (each frame):
 *   1. World camera  → tile layers + entities (day/night tinted)
 *   2. UI camera     → HUD + overlays + controls (full brightness)
 *
 * All overlay panels and mini-games render inside this screen to avoid
 * screen-switch latency on Android. A mutual-exclusion flag ensures
 * only one overlay is visible at a time.
 */
public class GameScreen extends BaseScreen {

    // ── World camera ──────────────────────────────────────────
    private OrthographicCamera gameCamera;
    private FitViewport        gameViewport;

    // ── World ─────────────────────────────────────────────────
    private World              world;
    private OrthogonalTiledMapRenderer mapRenderer;

    // ── Core entities ─────────────────────────────────────────
    private Player        player;
    private DayNightCycle dayNight;
    private WeatherSystem weather;

    // ── Mobile UI ─────────────────────────────────────────────
    private HUD            hud;
    private MobileControls controls;

    // ── Overlay panels ────────────────────────────────────────
    private InventoryUI inventoryUI;
    private ShopUI      shopUI;
    private QuestLogUI  questLogUI;
    private DialogueUI  dialogueUI;
    private MapScreen   mapScreen;

    // ── Mini-games ────────────────────────────────────────────
    private FishingGame    fishingGame;
    private KabaddiGame    kabaddiGame;
    private TractorRaceGame tractorRace;
    private CricketGame    cricketGame;
    private KiteFlyingGame kiteGame;

    // ── State ─────────────────────────────────────────────────
    private boolean paused     = false;
    private float   pauseAlpha = 0f;
    private float   shakeX     = 0f;
    private float   totalTime  = 0f;

    // ── Camera lerp ───────────────────────────────────────────
    private final Vector2 camTarget = new Vector2();

    // ─────────────────────────────────────────────────────────
    public GameScreen(VillageLegends game) { super(game); }

    @Override
    public void show() {
        // World camera
        gameCamera   = new OrthographicCamera();
        gameViewport = new FitViewport(
                GameConstants.VIRTUAL_WIDTH,
                GameConstants.VIRTUAL_HEIGHT,
                gameCamera);
        gameCamera.setToOrtho(false,
                GameConstants.VIRTUAL_WIDTH,
                GameConstants.VIRTUAL_HEIGHT);

        // World & map
        world = new World(game);
        world.loadRegion(game.activeSave.playerData.currentRegion);
        if (world.getTiledMap() != null) {
            mapRenderer = new OrthogonalTiledMapRenderer(
                    world.getTiledMap(),
                    GameConstants.UNIT_SCALE,
                    game.batch);
        }

        // Player
        player = new Player(game,
                game.activeSave.playerData.worldX,
                game.activeSave.playerData.worldY);

        // Time & weather
        dayNight = new DayNightCycle(game.activeSave.gameHour);
        weather  = new WeatherSystem();

        // UI
        hud         = new HUD(game, player);
        controls    = new MobileControls(game);
        inventoryUI = new InventoryUI(game, player);
        shopUI      = new ShopUI(game, player);
        questLogUI  = new QuestLogUI(game);
        dialogueUI  = new DialogueUI(game);
        mapScreen   = new MapScreen(game);

        // Mini-games
        fishingGame = new FishingGame(game, player);
        kabaddiGame = new KabaddiGame(game);
        tractorRace = new TractorRaceGame(game);
        cricketGame = new CricketGame(game);
        kiteGame    = new KiteFlyingGame(game);

        // Spawn NPCs and vehicles
        game.npcManager.spawnNPCsForRegion(world,
                game.activeSave.playerData.currentRegion);
        game.vehicleManager.spawnVehiclesForRegion(
                game.activeSave.playerData.currentRegion);

        game.audioManager.playRegionMusic(world.getCurrentRegionId());
        Gdx.input.setInputProcessor(controls.getInputProcessor());

        subscribeEvents();
    }

    private void subscribeEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.DIALOGUE_START, e -> {
            com.villagelegends.entities.NPC npc = game.npcManager.getDialogueNPC();
            if (npc != null) dialogueUI.openFor(npc);
        });
    }

    // ── Render ───────────────────────────────────────────────
    @Override
    public void render(float delta) {
        totalTime += delta;

        // Managers that always tick (even when overlay is open)
        game.audioManager.update(delta);
        game.saveManager.tick(delta);

        // Only update game logic when nothing is blocking
        if (!paused && !isAnyOverlayOpen()) {
            update(delta);
        }

        draw(delta);
    }

    // ── Update ────────────────────────────────────────────────
    private void update(float delta) {
        // Time progression
        dayNight.update(delta);
        weather.update(delta, dayNight.getHour());

        // Sync to save
        game.activeSave.gameHour   = dayNight.getHour();
        game.activeSave.currentDay = dayNight.getDay();
        game.activeSave.totalPlayTime += delta;

        // Day-change event (fires once when hour wraps to ~0)
        if (dayNight.getHour() < 0.05f && delta > 0) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.NEW_DAY, dayNight.getDay()));
        }

        // Player
        player.update(delta, controls, world);
        game.activeSave.playerData.worldX = player.getX();
        game.activeSave.playerData.worldY = player.getY();

        // Core systems
        game.npcManager.update(delta, dayNight.getHour(), player, world);
        game.vehicleManager.update(delta, world);
        game.farmingManager.update(delta, dayNight);
        game.combatSystem.update(delta, player,
                game.npcManager.getActiveNPCs(),
                game.vehicleManager.getActiveVehicles());
        game.questManager.tick(delta, player, game.npcManager, world);
        game.festivalManager.update(delta, dayNight);
        game.economyManager.update(delta);
        game.deliverySystem.update(delta, player, world.getCurrentRegionId());
        game.treasureSystem.update(delta, player, world.getCurrentRegionId());

        // Mini-games (each checks its own active state)
        if (fishingGame.isActive()) fishingGame.update(delta, controls);
        if (kabaddiGame.isActive()) kabaddiGame.update(delta, controls);
        if (tractorRace.isActive()) tractorRace.update(delta, controls);
        if (cricketGame.isActive())  cricketGame.update(delta, controls);
        if (kiteGame.isActive())     kiteGame.update(delta, controls);

        // World triggers (region transitions, quest events)
        world.checkTriggers(player);

        // Camera
        updateCamera(delta);
        shakeX = game.combatSystem.getShakeOffset();

        // Button events
        handleUIButtons();
    }

    private void handleUIButtons() {
        if (controls.justInventory()) inventoryUI.toggle();
        if (controls.justQuest())     questLogUI.toggle();
        if (controls.justMap())       mapScreen.toggle();
        if (controls.justPaused())    togglePause();
    }

    private void updateCamera(float delta) {
        camTarget.set(player.centerX(), player.centerY());
        float t = GameConstants.CAMERA_LERP_SPEED * delta;
        gameCamera.position.x = MathUtils.lerp(
                gameCamera.position.x, camTarget.x, t) + shakeX;
        gameCamera.position.y = MathUtils.lerp(
                gameCamera.position.y, camTarget.y, t);

        // Clamp to world bounds
        float halfW = gameCamera.viewportWidth  / 2f;
        float halfH = gameCamera.viewportHeight / 2f;
        float ww    = world.getWidthPixels();
        float wh    = world.getHeightPixels();
        if (ww > gameCamera.viewportWidth)
            gameCamera.position.x = MathUtils.clamp(
                    gameCamera.position.x, halfW, ww - halfW);
        if (wh > gameCamera.viewportHeight)
            gameCamera.position.y = MathUtils.clamp(
                    gameCamera.position.y, halfH, wh - halfH);
        gameCamera.update();
    }

    // ── Draw ─────────────────────────────────────────────────
    private void draw(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.04f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Day/night + festival tint
        float[] wt   = dayNight.getWorldTint();
        float[] tint = game.festivalManager.applyFestivalTint(wt);
        game.batch.setColor(tint[0], tint[1], tint[2], 1f);

        // ── TMX map: ground, water, decor ──────────────────
        if (mapRenderer != null) {
            mapRenderer.setView(gameCamera);
            if (world.getGroundLayer()  != null) mapRenderer.renderTileLayer(world.getGroundLayer());
            if (world.getWaterLayer()   != null) mapRenderer.renderTileLayer(world.getWaterLayer());
            if (world.getDecorLayer()   != null) mapRenderer.renderTileLayer(world.getDecorLayer());
        } else if (world.hasPlaceholder()) {
            // Placeholder coloured-tile world (no TMX files needed)
            Gdx.gl.glEnable(GL20.GL_BLEND);
            game.shapeRenderer.setProjectionMatrix(gameCamera.combined);
            world.drawPlaceholder(game.shapeRenderer);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ── Entity pass (world-space) ────────────────────────
        gameViewport.apply();
        game.batch.setProjectionMatrix(gameCamera.combined);
        game.batch.begin();

        game.vehicleManager.draw(game.batch, delta);
        game.npcManager.draw(game.batch, delta);
        player.draw(game.batch, delta);
        weather.draw(game.batch, gameCamera);

        game.batch.end();

        // Treasure dig-spot overlays (world-space)
        game.treasureSystem.draw(game.batch, delta, gameCamera, world.getCurrentRegionId());

        // ── Overhead tiles (roofs, tree canopies) ────────────
        if (mapRenderer != null && world.getOverheadLayer() != null) {
            game.batch.setColor(tint[0], tint[1], tint[2], 1f);
            mapRenderer.renderTileLayer(world.getOverheadLayer());
        }

        // ── UI pass (screen-space, always full brightness) ────
        game.batch.setColor(1f, 1f, 1f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        // Delivery timer bar (when active, shows in HUD)
        if (game.deliverySystem.hasActiveJobs()) {
            drawDeliveryHUD();
        }

        // HUD
        hud.draw(game.batch, delta, dayNight, player);

        // Controls (hidden when overlay is open)
        if (!isAnyOverlayOpen() && !paused) {
            controls.draw(game.batch, delta);
        }

        // Overlays — only one at a time
        if      (dialogueUI.isVisible())   dialogueUI.draw(game.batch, delta);
        else if (inventoryUI.isOpen())     inventoryUI.draw(game.batch, delta);
        else if (shopUI.isOpen())          shopUI.draw(game.batch, delta);
        else if (questLogUI.isOpen())      questLogUI.draw(game.batch, delta);
        else if (mapScreen.isOpen())       mapScreen.draw(game.batch, delta);

        // Mini-games (top layer)
        if (fishingGame.isActive()) fishingGame.draw(game.batch, delta);
        if (kabaddiGame.isActive()) kabaddiGame.draw(game.batch, delta);
        if (tractorRace.isActive()) tractorRace.draw(game.batch, delta);
        if (cricketGame.isActive())  cricketGame.draw(game.batch, delta);
        if (kiteGame.isActive())     kiteGame.draw(game.batch, delta);

        // Festival overlay (confetti, banners)
        game.festivalManager.drawOverlay(game.batch,
                GameConstants.VIRTUAL_WIDTH, GameConstants.VIRTUAL_HEIGHT);

        // Pause overlay
        if (paused) drawPauseOverlay(delta);

        game.batch.end();
    }

    // ── Delivery HUD bar ─────────────────────────────────────
    private void drawDeliveryHUD() {
        com.villagelegends.systems.DeliverySystem.DeliveryJob urgent =
                game.deliverySystem.getMostUrgent();
        if (urgent == null) return;

        float barW = 200f, barH = 14f;
        float bx   = GameConstants.VIRTUAL_WIDTH / 2f - barW / 2f;
        float by   = GameConstants.VIRTUAL_HEIGHT - 56f;

        game.batch.end();
        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        game.shapeRenderer.begin(
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 0.8f);
        game.shapeRenderer.rect(bx, by, barW, barH);
        float pct = 1f - urgent.getProgressFraction();
        com.badlogic.gdx.graphics.Color col = pct > 0.5f
                ? com.badlogic.gdx.graphics.Color.CYAN
                : pct > 0.25f ? com.badlogic.gdx.graphics.Color.YELLOW
                : com.badlogic.gdx.graphics.Color.RED;
        game.shapeRenderer.setColor(col);
        game.shapeRenderer.rect(bx, by, barW * pct, barH);
        game.shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        game.batch.begin();

        // Label
        com.badlogic.gdx.graphics.g2d.BitmapFont f =
                new com.badlogic.gdx.graphics.g2d.BitmapFont();
        f.getData().setScale(0.85f);
        f.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        f.draw(game.batch,
                "📦 " + urgent.itemDisplayName + "  →  " + urgent.destRegion
                + "   ⏱ " + urgent.getTimerText(),
                bx, by + barH + 14);
        f.dispose();
    }

    // ── Pause overlay ─────────────────────────────────────────
    private void drawPauseOverlay(float delta) {
        pauseAlpha = Math.min(1f, pauseAlpha + delta * 4f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.6f * pauseAlpha);
        game.shapeRenderer.rect(0, 0,
                GameConstants.VIRTUAL_WIDTH, GameConstants.VIRTUAL_HEIGHT);
        game.shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        hud.drawPauseMenu(game.batch, this);
    }

    // ── Overlay check ─────────────────────────────────────────
    private boolean isAnyOverlayOpen() {
        return inventoryUI.isOpen() || shopUI.isOpen()
            || questLogUI.isOpen()  || dialogueUI.isVisible()
            || mapScreen.isOpen()
            || fishingGame.isActive() || kabaddiGame.isActive()
            || tractorRace.isActive() || cricketGame.isActive()
            || kiteGame.isActive();
    }

    // ── Public API ────────────────────────────────────────────
    public void togglePause() {
        paused = !paused;
        if (!paused) pauseAlpha = 0f;
        game.audioManager.playSfx("ui_click");
    }

    public void saveAndQuit() {
        game.activeSave.playerData.worldX = player.getX();
        game.activeSave.playerData.worldY = player.getY();
        game.saveCurrentGame();
        game.returnToMenu();
    }

    /** Start a mini-game by name (called by NPC interactions / triggers). */
    public void startMiniGame(String name) {
        switch (name) {
            case "fishing":     fishingGame.start();   break;
            case "kabaddi":     kabaddiGame.start();   break;
            case "tractor_race":tractorRace.start();   break;
            case "cricket":     cricketGame.start();   break;
            case "kite":        kiteGame.start();      break;
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean     isPaused()    { return paused; }
    public Player      getPlayer()   { return player; }
    public DayNightCycle getDayNight(){ return dayNight; }
    public World       getWorld()    { return world; }

    // ── Lifecycle ─────────────────────────────────────────────
    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        gameViewport.update(w, h, false);
    }

    @Override
    public void pause() {
        super.pause();
        paused = true;
        game.saveCurrentGame();
    }

    @Override
    public void dispose() {
        world.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        hud.dispose();
        controls.dispose();
        inventoryUI.dispose();
        shopUI.dispose();
        questLogUI.dispose();
        dialogueUI.dispose();
        mapScreen.dispose();
        fishingGame.dispose();
        kabaddiGame.dispose();
        tractorRace.dispose();
        cricketGame.dispose();
        kiteGame.dispose();
    }
}
