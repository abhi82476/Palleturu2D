package com.villagelegends.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Item;
import com.villagelegends.data.PlayerData;
import com.villagelegends.ui.MobileControls;
import com.villagelegends.systems.GameEventBus;
import com.villagelegends.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Player entity.
 *
 * State machine:
 *   IDLE → WALK → RUN → ATTACK → DODGE → IN_VEHICLE → FARMING → FISHING → DIALOGUE
 *
 * Input comes from MobileControls (virtual joystick + buttons).
 * Collision is handled against the World's tile collision layer.
 */
public class Player extends Entity {

    // ─── State ───────────────────────────────────────────────
    public enum State {
        IDLE, WALK, RUN, ATTACK, DODGE, IN_VEHICLE, FARMING, FISHING, DIALOGUE
    }

    private State state        = State.IDLE;
    private State prevState    = State.IDLE;

    // ─── Stats ───────────────────────────────────────────────
    private int   money;
    private int   reputation;
    private float stamina;
    private float staminaDrain  = 0;
    private float invincible    = 0;   // countdown timer
    private float attackCooldown = 0;
    private float dodgeTimer    = 0;
    private float dodgeDirX, dodgeDirY;

    // ─── Inventory ───────────────────────────────────────────
    private final List<Item>  inventory = new ArrayList<>();
    private Item              equippedItem = null;
    private int               hotbarSlot = 0;

    // ─── Combat Stats ────────────────────────────────────────
    private int  attackDamage   = GameConstants.STICK_DAMAGE;
    private boolean blocking    = false;

    // ─── Vehicle ─────────────────────────────────────────────
    private Vehicle mountedVehicle = null;

    // ─── Animations ──────────────────────────────────────────
    // In production loaded from TextureAtlas; using placeholder colours here
    private static final float FRAME_DURATION = 0.12f;

    // ─── VillageLegends ref ───────────────────────────────────
    private final VillageLegends game;

    // ─────────────────────────────────────────────────────────
    public Player(VillageLegends game, float startX, float startY) {
        super(startX, startY, 24f, 32f);
        this.game    = game;
        this.health  = GameConstants.PLAYER_HEALTH_MAX;
        this.maxHealth = GameConstants.PLAYER_HEALTH_MAX;
        this.stamina = GameConstants.PLAYER_STAMINA_MAX;

        PlayerData pd = game.activeSave.playerData;
        this.money      = pd.money;
        this.reputation = pd.reputation;
    }

    // ─────────────────────────────────────────────────────────
    @Override
    public void update(float delta) {
        // Called by GameScreen; overloaded version below has more context
    }

    /** Full update with input and world context */
    public void update(float delta, MobileControls controls, World world) {
        stateTime    += delta;
        invincible    = Math.max(0, invincible    - delta);
        attackCooldown = Math.max(0, attackCooldown - delta);
        dodgeTimer    = Math.max(0, dodgeTimer    - delta);

        if (state == State.IN_VEHICLE) {
            updateVehicle(delta, controls, world);
            return;
        }

        if (state == State.DIALOGUE) return;  // frozen during dialogue

        handleMovement(delta, controls, world);
        handleCombatInput(controls);
        handleInteractInput(controls, world);
        regenStamina(delta);
    }

    // ── Movement ─────────────────────────────────────────────
    private void handleMovement(float delta, MobileControls ctrl, World world) {
        if (state == State.DODGE) {
            // Apply dodge impulse
            float ds = GameConstants.PLAYER_RUN_SPEED * 2.2f;
            tryMove(dodgeDirX * ds * delta, dodgeDirY * ds * delta, world);
            if (dodgeTimer <= 0) state = State.IDLE;
            return;
        }

        if (state == State.ATTACK) return;  // locked during attack swing

        float jx = ctrl.getJoystickX();
        float jy = ctrl.getJoystickY();
        boolean sprint = ctrl.isSprinting();
        boolean hasInput = Math.abs(jx) > 0.1f || Math.abs(jy) > 0.1f;

        if (hasInput) {
            // Consume stamina for sprinting
            float baseSpeed = sprint && stamina > 0
                    ? GameConstants.PLAYER_RUN_SPEED * GameConstants.PLAYER_SPRINT_BOOST
                    : GameConstants.PLAYER_SPEED;
            if (sprint) {
                stamina -= GameConstants.PLAYER_STAMINA_DRAIN * delta;
                stamina = Math.max(0, stamina);
            }

            float dx = jx * baseSpeed * delta;
            float dy = jy * baseSpeed * delta;

            // Determine facing
            if (Math.abs(jx) > Math.abs(jy)) {
                facing = jx > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                facing = jy > 0 ? Direction.UP : Direction.DOWN;
            }

            tryMove(dx, dy, world);

            state = sprint && stamina > 0 ? State.RUN : State.WALK;
        } else {
            velocity.set(0, 0);
            if (state == State.WALK || state == State.RUN) state = State.IDLE;
        }

        // Dodge trigger
        if (ctrl.justDodged() && state != State.ATTACK) {
            dodgeDirX = (hasInput ? jx : 0);
            dodgeDirY = (hasInput ? jy : (facing == Direction.UP ? -1 : 1));
            dodgeTimer = GameConstants.DODGE_WINDOW;
            state = State.DODGE;
            stateTime = 0;
        }
    }

