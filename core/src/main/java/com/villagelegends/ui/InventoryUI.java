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
 * InventoryUI — full-screen grid overlay showing all carried items.
 *
 * Grid: 8 columns × 5 rows = 40 slots
 * Selected item shows detail panel on right side:
 *   - Name, description, category, sell price
 *   - Action buttons: USE / EQUIP / DROP / SELL
 *
 * Touch input directly handled; no Scene2D required.
 */
public class InventoryUI {

    private static final float VW       = GameConstants.VIRTUAL_WIDTH;
    private static final float VH       = GameConstants.VIRTUAL_HEIGHT;
    private static final int   COLS     = 8;
    private static final int   ROWS     = 5;
    private static final float CELL_SIZE= 70f;
    private static final float GRID_X   = 30f;
    private static final float GRID_Y   = VH - 80f - ROWS * CELL_SIZE;
    private static final float PANEL_X  = GRID_X + COLS * CELL_SIZE + 30f;
    private static final float PANEL_W  = 300f;

    private final VillageLegends game;
    private final Player          player;
    private boolean               open   = false;
    private int                   selectedSlot = -1;
    private Item                  selectedItem = null;

    private final BitmapFont fontTitle;
    private final BitmapFont fontBody;
    private final GlyphLayout gl = new GlyphLayout();

    // Detail panel buttons
    private final Rectangle btnUse  = new Rectangle(PANEL_X, 200, 120, 44);
    private final Rectangle btnDrop = new Rectangle(PANEL_X + 130, 200, 120, 44);
    private final Rectangle btnSell = new Rectangle(PANEL_X, 145, 250, 44);

    // ─────────────────────────────────────────────────────────
    public InventoryUI(VillageLegends game, Player player) {
        this.game   = game;
        this.player = player;
        fontTitle = new BitmapFont(); fontTitle.getData().setScale(1.5f);
        fontBody  = new BitmapFont(); fontBody.getData().setScale(1.05f);
    }

    // ── Toggle ────────────────────────────────────────────────
    public void toggle() {
        open = !open;
        if (!open) selectedSlot = -1;
        game.audioManager.playSfx("ui_open");
    }

    public boolean isOpen() { return open; }

