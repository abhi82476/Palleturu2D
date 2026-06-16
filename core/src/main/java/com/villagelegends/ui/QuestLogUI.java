package com.villagelegends.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Quest;
import com.villagelegends.data.QuestObjective;
import com.villagelegends.systems.GameEventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * QuestLogUI — full-screen quest tracker.
 *
 * LEFT column:  Quest list with filter tabs (ALL | MAIN | SIDE | DONE)
 * RIGHT column: Selected quest details with objectives and progress bars
 *
 * Displays:
 *  - Quest title and description
 *  - All objectives with progress bars
 *  - Reward summary
 *  - Story act indicator for main quests
 */
public class QuestLogUI {

    private static final float VW = GameConstants.VIRTUAL_WIDTH;
    private static final float VH = GameConstants.VIRTUAL_HEIGHT;

    private final VillageLegends game;
    private final BitmapFont      fontTitle;
    private final BitmapFont      fontBody;
    private final GlyphLayout     gl = new GlyphLayout();

    private boolean open          = false;
    private String  filter        = "ALL";   // ALL | MAIN | SIDE | DONE
    private int     selectedIdx   = 0;
    private int     scrollOffset  = 0;

    private static final int  ROWS_VISIBLE = 14;
    private static final float CELL_H      = 46f;
    private static final float LIST_X      = 20f;
    private static final float LIST_W      = 390f;
    private static final float LIST_TOP    = VH - 120f;
    private static final float DETAIL_X    = LIST_X + LIST_W + 24f;
    private static final float DETAIL_W    = VW - DETAIL_X - 20f;

    // Filter tabs
    private final String[] FILTERS = {"ALL", "MAIN", "SIDE", "DONE"};
    private final Rectangle[] filterBtns = new Rectangle[4];
    private final Rectangle   btnClose   = new Rectangle(VW - 90, VH - 58, 76, 44);

    // ─────────────────────────────────────────────────────────
    public QuestLogUI(VillageLegends game) {
        this.game  = game;
        fontTitle  = new BitmapFont(); fontTitle.getData().setScale(1.4f);
        fontBody   = new BitmapFont(); fontBody.getData().setScale(1.0f);

        for (int i = 0; i < 4; i++) {
            filterBtns[i] = new Rectangle(LIST_X + i * 100f, VH - 90f, 94f, 38f);
        }
    }

    // ── Toggle ────────────────────────────────────────────────
    public void toggle() {
        open = !open;
        if (open) game.audioManager.playSfx("ui_open");
    }

    public boolean isOpen() { return open; }

    // ── Render ────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (!open) return;

        List<Quest> filtered = getFilteredQuests();

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        drawPanels(sr, filtered);
        drawFilterTabs(sr);

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawTitles(batch, filtered);
        drawQuestList(batch, filtered);
        drawQuestDetail(batch, filtered);
        drawStats(batch);