    /** Move, then resolve tile collisions by sliding. */
    private void tryMove(float dx, float dy, World world) {
        // Try X
        x += dx;
        updateBounds();
        if (world.collidesWithMap(bounds)) { x -= dx; updateBounds(); }

        // Try Y
        y += dy;
        updateBounds();
        if (world.collidesWithMap(bounds)) { y -= dy; updateBounds(); }
    }

    // ── Combat ───────────────────────────────────────────────
    private void handleCombatInput(MobileControls ctrl) {
        if (ctrl.justAttacked() && attackCooldown <= 0 && state != State.IN_VEHICLE) {
            state        = State.ATTACK;
            stateTime    = 0;
            attackCooldown = GameConstants.ATTACK_COOLDOWN;
            game.combatSystem.playerAttack(this);
            game.audioManager.playSfx("attack_swing");
        }

        blocking = ctrl.isBlocking();
    }

    // ── Interaction ──────────────────────────────────────────
    private void handleInteractInput(MobileControls ctrl, World world) {
        if (!ctrl.justInteracted()) return;

        // Check NPC interaction first
        NPC nearby = game.npcManager.getNearbyNPC(
                centerX(), centerY(), GameConstants.INTERACTION_RADIUS);
        if (nearby != null) {
            game.npcManager.startDialogue(nearby, this);
            state = State.DIALOGUE;
            return;
        }

        // Vehicle mount/dismount
        Vehicle nearVehicle = game.vehicleManager.getNearbyVehicle(
                centerX(), centerY(), GameConstants.INTERACTION_RADIUS);
        if (nearVehicle != null && mountedVehicle == null) {
            mountVehicle(nearVehicle);
            return;
        }
        if (mountedVehicle != null) {
            dismountVehicle();
            return;
        }

        // World interaction (farm plot, treasure chest, shop door…)
        world.interact(this, game);
    }

    // ── Vehicle ──────────────────────────────────────────────
    private void mountVehicle(Vehicle v) {
        mountedVehicle = v;
        state = State.IN_VEHICLE;
        v.mount(this);
    }

    private void dismountVehicle() {
        if (mountedVehicle != null) {
            mountedVehicle.dismount();
            mountedVehicle = null;
        }
        state = State.IDLE;
    }

    private void updateVehicle(float delta, MobileControls ctrl, World world) {
        if (mountedVehicle == null) { state = State.IDLE; return; }
        mountedVehicle.driveUpdate(delta, ctrl, world);
        x = mountedVehicle.getX() + mountedVehicle.getWidth()  / 2f - width  / 2f;
        y = mountedVehicle.getY() + mountedVehicle.getHeight() / 2f - height / 2f;
        updateBounds();

        if (ctrl.justInteracted()) dismountVehicle();
    }

    // ── Stamina ──────────────────────────────────────────────
    private void regenStamina(float delta) {
        if (state == State.IDLE || state == State.WALK) {
            stamina = Math.min(GameConstants.PLAYER_STAMINA_MAX,
                               stamina + GameConstants.PLAYER_STAMINA_REGEN * delta);
        }
    }

    // ── Combat Receive ────────────────────────────────────────
    @Override
    public void takeDamage(int amount) {
        if (invincible > 0) return;
        if (blocking) amount = (int)(amount * (1f - GameConstants.BLOCK_DAMAGE_REDUCTION));
        super.takeDamage(amount);
        invincible = GameConstants.INVINCIBILITY_TIME;
        game.audioManager.playSfx("player_hurt");
        // Screen shake
        if (amount > 0) game.eventBus.post(new GameEventBus.Event(GameEventBus.EventType.PLAYER_HIT, amount));
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        state = State.IDLE;
        game.eventBus.post(new GameEventBus.Event(GameEventBus.EventType.PLAYER_DIED, 0));
    }

