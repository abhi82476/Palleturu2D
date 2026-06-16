package com.villagelegends;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.villagelegends.screens.SplashScreen;
import com.villagelegends.screens.MainMenuScreen;
import com.villagelegends.screens.GameScreen;
import com.villagelegends.systems.AudioManager;
import com.villagelegends.systems.CombatSystem;
import com.villagelegends.systems.DeliverySystem;
import com.villagelegends.systems.EconomyManager;
import com.villagelegends.systems.FarmingManager;
import com.villagelegends.systems.FestivalManager;
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.systems.NPCManager;
import com.villagelegends.systems.QuestManager;
import com.villagelegends.systems.SaveManager;
import com.villagelegends.systems.TreasureHuntSystem;
import com.villagelegends.systems.VehicleManager;
import com.villagelegends.data.PlayerData;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.utils.AnimationManager;
import com.villagelegends.utils.LocalizationManager;
import com.villagelegends.world.DayNightCycle;

/**
 * Village Legends 2D — Root LibGDX Game class.
 *
 * Single instance per session. Owns all shared resources and managers.
 * Screens swap via setScreen(); the active Screen drives render().
 *
 * Manager initialisation order:
 *   1. EventBus (others subscribe during construction)
 *   2. SaveManager (needed by any screen)
 *   3. AudioManager (starts menu music on SplashScreen exit)
 *   4. AnimationManager (loads atlases)
 *   5. Game-logic managers (Quest, NPC, Economy, Farming, Vehicle …)
 */
public class VillageLegends extends Game {

    // ── Shared rendering ──────────────────────────────────────
    public SpriteBatch     batch;
    public ShapeRenderer   shapeRenderer;
    public AssetManager    assetManager;
    public AnimationManager animationManager;

    // ── Core event infrastructure ─────────────────────────────
    public GameEventBus    eventBus;

    // ── Persistence ───────────────────────────────────────────
    public SaveManager     saveManager;

    // ── Audio ─────────────────────────────────────────────────
    public AudioManager    audioManager;

    // ── World managers ────────────────────────────────────────
    public QuestManager      questManager;
    public NPCManager        npcManager;
    public EconomyManager    economyManager;
    public FarmingManager    farmingManager;
    public VehicleManager    vehicleManager;
    public CombatSystem      combatSystem;
    public FestivalManager   festivalManager;
    public DeliverySystem    deliverySystem;
    public TreasureHuntSystem treasureSystem;

    // ── Global game state ─────────────────────────────────────
    public GameSaveData activeSave;
    public String       language = "en";   // "en" | "te" | "hi"

    // ── Singleton ─────────────────────────────────────────────
    private static VillageLegends instance;
    public  static VillageLegends get() { return instance; }

    // ─────────────────────────────────────────────────────────
    @Override
    public void create() {
        instance      = this;

        // Rendering primitives
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        assetManager  = new AssetManager();

        // Infrastructure
        eventBus     = new GameEventBus();
        saveManager  = new SaveManager(this);
        audioManager = new AudioManager(this);
        animationManager = new AnimationManager();

        // Game managers (order matters — some subscribe to events)
        questManager    = new QuestManager(this);
        npcManager      = new NPCManager(this);
        economyManager  = new EconomyManager(this);
        farmingManager  = new FarmingManager(this);
        vehicleManager  = new VehicleManager(this);
        combatSystem    = new CombatSystem(this);
        festivalManager = new FestivalManager(this);
        deliverySystem  = new DeliverySystem(this);
        treasureSystem  = new TreasureHuntSystem(this);

        Gdx.app.log("VillageLegends", "All systems initialised");
        setScreen(new SplashScreen(this));
    }

    // ── Game flow ─────────────────────────────────────────────
    public void newGame(int saveSlot) {
        activeSave = saveManager.createNewSave(saveSlot);
        language   = activeSave.language;
        LocalizationManager.setLanguage(language);
        initManagersFromSave();
        setScreen(new GameScreen(this));
    }

    public void loadGame(int saveSlot) {
        activeSave = saveManager.loadGame(saveSlot);
        if (activeSave == null) {
            Gdx.app.error("VillageLegends", "Load failed slot=" + saveSlot);
            setScreen(new MainMenuScreen(this));
            return;
        }
        language = activeSave.language;
        LocalizationManager.setLanguage(language);
        initManagersFromSave();
        setScreen(new GameScreen(this));
    }

    public void returnToMenu() {
        audioManager.playMenuMusic();
        setScreen(new MainMenuScreen(this));
    }

    public void saveCurrentGame() {
        if (activeSave == null) return;
        // Sync sub-managers before writing
        farmingManager.syncToSave(activeSave);
        npcManager.syncToSave(activeSave);
        festivalManager.syncToSave(activeSave);
        economyManager.syncToSave(activeSave.playerData);
        saveManager.saveGame(activeSave);
    }

    private void initManagersFromSave() {
        economyManager.loadFromSave(activeSave.playerData);
        farmingManager.loadFromSave(activeSave);
        questManager.loadFromSave(activeSave);
        npcManager.loadFromSave(activeSave);
        festivalManager.loadFromSave(activeSave);
    }

    /**
     * Convenience accessor for the active GameScreen's DayNightCycle.
     * Returns null when no GameScreen is active (menus, loading).
     */
    public DayNightCycle dayNight() {
        if (getScreen() instanceof GameScreen) {
            return ((GameScreen) getScreen()).getDayNight();
        }
        return null;
    }

    // ── LibGDX lifecycle ──────────────────────────────────────
    @Override
    public void render() { super.render(); }

    @Override
    public void resize(int w, int h) { super.resize(w, h); }

    @Override
    public void pause() {
        super.pause();
        if (activeSave != null) saveCurrentGame();
    }

    @Override
    public void resume() { super.resume(); }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        assetManager.dispose();
        audioManager.dispose();
        animationManager.dispose();
        treasureSystem.dispose();
    }
}