        handleInput(filtered);
    }

    // ── Panel backgrounds ─────────────────────────────────────
    private void drawPanels(ShapeRenderer sr, List<Quest> filtered) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.88f);   sr.rect(0, 0, VW, VH);
        sr.setColor(0.05f, 0.08f, 0.12f, 0.95f);
        sr.rect(LIST_X - 6, 30, LIST_W + 12, VH - 60f);
        sr.setColor(0.08f, 0.06f, 0.12f, 0.95f);
        sr.rect(DETAIL_X - 6, 30, DETAIL_W + 12, VH - 60f);

        // Highlight selected row
        if (selectedIdx >= scrollOffset && selectedIdx < scrollOffset + ROWS_VISIBLE
                && selectedIdx < filtered.size()) {
            int rel = selectedIdx - scrollOffset;
            float rowY = LIST_TOP - rel * (CELL_H + 2);
            sr.setColor(0.15f, 0.30f, 0.45f, 0.85f);
            sr.rect(LIST_X, rowY - CELL_H, LIST_W, CELL_H);
        }

        // Close button
        sr.setColor(0.5f, 0.08f, 0.08f, 0.9f);
        sr.rect(btnClose.x, btnClose.y, btnClose.width, btnClose.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.3f, 0.5f, 0.7f, 0.7f);
        sr.rect(LIST_X - 6, 30, LIST_W + 12, VH - 60f);
        sr.setColor(0.5f, 0.3f, 0.7f, 0.7f);
        sr.rect(DETAIL_X - 6, 30, DETAIL_W + 12, VH - 60f);
        sr.setColor(Color.RED);
        sr.rect(btnClose.x, btnClose.y, btnClose.width, btnClose.height);
        sr.end();
    }

    // ── Filter tabs ───────────────────────────────────────────
    private void drawFilterTabs(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 4; i++) {
            boolean active = FILTERS[i].equals(filter);
            sr.setColor(active ? 0.2f : 0.08f, active ? 0.45f : 0.15f, active ? 0.2f : 0.08f, 0.9f);
            sr.rect(filterBtns[i].x, filterBtns[i].y, filterBtns[i].width, filterBtns[i].height);
        }
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < 4; i++) {
            boolean active = FILTERS[i].equals(filter);
            sr.setColor(active ? Color.CYAN : Color.GRAY);
            sr.rect(filterBtns[i].x, filterBtns[i].y, filterBtns[i].width, filterBtns[i].height);
        }
        sr.end();
    }

    // ── Text layers ───────────────────────────────────────────
    private void drawTitles(SpriteBatch batch, List<Quest> filtered) {
        fontTitle.getData().setScale(1.8f);
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "Quest Log", LIST_X, VH - 20f);

        // Stats line
        fontBody.setColor(Color.LIGHT_GRAY);
        fontBody.getData().setScale(0.9f);
        int done  = game.questManager.getCompletedCount();
        int total = game.questManager.getTotalQuests();
        fontBody.draw(batch, "Completed: " + done + " / " + total
                + "   Active: " + game.questManager.getActiveQuestIds().size(),
                LIST_X, VH - 42f);
        fontBody.getData().setScale(1.0f);
        fontTitle.getData().setScale(1.4f);

        // Filter tab labels
        for (int i = 0; i < 4; i++) {
            boolean active = FILTERS[i].equals(filter);
            fontBody.setColor(active ? Color.CYAN : Color.GRAY);
            fontBody.draw(batch, FILTERS[i],
                    filterBtns[i].x + 12, filterBtns[i].y + 26);
        }

        // Close
        fontBody.setColor(Color.RED);
        fontBody.draw(batch, "CLOSE", btnClose.x + 8, btnClose.y + 28);

        // Empty state
        if (filtered.isEmpty()) {
            fontBody.setColor(Color.GRAY);
            fontBody.draw(batch, "No quests in this category.", LIST_X + 20, VH / 2f);
        }
    }

    private void drawQuestList(SpriteBatch batch, List<Quest> filtered) {
        int end = Math.min(scrollOffset + ROWS_VISIBLE, filtered.size());
        for (int i = scrollOffset; i < end; i++) {
            Quest q       = filtered.get(i);
            Quest.State st= game.questManager.getState(q.id);
            float rowY    = LIST_TOP - (i - scrollOffset) * (CELL_H + 2);

            // Status indicator
            String marker;
            Color col;
            if (st == Quest.State.COMPLETE)       { marker = "✓ "; col = Color.GREEN; }
            else if (st == Quest.State.ACTIVE)    { marker = "▶ "; col = Color.YELLOW; }
            else if (st == Quest.State.AVAILABLE) { marker = "! "; col = Color.ORANGE; }
            else                                  { marker = "○ "; col = Color.DARK_GRAY; }

            fontBody.setColor(col);
            fontBody.draw(batch, marker, LIST_X + 4, rowY - 10);
            fontBody.setColor(i == selectedIdx ? Color.WHITE : (st == Quest.State.COMPLETE ? new Color(0.6f,0.7f,0.6f,1f) : col));
            String displayTitle = q.title.length() > 26 ? q.title.substring(0, 25) + "…" : q.title;
            fontBody.draw(batch, displayTitle, LIST_X + 28, rowY - 10);

            // Quest type badge
            fontBody.getData().setScale(0.75f);
            fontBody.setColor(q.questType == Quest.Type.MAIN ? Color.GOLD : Color.LIGHT_GRAY);
            fontBody.draw(batch, "[" + q.questType + "]", LIST_X + 270, rowY - 10);
            fontBody.getData().setScale(1.0f);
        }

        // Scroll indicator
        if (filtered.size() > ROWS_VISIBLE) {
            fontBody.setColor(Color.GRAY);
            fontBody.getData().setScale(0.8f);
            fontBody.draw(batch, scrollOffset + 1 + "–" + Math.min(scrollOffset + ROWS_VISIBLE, filtered.size())
                    + " of " + filtered.size(), LIST_X, 45f);
            fontBody.getData().setScale(1.0f);
        }
    }

    private void drawQuestDetail(SpriteBatch batch, List<Quest> filtered) {
        if (filtered.isEmpty() || selectedIdx >= filtered.size()) return;
        Quest q  = filtered.get(selectedIdx);
        Quest.State st = game.questManager.getState(q.id);

        float px = DETAIL_X + 8, py = VH - 90f;

        // Quest type tag
        fontBody.getData().setScale(0.85f);
        Color typeColor = q.questType == Quest.Type.MAIN ? Color.GOLD
                : q.questType == Quest.Type.FESTIVAL ? Color.MAGENTA : Color.CYAN;
        fontBody.setColor(typeColor);
        fontBody.draw(batch, "[ " + q.questType + " QUEST ]", px, py); py -= 20;
        fontBody.getData().setScale(1.0f);

        // Title
        fontTitle.setColor(Color.WHITE);
        fontTitle.getData().setScale(1.6f);
        fontTitle.draw(batch, q.title, px, py); py -= 34;
        fontTitle.getData().setScale(1.4f);

        // Status
        fontBody.setColor(getStateColor(st));
        fontBody.draw(batch, "Status: " + st.name(), px, py); py -= 24;

        // Description (word wrap at 36 chars)
        fontBody.setColor(new Color(0.8f, 0.8f, 0.85f, 1f));
        String desc = q.description;
        while (desc.length() > 36) {
            fontBody.draw(batch, desc.substring(0, 36), px, py); py -= 18;
            desc = desc.substring(36);
        }
        fontBody.draw(batch, desc, px, py); py -= 30;

        // Objectives
        if (st == Quest.State.ACTIVE || st == Quest.State.COMPLETE) {
            fontBody.setColor(Color.CYAN);
            fontBody.draw(batch, "Objectives:", px, py); py -= 22;

            for (QuestObjective obj : q.getObjectives()) {
                float barW = DETAIL_W - 20f;
                float prog  = obj.requiredCount > 0
                        ? (float) obj.getProgress() / obj.requiredCount : 0;

                fontBody.setColor(obj.isComplete() ? Color.GREEN : Color.WHITE);
                String label = (obj.isComplete() ? "✓ " : "  ") + obj.getStatusText();
                if (label.length() > 38) label = label.substring(0, 37) + "…";
                fontBody.draw(batch, label, px, py); py -= 16;

                // Progress bar
                batch.end();
                ShapeRenderer sr = game.shapeRenderer;
                sr.setProjectionMatrix(batch.getProjectionMatrix());
                Gdx.gl.glEnable(GL20.GL_BLEND);
                sr.begin(ShapeRenderer.ShapeType.Filled);
                sr.setColor(0.2f, 0.2f, 0.2f, 0.8f); sr.rect(px, py - 2, barW, 8);
                sr.setColor(obj.isComplete() ? Color.GREEN : Color.CYAN);
                sr.rect(px, py - 2, barW * prog, 8);
                sr.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);
                batch.begin();
                py -= 18;
            }
        }

        // Rewards
        py -= 10;
        fontBody.setColor(Color.GOLD);
        fontBody.draw(batch, "Rewards:", px, py); py -= 20;
        fontBody.setColor(Color.YELLOW);
        if (q.rewardMoney > 0) { fontBody.draw(batch, "  ₹ " + q.rewardMoney, px, py); py -= 18; }
        if (q.rewardRep   > 0) { fontBody.draw(batch, "  +" + q.rewardRep + " Reputation", px, py); py -= 18; }
        if (q.rewardItem != null) { fontBody.draw(batch, "  Item: " + q.rewardItem, px, py); py -= 18; }

        // Start quest button (if AVAILABLE)
        if (st == Quest.State.AVAILABLE) {
            fontTitle.setColor(Color.LIME);
            fontTitle.draw(batch, "[ Talk to quest giver to start ]", px, py - 20);
        }
    }

    private void drawStats(SpriteBatch batch) {
        // Bottom: quick summary of overall progress
        fontBody.getData().setScale(0.85f);
        fontBody.setColor(new Color(0.4f, 0.6f, 0.4f, 1f));
        fontBody.draw(batch, "Rep: " + game.activeSave.playerData.reputation
                + "  |  Day " + game.activeSave.currentDay
                + "  |  ₹" + game.activeSave.playerData.money,
                LIST_X, 48f);
        fontBody.getData().setScale(1.0f);
    }

    private Color getStateColor(Quest.State st) {
        if (st == Quest.State.COMPLETE)  return Color.GREEN;
        if (st == Quest.State.ACTIVE)    return Color.YELLOW;
        if (st == Quest.State.AVAILABLE) return Color.ORANGE;
        if (st == Quest.State.FAILED)    return Color.RED;
        return Color.GRAY;
    }

    // ── Filtered quest list ───────────────────────────────────
    private List<Quest> getFilteredQuests() {
        Collection<Quest> all = game.questManager.getAllQuests();
        List<Quest> result = new ArrayList<>();
        for (Quest q : all) {
            Quest.State st = game.questManager.getState(q.id);
            if (st == Quest.State.LOCKED) continue;
            switch (filter) {
                case "MAIN": if (q.questType != Quest.Type.MAIN)     continue; break;
                case "SIDE": if (q.questType == Quest.Type.MAIN)     continue; break;
                case "DONE": if (st != Quest.State.COMPLETE)          continue; break;
            }
            result.add(q);
        }
        return result;
    }

    // ── Touch input ───────────────────────────────────────────
    private void handleInput(List<Quest> filtered) {
        if (!Gdx.input.justTouched()) return;
        float sx = Gdx.input.getX(), sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float vx = sx * VW / Gdx.graphics.getWidth();
        float vy = sy * VH / Gdx.graphics.getHeight();

        if (btnClose.contains(vx, vy)) { toggle(); return; }

        // Filter tabs
        for (int i = 0; i < 4; i++) {
            if (filterBtns[i].contains(vx, vy)) {
                filter      = FILTERS[i];
                selectedIdx = 0;
                scrollOffset= 0;
                game.audioManager.playSfx("ui_click");
                return;
            }
        }

        // List rows
        if (vx >= LIST_X && vx <= LIST_X + LIST_W) {
            int end = Math.min(scrollOffset + ROWS_VISIBLE, filtered.size());
            for (int i = scrollOffset; i < end; i++) {
                float rowY = LIST_TOP - (i - scrollOffset) * (CELL_H + 2);
                if (vy <= rowY && vy >= rowY - CELL_H) {
                    selectedIdx = i;
                    game.audioManager.playSfx("ui_click");
                    return;
                }
            }

            // Swipe scroll (simplified: up/down region)
            if (vy < 100 && scrollOffset + ROWS_VISIBLE < filtered.size()) scrollOffset++;
            if (vy > VH - 130 && scrollOffset > 0) scrollOffset--;
        }
    }

    public void dispose() { fontTitle.dispose(); fontBody.dispose(); }
}
