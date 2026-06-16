package com.villagelegends.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Base for every game entity (Player, NPC, Vehicle, PickUp, etc.).
 *
 * Holds position, velocity, collision box, health and a common
 * animation state.  Subclasses override update() and draw().
 */
public abstract class Entity {

    // ── Position / Movement ──────────────────────────────────
    protected float x, y;
    protected float width, height;
    protected final Vector2  velocity   = new Vector2();
    protected final Rectangle bounds    = new Rectangle();   // collision box

    // ── Health ───────────────────────────────────────────────
    protected int   health;
    protected int   maxHealth;
    protected boolean alive = true;

    // ── Visual ───────────────────────────────────────────────
    protected TextureRegion currentFrame;
    protected float         stateTime  = 0f;
    protected Direction     facing     = Direction.DOWN;

    // ── Flags ────────────────────────────────────────────────
    protected boolean visible = true;
    protected boolean solid   = true;   // does it block movement?

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    // ─────────────────────────────────────────────────────────
    public Entity(float x, float y, float w, float h) {
        this.x      = x;
        this.y      = y;
        this.width  = w;
        this.height = h;
        updateBounds();
    }

    /** Called once per game tick. */
    public abstract void update(float delta);

    /** Draw the entity's sprite. */
    public abstract void draw(SpriteBatch batch, float delta);

    // ── Collision helpers ─────────────────────────────────────
    protected void updateBounds() {
        // Collision box slightly narrower / taller-offset for top-down look
        float bw = width  * 0.6f;
        float bh = height * 0.35f;
        bounds.set(x + (width - bw) / 2f, y, bw, bh);
    }

    public boolean overlaps(Entity other) {
        return bounds.overlaps(other.bounds);
    }

    public boolean overlaps(Rectangle r) {
        return bounds.overlaps(r);
    }

    public float distanceTo(Entity other) {
        float dx = (other.x + other.width  / 2f) - (x + width  / 2f);
        float dy = (other.y + other.height / 2f) - (y + height / 2f);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float centerX() { return x + width  / 2f; }
    public float centerY() { return y + height / 2f; }

    // ── Health ───────────────────────────────────────────────
    public void takeDamage(int amount) {
        if (!alive) return;
        health = Math.max(0, health - amount);
        if (health == 0) onDeath();
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    protected void onDeath() {
        alive = false;
    }

    // ── Getters / Setters ─────────────────────────────────────
    public float     getX()        { return x; }
    public float     getY()        { return y; }
    public float     getWidth()    { return width; }
    public float     getHeight()   { return height; }
    public Rectangle getBounds()   { return bounds; }
    public boolean   isAlive()     { return alive; }
    public int       getHealth()   { return health; }
    public int       getMaxHealth(){ return maxHealth; }
    public Direction getFacing()   { return facing; }
    public boolean   isVisible()   { return visible; }
    public boolean   isSolid()     { return solid; }

    public void setPosition(float nx, float ny) {
        this.x = nx;  this.y = ny;
        updateBounds();
    }

    public void setHealth(int health, int maxHealth) {
        this.health    = health;
        this.maxHealth = maxHealth;
    }
}
