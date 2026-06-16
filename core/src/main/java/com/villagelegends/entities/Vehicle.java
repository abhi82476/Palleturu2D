package com.villagelegends.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.ui.MobileControls;
import com.villagelegends.world.World;

/**
 * Vehicle entity used for all drivable objects:
 * Bicycle, Motorcycle, Tractor, Bullock Cart, Boat, Bus.
 *
 * Physics model: top-down with angular momentum.
 * Fuel depletes per tile travelled; runs out → stops.
 */
public class Vehicle extends Entity {

    public enum VehicleType {
        BICYCLE, MOTORCYCLE, TRACTOR, BULLOCK_CART, BOAT, BUS
    }

    // ── Config ───────────────────────────────────────────────
    private final VehicleType vtype;
    private final float  maxSpeed;
    private final float  acceleration;
    private final float  turnSpeed;    // degrees/s
    private final float  fuelBurnRate; // per tile

    // ── State ────────────────────────────────────────────────
    private float   speed       = 0;
    private float   angle       = 0;    // degrees, 0=right
    private float   fuel;
    private boolean occupied    = false;
    private Player  driver      = null;
    private boolean engineOn    = false;

    // ── Upgrades ─────────────────────────────────────────────
    private int speedUpgrade    = 0;    // 0-3
    private int capacityUpgrade = 0;
    private int armorUpgrade    = 0;

    // ── Cargo ────────────────────────────────────────────────
    private int cargoSlots;

    private final VillageLegends game;

    // ─────────────────────────────────────────────────────────
    public Vehicle(VillageLegends game, VehicleType type, float x, float y) {
        super(x, y, getWidthFor(type), getHeightFor(type));
        this.game         = game;
        this.vtype        = type;
        this.fuel         = GameConstants.VEHICLE_FUEL_MAX;
        this.maxSpeed     = getMaxSpeedFor(type);
        this.acceleration = getAccelFor(type);
        this.turnSpeed    = getTurnSpeedFor(type);
        this.fuelBurnRate = getFuelBurnFor(type);
        this.cargoSlots   = getCargoFor(type);
        this.health = this.maxHealth = getHealthFor(type);
    }

    // ── Driving ──────────────────────────────────────────────
    public void driveUpdate(float delta, MobileControls ctrl, World world) {
        if (!engineOn || fuel <= 0) {
            speed = MathUtils.lerp(speed, 0, delta * 3f);
            if (speed < 0.5f) speed = 0;
            return;
        }

        float jx = ctrl.getJoystickX();
        float jy = ctrl.getJoystickY();
        boolean accel = ctrl.isAccelerating();
        boolean brake = ctrl.isBraking();

        // Steering
        if (jx != 0 && speed > 5f) {
            angle -= jx * turnSpeed * delta * (speed / maxSpeed);
        }

        float targetSpeed = 0;
        if (accel)       targetSpeed = maxSpeed * (1 + speedUpgrade * 0.15f);
        else if (brake)  targetSpeed = -maxSpeed * 0.3f;

        speed = MathUtils.lerp(speed, targetSpeed, acceleration * delta);

        float rad = angle * MathUtils.degreesToRadians;
        float dx = MathUtils.cos(rad) * speed * delta;
        float dy = MathUtils.sin(rad) * speed * delta;

        // Try move with collision
        float prevX = x, prevY = y;
        x += dx; updateBounds();
        if (world.collidesWithMap(bounds)) { x = prevX; speed *= -0.3f; }
        y += dy; updateBounds();
        if (world.collidesWithMap(bounds)) { y = prevY; speed *= -0.3f; }

        // Burn fuel
        float travelDist = (float) Math.sqrt(dx * dx + dy * dy) / GameConstants.TILE_SIZE;
        fuel = Math.max(0, fuel - travelDist * fuelBurnRate);
        if (fuel == 0) game.audioManager.playSfx("engine_off");

        // Facing angle for sprite rotation
        if (speed > 1f) {
            facing = angleToDirection(angle);
        }
    }

    @Override
    public void update(float delta) { /* full update via driveUpdate */ }

    // ── Mount / Dismount ─────────────────────────────────────
    public void mount(Player player) {
        occupied    = true;
        driver      = player;
        engineOn    = true;
        game.audioManager.playSfx("engine_start");
    }

    public void dismount() {
        occupied    = false;
        driver      = null;
        engineOn    = false;
    }

    public void refuel(float amount) {
        fuel = Math.min(GameConstants.VEHICLE_FUEL_MAX, fuel + amount);
    }