    // ── Render ────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (!open) return;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Background overlay
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.80f);
        sr.rect(0, 0, VW, VH);

        // Grid background
        sr.setColor(0.1f, 0.12f, 0.14f, 0.95f);
        sr.rect(GRID_X - 8, GRID_Y - 8, COLS * CELL_SIZE + 16, ROWS * CELL_SIZE + 48);

        // Cells
        List<Item> inv = player.getInventory();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = row * COLS + col;
                float cx = GRID_X + col * CELL_SIZE;
                float cy = GRID_Y + (ROWS - 1 - row) * CELL_SIZE;
                boolean isSelected = slot == selectedSlot;
                sr.setColor(isSelected ? 0.35f : 0.18f,
                            isSelected ? 0.45f : 0.22f,
                            isSelected ? 0.35f : 0.18f, 0.9f);
                sr.rect(cx + 2, cy + 2, CELL_SIZE - 4, CELL_SIZE - 4);

                if (slot < inv.size()) {
                    // Item present — draw coloured block by category
                    Item item = inv.get(slot);
                    sr.setColor(getCatColor(item.category));
                    sr.rect(cx + 10, cy + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                }
            }
        }

        // Detail panel background
        sr.setColor(0.08f, 0.10f, 0.12f, 0.95f);
        sr.rect(PANEL_X - 10, 80, PANEL_W + 20, VH - 120f);
        sr.end();

        // Grid outline
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.4f, 0.55f, 0.4f, 0.8f);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float cx = GRID_X + col * CELL_SIZE;
                float cy = GRID_Y + (ROWS - 1 - row) * CELL_SIZE;
                sr.rect(cx + 2, cy + 2, CELL_SIZE - 4, CELL_SIZE - 4);
            }
        }
        sr.end();

        // Buttons if item selected
        if (selectedItem != null) {
            drawActionButtons(sr);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        // Text
        drawText(batch, inv);
        handleTouchInput(inv);
    }

    private void drawActionButtons(ShapeRenderer sr) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.6f, 0.2f, 0.85f); sr.rect(btnUse.x, btnUse.y, btnUse.width, btnUse.height);
        sr.setColor(0.6f, 0.2f, 0.2f, 0.85f); sr.rect(btnDrop.x, btnDrop.y, btnDrop.width, btnDrop.height);
        if (selectedItem != null && selectedItem.isSellable()) {
            sr.setColor(0.8f, 0.6f, 0.1f, 0.85f);
            sr.rect(btnSell.x, btnSell.y, btnSell.width, btnSell.height);
        }
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawText(SpriteBatch batch, List<Item> inv) {
        // Title
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "Inventory  [" + inv.size() + "/" + GameConstants.MAX_INVENTORY_SLOTS + "]",
                GRID_X, GRID_Y + ROWS * CELL_SIZE + 36f);

        // Item labels inside cells
        fontBody.getData().setScale(0.7f);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = row * COLS + col;
                if (slot >= inv.size()) continue;
                Item item = inv.get(slot);
                float cx = GRID_X + col * CELL_SIZE;
                float cy = GRID_Y + (ROWS - 1 - row) * CELL_SIZE;
                // Draw abbreviated name
                String abbr = item.displayName.length() > 7
                        ? item.displayName.substring(0, 6) + "…"
                        : item.displayName;
                fontBody.setColor(Color.WHITE);
                fontBody.draw(batch, abbr, cx + 4, cy + 16);
                if (item.quantity > 1) {
                    fontBody.setColor(Color.YELLOW);
                    fontBody.draw(batch, "×" + item.quantity, cx + 4, cy + 5);
                }
            }
        }
        fontBody.getData().setScale(1.05f);

        // Detail panel
        if (selectedItem != null) {
            float px = PANEL_X, py = VH - 130f;
            fontTitle.setColor(Color.YELLOW);
            fontTitle.draw(batch, selectedItem.displayName, px, py);
            py -= 28;
            fontBody.setColor(Color.LIGHT_GRAY);
            fontBody.draw(batch, "Type: " + selectedItem.category, px, py); py -= 22;
            fontBody.draw(batch, "Value: ₹" + selectedItem.price, px, py);  py -= 22;
            fontBody.setColor(Color.WHITE);
            // Word-wrap description
            String desc = selectedItem.description != null ? selectedItem.description : "";
            if (desc.length() > 28) {
                fontBody.draw(batch, desc.substring(0, 28), px, py); py -= 18;
                fontBody.draw(batch, desc.substring(28), px, py);
            } else {
                fontBody.draw(batch, desc, px, py);
            }

            // Button labels
            fontBody.setColor(Color.WHITE);
            fontBody.draw(batch, "USE",  btnUse.x  + 30, btnUse.y  + 30);
            fontBody.draw(batch, "DROP", btnDrop.x + 22, btnDrop.y + 30);
            if (selectedItem.isSellable()) {
                int sellPrice = game.economyManager.getSellPrice(selectedItem.id, player.getReputation());
                fontBody.setColor(Color.GOLD);
                fontBody.draw(batch, "SELL  ₹" + sellPrice, btnSell.x + 35, btnSell.y + 30);
            }
        }
    }

    // ── Touch input ───────────────────────────────────────────
    private void handleTouchInput(List<Item> inv) {
        if (!Gdx.input.justTouched()) return;
        float sx = Gdx.input.getX();
        float sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float scaleX = VW / Gdx.graphics.getWidth();
        float scaleY = VH / Gdx.graphics.getHeight();
        float vx = sx * scaleX, vy = sy * scaleY;

        // Hit-test grid cells
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = row * COLS + col;
                float cx = GRID_X + col * CELL_SIZE;
                float cy = GRID_Y + (ROWS - 1 - row) * CELL_SIZE;
                if (vx >= cx + 2 && vx <= cx + CELL_SIZE - 2
                        && vy >= cy + 2 && vy <= cy + CELL_SIZE - 2) {
                    selectedSlot = slot;
                    selectedItem = slot < inv.size() ? inv.get(slot) : null;
                    game.audioManager.playSfx("ui_click");
                    return;
                }
            }
        }

        // Action buttons
        if (selectedItem != null) {
            if (btnSell.contains(vx, vy) && selectedItem.isSellable()) {
                game.economyManager.sellItem(selectedItem.id, player);
                selectedItem = null; selectedSlot = -1;
            } else if (btnDrop.contains(vx, vy)) {
                player.removeItem(selectedItem);
                selectedItem = null; selectedSlot = -1;
            } else if (btnUse.contains(vx, vy)) {
                // Use item (heal potion, pesticide, etc.) — resolved by EconomyManager/Player
                useSelectedItem();
            }
        }
    }

    private void useSelectedItem() {
        if (selectedItem == null) return;
        switch (selectedItem.id) {
            case "medicine":
                player.heal(40);
                player.removeItem(selectedItem);
                game.audioManager.playSfx("ui_click");
                break;
            default:
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION,
                        "Can't use " + selectedItem.displayName + " from here."));
        }
        selectedItem = null; selectedSlot = -1;
    }

    private Color getCatColor(Item.Category cat) {
        switch (cat) {
            case SEED:       return new Color(0.3f, 0.8f, 0.3f, 0.85f);
            case CROP:       return new Color(0.7f, 0.8f, 0.2f, 0.85f);
            case TOOL:       return new Color(0.5f, 0.5f, 0.8f, 0.85f);
            case WEAPON:     return new Color(0.8f, 0.3f, 0.2f, 0.85f);
            case CONSUMABLE: return new Color(0.9f, 0.4f, 0.7f, 0.85f);
            case QUEST_ITEM: return new Color(1.0f, 0.8f, 0.0f, 0.9f);
            case FISH:       return new Color(0.2f, 0.5f, 0.9f, 0.85f);
            case FUEL:       return new Color(0.8f, 0.5f, 0.1f, 0.85f);
            default:         return new Color(0.5f, 0.5f, 0.5f, 0.85f);
        }
    }

    public void dispose() { fontTitle.dispose(); fontBody.dispose(); }
}
