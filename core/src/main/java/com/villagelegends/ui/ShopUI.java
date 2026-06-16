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
import com.villagelegends.data.Item;
import com.villagelegends.entities.Player;
import com.villagelegends.systems.GameEventBus;

import java.util.List;

/**
 * ShopUI — full-screen two-panel shop overlay.
 *
 * LEFT panel:  Shop inventory (items available to buy)
 * RIGHT panel: Player inventory (items available to sell)
 * BOTTOM bar:  Player money, selected item details, BUY/SELL buttons
 *
 * Opened by the EventBus OPEN_SHOP event carrying the shopId.
 * Closed by the BACK button or tapping outside the panel.
 */
public class ShopUI {

    private static final float VW        = GameConstants.VIRTUAL_WIDTH;
    private static final float VH        = GameConstants.VIRTUAL_HEIGHT;
    private static final float CELL_W    = 200f;
    private static final float CELL_H    = 52f;
    private static final float LEFT_X    = 30f;
    private static final float RIGHT_X   = VW / 2f + 20f;
    private static final float LIST_Y    = VH - 100f;
    private static final int   ROWS_VISIBLE = 9;

    private final VillageLegends game;
    private final Player          player;
    private final BitmapFont      fontTitle;
    private final BitmapFont      fontBody;
    private final GlyphLayout     gl = new GlyphLayout();

    private boolean open     = false;
    private String  shopId   = null;

    // Selection state
    private int  selectedBuyIdx  = -1;
    private int  selectedSellIdx = -1;
    private int  scrollBuy       = 0;
    private int  scrollSell      = 0;

    // Action buttons
    private final Rectangle btnBuy    = new Rectangle(LEFT_X,       40, 280, 50);
    private final Rectangle btnSell   = new Rectangle(RIGHT_X,      40, 280, 50);
    private final Rectangle btnClose  = new Rectangle(VW - 100, VH - 60, 80, 44);

    // ─────────────────────────────────────────────────────────
    public ShopUI(VillageLegends game, Player player) {
        this.game   = game;
        this.player = player;
        fontTitle   = new BitmapFont(); fontTitle.getData().setScale(1.5f);
        fontBody    = new BitmapFont(); fontBody.getData().setScale(1.05f);

        // Listen for shop open events from triggers/NPCs
        game.eventBus.subscribe(GameEventBus.EventType.OPEN_SHOP, e -> {
            openShop(e.stringData());
        });
    }

    // ── Open / Close ──────────────────────────────────────────
    public void openShop(String shopId) {
        this.shopId         = shopId;
        this.open           = true;
        this.selectedBuyIdx = -1;
        this.selectedSellIdx= -1;
        this.scrollBuy      = 0;
        this.scrollSell     = 0;
        game.audioManager.playSfx("ui_open");
    }

    public void close() {
        open   = false;
        shopId = null;
    }

    public boolean isOpen() { return open; }

    // ── Render ────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (!open || shopId == null) return;

