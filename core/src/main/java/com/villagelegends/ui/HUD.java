package com.villagelegends.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Quest;
import com.villagelegends.data.QuestObjective;
import com.villagelegends.entities.Player;
import com.villagelegends.screens.GameScreen;
import com.villagelegends.world.DayNightCycle;
import com.villagelegends.systems.GameEventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD draws all 2D overlay elements in screen-space (UI viewport):
 *
 *  TOP-LEFT:   Health bar  |  Stamina bar
 *  TOP-CENTER: Clock + Day counter  |  Weather icon  |  Festival banner
 *  TOP-RIGHT:  Money  |  Reputation star
 *  BOTTOM-LEFT: Active quest objectives (up to 3 lines)
 *  MINIMAP:    Bottom-right 160×160 circle minimap
 *  CENTRE:     Floating notifications (fade in/out)
 *  PAUSE MENU: Semi-transparent overlay when paused
 */
public class HUD {

    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    // ── Fonts ─────────────────────────────────────────────────
    private final BitmapFont fontLarge;
    private final BitmapFont fontMedium;
    private final BitmapFont fontSmall;
    private final GlyphLayout gl = new GlyphLayout();

    // ── References ────────────────────────────────────────────
    private final VillageLegends game;
    private final Player          player;

    // ── Notification queue ────────────────────────────────────
    private static final int   MAX_NOTIFS   = 4;
    private static final float NOTIF_LIFE   = 3.5f;
    private static final float NOTIF_FADE   = 0.6f;

    private final List<Notification> notifications = new ArrayList<>();

    // ── Minimap ───────────────────────────────────────────────
    private static final float MM_SIZE   = 140f;
    private static final float MM_X      = VW - MM_SIZE - 12f;
    private static final float MM_Y      = 12f;
    private static final float MM_SCALE  = MM_SIZE / (GameConstants.WORLD_WIDTH_TILES
                                           * GameConstants.TILE_SIZE);

    // ── Pause menu buttons ────────────────────────────────────
    private final com.badlogic.gdx.math.Rectangle pResume   = new com.badlogic.gdx.math.Rectangle(480, 380, 320, 56);
    private final com.badlogic.gdx.math.Rectangle pSave     = new com.badlogic.gdx.math.Rectangle(480, 305, 320, 56);
    private final com.badlogic.gdx.math.Rectangle pSettings = new com.badlogic.gdx.math.Rectangle(480, 230, 320, 56);
    private final com.badlogic.gdx.math.Rectangle pQuit     = new com.badlogic.gdx.math.Rectangle(480, 155, 320, 56);

    // ─────────────────────────────────────────────────────────
    public HUD(VillageLegends game, Player player) {
        this.game   = game;
        this.player = player;

        fontLarge  = new BitmapFont(); fontLarge.getData().setScale(1.8f);
        fontMedium = new BitmapFont(); fontMedium.getData().setScale(1.3f);
        fontSmall  = new BitmapFont(); fontSmall.getData().setScale(0.95f);

        // Subscribe for notifications
        game.eventBus.subscribe(GameEventBus.EventType.SHOW_NOTIFICATION, e ->
                pushNotification(e.stringData()));
        game.eventBus.subscribe(GameEventBus.EventType.QUEST_COMPLETE, e ->
                pushNotification("✓ Quest Complete: " + e.stringData()));
        game.eventBus.subscribe(GameEventBus.EventType.VEHICLE_FUEL_LOW, e ->
                pushNotification("⚠ Low fuel!"));
        game.eventBus.subscribe(GameEventBus.EventType.PEST_ATTACK, e ->
                pushNotification("🐛 Pest on " + e.stringData() + " crop!"));
        game.eventBus.subscribe(GameEventBus.EventType.FESTIVAL_START, e ->
                pushNotification("🎉 " + e.stringData() + " festival has started!"));
    }

    // ── Main draw entry point ──────────────────────────────────
    public void draw(SpriteBatch batch, float delta, DayNightCycle dayNight, Player player) {
        updateNotifications(delta);

        // HUD is always drawn in full bright colour regardless of day/night tint
        batch.setColor(Color.WHITE);

        drawHealthStamina(batch);
        drawClock(batch, dayNight);
        drawMoney(batch);
        drawReputation(batch);
        drawActiveQuest(batch);
        drawMinimap(batch, player, dayNight);
        drawNotifications(batch);
        drawFestivalBanner(batch);

        // Check pause button input
        if (game.getScreen() instanceof GameScreen) {
            GameScreen gs = (GameScreen) game.getScreen();
            // MobileControls fires justPaused via eventbus; handled in GameScreen
        }
    }

    // ── Health & Stamina ──────────────────────────────────────
    private void drawHealthStamina(SpriteBatch batch) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        float barW = 190f, barH = 14f;
        float hpX  = 12f,  hpY  = VH - 36f;
        float stX  = 12f,  stY  = VH - 58f;