    // ── Dialogue exit ────────────────────────────────────────
    public void exitDialogue() {
        state = State.IDLE;
    }

    // ── Draw ─────────────────────────────────────────────────
    @Override
    public void draw(SpriteBatch batch, float delta) {
        if (!visible) return;
        stateTime += delta;

        // Production: use AnimationManager to get correct frame.
        // Placeholder: draw a coloured rectangle.
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Body colour changes with state
        switch (state) {
            case ATTACK: sr.setColor(1f, 0.3f, 0.1f, 1f); break;
            case RUN:    sr.setColor(0.3f, 0.8f, 0.3f, 1f); break;
            case DODGE:  sr.setColor(0.3f, 0.3f, 1.0f, 1f); break;
            default:     sr.setColor(0.6f, 0.85f, 0.5f, 1f);
        }
        sr.rect(x, y, width, height);

        // Shadow
        sr.setColor(0f, 0f, 0f, 0.3f);
        sr.ellipse(x + 2, y - 4, width - 4, 8);

        // Facing dot
        sr.setColor(1f, 1f, 0.3f, 1f);
        switch (facing) {
            case UP:    sr.rect(x + 8, y + height - 6, 8, 4); break;
            case DOWN:  sr.rect(x + 8, y + 2, 8, 4); break;
            case LEFT:  sr.rect(x + 2, y + 12, 4, 8); break;
            case RIGHT: sr.rect(x + width - 6, y + 12, 4, 8); break;
        }
        sr.end();

        batch.begin();

        // Health bar
        if (health < maxHealth) drawHealthBar(batch);
    }

    private void drawHealthBar(SpriteBatch batch) {
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        sr.rect(x, y + height + 4, width, 4);
        float ratio = (float) health / maxHealth;
        sr.setColor(ratio > 0.5f ? Color.GREEN : ratio > 0.25f ? Color.YELLOW : Color.RED);
        sr.rect(x, y + height + 4, width * ratio, 4);
        sr.end();
        batch.begin();
    }

    // ── Inventory ────────────────────────────────────────────
    public boolean addItem(Item item) {
        if (inventory.size() >= GameConstants.MAX_INVENTORY_SLOTS) return false;
        inventory.add(item);
        game.eventBus.post(new GameEventBus.Event(GameEventBus.EventType.ITEM_PICKED_UP, item));
        return true;
    }

    public boolean removeItem(Item item) {
        return inventory.remove(item);
    }

    public boolean hasItem(String itemId) {
        for (Item i : inventory) if (i.id.equals(itemId)) return true;
        return false;
    }

    public int countItem(String itemId) {
        int c = 0;
        for (Item i : inventory) if (i.id.equals(itemId)) c++;
        return c;
    }

    public boolean removeItems(String itemId, int amount) {
        int removed = 0;
        for (int i = inventory.size() - 1; i >= 0 && removed < amount; i--) {
            if (inventory.get(i).id.equals(itemId)) {
                inventory.remove(i);
                removed++;
            }
        }
        return removed == amount;
    }

    // ── Economy ──────────────────────────────────────────────
    public void addMoney(int amount) {
        money += amount;
        game.activeSave.playerData.money = money;
        game.eventBus.post(new GameEventBus.Event(GameEventBus.EventType.MONEY_CHANGED, amount));
    }

    public boolean spendMoney(int amount) {
        if (money < amount) return false;
        money -= amount;
        game.activeSave.playerData.money = money;
        game.eventBus.post(new GameEventBus.Event(GameEventBus.EventType.MONEY_CHANGED, -amount));
        return true;
    }

    public void changeReputation(int delta) {
        reputation = MathUtils.clamp(reputation + delta, -500, 500);
        game.activeSave.playerData.reputation = reputation;
    }

    // ── Getters ───────────────────────────────────────────────
    public State          getState()      { return state; }
    public int            getMoney()      { return money; }
    public int            getReputation() { return reputation; }
    public float          getStamina()    { return stamina; }
    public boolean        isBlocking()    { return blocking; }
    public int            getAttackDmg()  { return attackDamage; }
    public List<Item>     getInventory()  { return inventory; }
    public Vehicle        getVehicle()    { return mountedVehicle; }
    public boolean        isInVehicle()   { return mountedVehicle != null; }
}