        List<Item> shopItems = game.economyManager.getShopInventory(shopId);
        List<Item> invItems  = player.getInventory();

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        drawBackground(sr);
        drawShopList(sr, shopItems);
        drawSellList(sr, invItems);
        drawActionButtons(sr);

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        drawText(batch, shopItems, invItems);
        handleInput(shopItems, invItems);
    }

    // ── Background ────────────────────────────────────────────
    private void drawBackground(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Full overlay
        sr.setColor(0f, 0f, 0f, 0.88f);
        sr.rect(0, 0, VW, VH);
        // Left panel (BUY)
        sr.setColor(0.06f, 0.12f, 0.08f, 0.95f);
        sr.rect(LEFT_X - 8, 90, VW / 2f - 40, VH - 150f);
        // Right panel (SELL)
        sr.setColor(0.10f, 0.06f, 0.12f, 0.95f);
        sr.rect(RIGHT_X - 8, 90, VW / 2f - 40, VH - 150f);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.3f, 0.7f, 0.4f, 0.8f);
        sr.rect(LEFT_X - 8, 90, VW / 2f - 40, VH - 150f);
        sr.setColor(0.7f, 0.4f, 0.7f, 0.8f);
        sr.rect(RIGHT_X - 8, 90, VW / 2f - 40, VH - 150f);
        sr.end();
    }

    // ── Shop buy list ─────────────────────────────────────────
    private void drawShopList(ShapeRenderer sr, List<Item> shopItems) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        int end = Math.min(scrollBuy + ROWS_VISIBLE, shopItems.size());
        for (int i = scrollBuy; i < end; i++) {
            float rowY = LIST_Y - (i - scrollBuy) * (CELL_H + 4);
            boolean sel = (i == selectedBuyIdx);
            sr.setColor(sel ? 0.15f : 0.08f, sel ? 0.40f : 0.18f, sel ? 0.15f : 0.08f, 0.9f);
            sr.rect(LEFT_X, rowY - CELL_H, CELL_W * 2f, CELL_H);
        }
        sr.end();
    }

    // ── Sell list (player inventory) ──────────────────────────
    private void drawSellList(ShapeRenderer sr, List<Item> inv) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        int end = Math.min(scrollSell + ROWS_VISIBLE, inv.size());
        for (int i = scrollSell; i < end; i++) {
            float rowY = LIST_Y - (i - scrollSell) * (CELL_H + 4);
            boolean sel = (i == selectedSellIdx);
            sr.setColor(sel ? 0.35f : 0.08f, sel ? 0.10f : 0.08f, sel ? 0.35f : 0.14f, 0.9f);
            sr.rect(RIGHT_X, rowY - CELL_H, CELL_W * 2f, CELL_H);
        }
        sr.end();
    }

    // ── Action buttons ────────────────────────────────────────
    private void drawActionButtons(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.55f, 0.15f, 0.88f); sr.rect(btnBuy.x,  btnBuy.y,  btnBuy.width,  btnBuy.height);
        sr.setColor(0.55f, 0.15f, 0.55f, 0.88f); sr.rect(btnSell.x, btnSell.y, btnSell.width, btnSell.height);
        sr.setColor(0.50f, 0.10f, 0.10f, 0.88f); sr.rect(btnClose.x,btnClose.y,btnClose.width,btnClose.height);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.GREEN);  sr.rect(btnBuy.x,  btnBuy.y,  btnBuy.width,  btnBuy.height);
        sr.setColor(Color.PURPLE); sr.rect(btnSell.x, btnSell.y, btnSell.width, btnSell.height);
        sr.setColor(Color.RED);    sr.rect(btnClose.x,btnClose.y,btnClose.width,btnClose.height);
        sr.end();
    }

    // ── Text rendering ────────────────────────────────────────
    private void drawText(SpriteBatch batch, List<Item> shopItems, List<Item> inv) {
        // Panel headers
        fontTitle.setColor(Color.GREEN);
        fontTitle.draw(batch, "🛒 SHOP — BUY", LEFT_X, VH - 20f);
        fontTitle.setColor(Color.MAGENTA);
        fontTitle.draw(batch, "💰 YOUR ITEMS — SELL", RIGHT_X, VH - 20f);

        // Money display
        fontTitle.setColor(Color.GOLD);
        gl.setText(fontTitle, "₹ " + player.getMoney());
        fontTitle.draw(batch, "₹ " + player.getMoney(), VW / 2f - gl.width / 2f, 55f);

        // Shop items
        int endB = Math.min(scrollBuy + ROWS_VISIBLE, shopItems.size());
        for (int i = scrollBuy; i < endB; i++) {
            Item item = shopItems.get(i);
            float rowY = LIST_Y - (i - scrollBuy) * (CELL_H + 4) - 8;
            int buyPrice = game.economyManager.getBuyPrice(item.id, player.getReputation());
            fontBody.setColor(i == selectedBuyIdx ? Color.YELLOW : Color.WHITE);
            fontBody.draw(batch, item.displayName, LEFT_X + 8, rowY);
            fontBody.setColor(Color.GOLD);
            fontBody.draw(batch, "₹" + buyPrice, LEFT_X + 280, rowY);
        }

        // Inventory items
        int endS = Math.min(scrollSell + ROWS_VISIBLE, inv.size());
        for (int i = scrollSell; i < endS; i++) {
            Item item = inv.get(i);
            if (!item.isSellable()) continue;
            float rowY = LIST_Y - (i - scrollSell) * (CELL_H + 4) - 8;
            int sellPrice = game.economyManager.getSellPrice(item.id, player.getReputation());
            fontBody.setColor(i == selectedSellIdx ? Color.YELLOW : Color.LIGHT_GRAY);
            fontBody.draw(batch, item.displayName, RIGHT_X + 8, rowY);
            fontBody.setColor(Color.GREEN);
            fontBody.draw(batch, "₹" + sellPrice, RIGHT_X + 280, rowY);
        }

        // Button labels
        fontBody.setColor(Color.WHITE);
        fontBody.draw(batch, selectedBuyIdx >= 0 ? "BUY ITEM" : "SELECT ITEM",
                btnBuy.x + 60, btnBuy.y + 34);
        fontBody.draw(batch, selectedSellIdx >= 0 ? "SELL ITEM" : "SELECT ITEM",
                btnSell.x + 55, btnSell.y + 34);
        fontBody.setColor(Color.RED);
        fontBody.draw(batch, "CLOSE", btnClose.x + 10, btnClose.y + 30);

        // Selected item details in centre
        drawSelectedDetails(batch, shopItems, inv);

        // Scroll hints
        if (shopItems.size() > ROWS_VISIBLE) {
            fontBody.setColor(0.5f, 0.5f, 0.5f, 0.8f);
            fontBody.draw(batch, "▲ ▼ swipe to scroll", LEFT_X, 125f);
        }
    }

    private void drawSelectedDetails(SpriteBatch batch, List<Item> shopItems, List<Item> inv) {
        Item sel = null;
        if (selectedBuyIdx >= 0 && selectedBuyIdx < shopItems.size())
            sel = shopItems.get(selectedBuyIdx);
        else if (selectedSellIdx >= 0 && selectedSellIdx < inv.size())
            sel = inv.get(selectedSellIdx);
        if (sel == null) return;

        fontBody.setColor(Color.CYAN);
        fontBody.draw(batch, sel.displayName + "  [" + sel.category + "]",
                VW / 2f - 120, 115f);
        fontBody.setColor(Color.LIGHT_GRAY);
        fontBody.draw(batch, sel.description != null ? sel.description : "", LEFT_X, 95f);
    }

    // ── Touch input ───────────────────────────────────────────
    private void handleInput(List<Item> shopItems, List<Item> inv) {
        if (!Gdx.input.justTouched()) return;

        float sx = Gdx.input.getX(), sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float vx = sx * VW / Gdx.graphics.getWidth();
        float vy = sy * VH / Gdx.graphics.getHeight();

        // Close button
        if (btnClose.contains(vx, vy)) { close(); return; }

        // BUY button
        if (btnBuy.contains(vx, vy) && selectedBuyIdx >= 0 && selectedBuyIdx < shopItems.size()) {
            Item item = shopItems.get(selectedBuyIdx);
            game.economyManager.buyItem(shopId, item.id, player);
            return;
        }

        // SELL button
        if (btnSell.contains(vx, vy) && selectedSellIdx >= 0 && selectedSellIdx < inv.size()) {
            Item item = inv.get(selectedSellIdx);
            game.economyManager.sellItem(item.id, player);
            selectedSellIdx = -1;
            return;
        }

        // Shop item rows (left side)
        if (vx >= LEFT_X && vx <= LEFT_X + CELL_W * 2f) {
            int endB = Math.min(scrollBuy + ROWS_VISIBLE, shopItems.size());
            for (int i = scrollBuy; i < endB; i++) {
                float rowY = LIST_Y - (i - scrollBuy) * (CELL_H + 4);
                if (vy <= rowY && vy >= rowY - CELL_H) {
                    selectedBuyIdx  = i;
                    selectedSellIdx = -1;
                    game.audioManager.playSfx("ui_click");
                    return;
                }
            }
        }

        // Inventory rows (right side)
        if (vx >= RIGHT_X && vx <= RIGHT_X + CELL_W * 2f) {
            int endS = Math.min(scrollSell + ROWS_VISIBLE, inv.size());
            for (int i = scrollSell; i < endS; i++) {
                float rowY = LIST_Y - (i - scrollSell) * (CELL_H + 4);
                if (vy <= rowY && vy >= rowY - CELL_H) {
                    selectedSellIdx = i;
                    selectedBuyIdx  = -1;
                    game.audioManager.playSfx("ui_click");
                    return;
                }
            }
        }
    }

    public void dispose() { fontTitle.dispose(); fontBody.dispose(); }
}