        // Health bar
        sr.setColor(0.15f, 0.15f, 0.15f, 0.75f);
        sr.rect(hpX, hpY, barW, barH);
        float hpRatio = (float) player.getHealth() / player.getMaxHealth();
        Color hpCol   = hpRatio > 0.5f ? Color.GREEN : hpRatio > 0.25f ? Color.YELLOW : Color.RED;
        sr.setColor(hpCol.r, hpCol.g, hpCol.b, 0.9f);
        sr.rect(hpX, hpY, barW * hpRatio, barH);

        // Stamina bar
        sr.setColor(0.15f, 0.15f, 0.15f, 0.75f);
        sr.rect(stX, stY, barW, barH - 4);
        float stRatio = player.getStamina() / GameConstants.PLAYER_STAMINA_MAX;
        sr.setColor(0.3f, 0.6f, 1.0f, 0.85f);
        sr.rect(stX, stY, (barW - 4) * stRatio, barH - 4);

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        fontSmall.setColor(Color.WHITE);
        fontSmall.draw(batch, "HP " + player.getHealth() + "/" + player.getMaxHealth(),
                hpX + 4, hpY + barH - 1);
        fontSmall.draw(batch, "STA " + (int) player.getStamina(), stX + 4, stY + barH - 5);
    }

    // ── Clock ─────────────────────────────────────────────────
    private void drawClock(SpriteBatch batch, DayNightCycle dayNight) {
        String time = dayNight.getTimeString();
        String day  = "Day " + dayNight.getDay();

        fontMedium.setColor(dayNight.isDay() ? Color.YELLOW : Color.CYAN);
        gl.setText(fontMedium, time);
        fontMedium.draw(batch, time, VW / 2f - gl.width / 2f, VH - 14f);

        fontSmall.setColor(Color.LIGHT_GRAY);
        gl.setText(fontSmall, day);
        fontSmall.draw(batch, day, VW / 2f - gl.width / 2f, VH - 34f);

        // Weather icon text
        String wIcon = getWeatherIcon();
        fontMedium.draw(batch, wIcon, VW / 2f + 70f, VH - 14f);
    }

    private String getWeatherIcon() {
        if (game.activeSave == null) return "☀";
        // In full build: reference WeatherSystem via GameScreen
        return "☀";
    }

    // ── Money ─────────────────────────────────────────────────
    private void drawMoney(SpriteBatch batch) {
        String text = "₹ " + player.getMoney();
        fontLarge.setColor(Color.GOLD);
        gl.setText(fontLarge, text);
        fontLarge.draw(batch, text, VW - gl.width - 160f, VH - 12f);
    }

    // ── Reputation ────────────────────────────────────────────
    private void drawReputation(SpriteBatch batch) {
        int rep = player.getReputation();
        String label = rep >= GameConstants.REP_LEGEND ? "LEGEND"
                     : rep >= GameConstants.REP_HERO_MIN ? "Hero"
                     : rep >= 0 ? "Neutral"
                     : "Outlaw";
        Color c = rep >= GameConstants.REP_HERO_MIN ? Color.GREEN
                : rep >= 0 ? Color.YELLOW : Color.RED;
        fontSmall.setColor(c);
        fontSmall.draw(batch, "★ " + rep + " – " + label, VW - 180f, VH - 38f);
    }

    // ── Active quest objectives ───────────────────────────────
    private void drawActiveQuest(SpriteBatch batch) {
        List<String> activeIds = game.questManager.getActiveQuestIds();
        if (activeIds.isEmpty()) return;

        String qid = activeIds.get(0);
        Quest  q   = game.questManager.getQuest(qid);
        if (q == null) return;

        float x = 12f, y = 185f;
        fontSmall.setColor(Color.CYAN);
        fontSmall.getData().setScale(1.1f);
        fontSmall.draw(batch, "▶ " + q.title, x, y);
        y -= 20;

        fontSmall.getData().setScale(0.9f);
        int shown = 0;
        for (QuestObjective obj : q.getObjectives()) {
            if (shown >= 3) break;
            fontSmall.setColor(obj.isComplete() ? Color.GREEN : Color.LIGHT_GRAY);
            fontSmall.draw(batch, (obj.isComplete() ? "✓ " : "○ ") + obj.getStatusText(), x + 8, y);
            y -= 17;
            shown++;
        }
        fontSmall.getData().setScale(0.95f);
    }

    // ── Minimap ───────────────────────────────────────────────
    private void drawMinimap(SpriteBatch batch, Player player, DayNightCycle dayNight) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        sr.setColor(0.05f, 0.1f, 0.05f, 0.75f);
        sr.rect(MM_X - 2, MM_Y - 2, MM_SIZE + 4, MM_SIZE + 4);

        // Region colour
        sr.setColor(0.2f, 0.45f, 0.2f, 0.85f);
        sr.rect(MM_X, MM_Y, MM_SIZE, MM_SIZE);

        // Player blip
        float px = player.getX() * MM_SCALE;
        float py = player.getY() * MM_SCALE;
        px = MathUtils.clamp(px, 0, MM_SIZE - 4);
        py = MathUtils.clamp(py, 0, MM_SIZE - 4);
        sr.setColor(Color.YELLOW);
        sr.circle(MM_X + px, MM_Y + py, 4, 8);

        // NPC blips
        for (com.villagelegends.entities.NPC npc : game.npcManager.getActiveNPCs()) {
            float nx = MathUtils.clamp(npc.getX() * MM_SCALE, 0, MM_SIZE - 3);
            float ny = MathUtils.clamp(npc.getY() * MM_SCALE, 0, MM_SIZE - 3);
            sr.setColor(npc.isHostile() ? Color.RED : Color.WHITE);
            sr.circle(MM_X + nx, MM_Y + ny, 2.5f, 6);
        }

        sr.end();

        // Border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.6f, 0.8f, 0.6f, 0.8f);
        sr.rect(MM_X - 2, MM_Y - 2, MM_SIZE + 4, MM_SIZE + 4);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        // Region label
        fontSmall.setColor(Color.WHITE);
        fontSmall.getData().setScale(0.8f);
        fontSmall.draw(batch, game.activeSave.playerData.currentRegion.replace("_", " "),
                MM_X, MM_Y + MM_SIZE + 14f);
        fontSmall.getData().setScale(0.95f);
    }

    // ── Notifications ─────────────────────────────────────────
    private void pushNotification(String text) {
        if (notifications.size() >= MAX_NOTIFS) notifications.remove(0);
        notifications.add(new Notification(text, NOTIF_LIFE));
        game.audioManager.playSfx("ui_click");
    }

    private void updateNotifications(float delta) {
        notifications.removeIf(n -> {
            n.life -= delta;
            return n.life <= 0;
        });
    }

    private void drawNotifications(SpriteBatch batch) {
        float y = VH / 2f + 80f;
        for (Notification n : notifications) {
            float alpha = n.life > NOTIF_FADE ? 1f : n.life / NOTIF_FADE;
            fontMedium.setColor(1f, 0.95f, 0.4f, alpha);
            gl.setText(fontMedium, n.text);
            fontMedium.draw(batch, n.text, VW / 2f - gl.width / 2f, y);
            y += 28f;
        }
    }

    // ── Festival banner ───────────────────────────────────────
    private void drawFestivalBanner(SpriteBatch batch) {
        if (!game.festivalManager.isFestivalActive()) return;
        String name = game.festivalManager.getFestivalName();
        fontLarge.setColor(1f, 0.85f, 0.1f, 0.9f);
        gl.setText(fontLarge, name);
        fontLarge.draw(batch, name, VW / 2f - gl.width / 2f, VH - 8f);
    }

    // ── Pause menu ────────────────────────────────────────────
    public void drawPauseMenu(SpriteBatch batch, GameScreen gameScreen) {
        fontLarge.setColor(Color.WHITE);
        fontLarge.getData().setScale(2.5f);
        gl.setText(fontLarge, "PAUSED");
        fontLarge.draw(batch, "PAUSED", VW / 2f - gl.width / 2f, 520f);
        fontLarge.getData().setScale(1.8f);

        drawMenuBtn(batch, pResume,   "Resume Game",    Color.GREEN);
        drawMenuBtn(batch, pSave,     "Save Game",      Color.CYAN);
        drawMenuBtn(batch, pSettings, "Settings",       Color.YELLOW);
        drawMenuBtn(batch, pQuit,     "Quit to Menu",   Color.FIREBRICK);

        handlePauseInput(gameScreen);
    }

    private void drawMenuBtn(SpriteBatch batch, com.badlogic.gdx.math.Rectangle r,
                             String label, Color col) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(col.r * 0.2f, col.g * 0.2f, col.b * 0.2f, 0.85f);
        sr.rect(r.x, r.y, r.width, r.height);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(col);
        sr.rect(r.x, r.y, r.width, r.height);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        fontMedium.setColor(col);
        gl.setText(fontMedium, label);
        fontMedium.draw(batch, label, r.x + r.width / 2f - gl.width / 2f,
                r.y + r.height / 2f + gl.height / 2f);
    }

    private void handlePauseInput(GameScreen gameScreen) {
        if (!Gdx.input.justTouched()) return;
        float sx = Gdx.input.getX(), sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float scaleX = VW / Gdx.graphics.getWidth();
        float scaleY = VH / Gdx.graphics.getHeight();
        float vx = sx * scaleX, vy = sy * scaleY;

        if (pResume.contains(vx, vy))   { gameScreen.togglePause(); game.audioManager.playSfx("ui_click"); }
        if (pSave.contains(vx, vy))     { game.saveCurrentGame(); pushNotification("Game saved!"); }
        if (pSettings.contains(vx, vy)) { game.setScreen(new com.villagelegends.screens.SettingsScreen(game)); }
        if (pQuit.contains(vx, vy))     { gameScreen.saveAndQuit(); }
    }

    // ── Cleanup ───────────────────────────────────────────────
    public void dispose() {
        fontLarge.dispose();
        fontMedium.dispose();
        fontSmall.dispose();
    }

    // ── Inner notification record ─────────────────────────────
    private static class Notification {
        String text;
        float  life;
        Notification(String t, float l) { text = t; life = l; }
    }
}