    // ── Draw ─────────────────────────────────────────────────
    @Override
    public void draw(SpriteBatch batch, float delta) {
        if (!visible) return;
        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Vehicle body
        sr.setColor(getVehicleColor());
        sr.rect(x, y, width, height);

        // Direction indicator
        sr.setColor(Color.WHITE);
        float cx = x + width / 2f, cy = y + height / 2f;
        float rad = angle * MathUtils.degreesToRadians;
        sr.rect(cx + MathUtils.cos(rad) * 6 - 3, cy + MathUtils.sin(rad) * 6 - 3, 6, 6);

        // Fuel bar (above vehicle)
        if (occupied) {
            float fw = width * (fuel / GameConstants.VEHICLE_FUEL_MAX);
            sr.setColor(0.15f, 0.15f, 0.15f, 0.8f);
            sr.rect(x, y + height + 4, width, 4);
            sr.setColor(fuel > 30 ? Color.ORANGE : Color.RED);
            sr.rect(x, y + height + 4, fw, 4);
        }

        sr.end();
        batch.begin();
    }

    private Color getVehicleColor() {
        switch (vtype) {
            case BICYCLE:     return new Color(0.2f, 0.5f, 0.9f, 1f);
            case MOTORCYCLE:  return new Color(0.7f, 0.1f, 0.1f, 1f);
            case TRACTOR:     return new Color(0.8f, 0.5f, 0.1f, 1f);
            case BULLOCK_CART:return new Color(0.6f, 0.4f, 0.2f, 1f);
            case BOAT:        return new Color(0.2f, 0.6f, 0.8f, 1f);
            case BUS:         return new Color(0.1f, 0.7f, 0.2f, 1f);
            default:          return Color.GRAY;
        }
    }

    private Direction angleToDirection(float angleDeg) {
        float a = ((angleDeg % 360) + 360) % 360;
        if      (a < 45 || a >= 315) return Direction.RIGHT;
        else if (a < 135)            return Direction.UP;
        else if (a < 225)            return Direction.LEFT;
        else                         return Direction.DOWN;
    }

    // ── Static config helpers ─────────────────────────────────
    private static float getMaxSpeedFor(VehicleType t) {
        switch (t) {
            case BICYCLE:      return GameConstants.BICYCLE_SPEED;
            case MOTORCYCLE:   return GameConstants.MOTORCYCLE_SPEED;
            case TRACTOR:      return GameConstants.TRACTOR_SPEED;
            case BULLOCK_CART: return GameConstants.BULLOCK_CART_SPEED;
            case BOAT:         return GameConstants.BOAT_SPEED;
            case BUS:          return GameConstants.BUS_SPEED;
            default: return 120;
        }
    }
    private static float getAccelFor(VehicleType t) {
        switch (t) {
            case MOTORCYCLE: return 4f;
            case BICYCLE:    return 2.5f;
            case BUS:        return 1.5f;
            default:         return 2f;
        }
    }
    private static float getTurnSpeedFor(VehicleType t) {
        switch (t) {
            case BICYCLE:    return 180f;
            case MOTORCYCLE: return 150f;
            case BOAT:       return 80f;
            case TRACTOR:    return 60f;
            case BUS:        return 55f;
            default:         return 100f;
        }
    }
    private static float getFuelBurnFor(VehicleType t) {
        switch (t) {
            case MOTORCYCLE: return 0.8f;
            case BUS:        return 1.2f;
            case BICYCLE:
            case BULLOCK_CART: return 0f;   // no fuel needed
            default:         return GameConstants.VEHICLE_FUEL_BURN;
        }
    }
    private static float getWidthFor(VehicleType t) {
        switch (t) {
            case BUS:    return 48f;
            case TRACTOR:return 40f;
            default:     return 28f;
        }
    }
    private static float getHeightFor(VehicleType t) {
        switch (t) {
            case BUS:    return 80f;
            case TRACTOR:return 52f;
            default:     return 44f;
        }
    }
    private static int getCargoFor(VehicleType t) {
        switch (t) {
            case TRACTOR:     return 20;
            case BULLOCK_CART:return 15;
            case BUS:         return 30;
            default:          return 4;
        }
    }
    private static int getHealthFor(VehicleType t) {
        switch (t) {
            case BUS:    return 200;
            case TRACTOR:return 160;
            default:     return 100;
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public VehicleType getType()      { return vtype; }
    public float       getFuel()      { return fuel; }
    public float       getFuelPct()   { return fuel / GameConstants.VEHICLE_FUEL_MAX; }
    public float       getSpeed()     { return speed; }
    public boolean     isOccupied()   { return occupied; }
    public boolean     isEngineOn()   { return engineOn; }
    public int         getSpeedUpgrade()   { return speedUpgrade; }
    public void        upgradeSpeed()      { speedUpgrade   = Math.min(3, speedUpgrade   + 1); }
    public void        upgradeCapacity()   { capacityUpgrade = Math.min(3, capacityUpgrade + 1); }
    public void        upgradeArmor()      { armorUpgrade   = Math.min(3, armorUpgrade   + 1); }
}
