package com.villagelegends.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.villagelegends.VillageLegends;
import com.villagelegends.entities.Player;
import com.villagelegends.entities.Vehicle;
import com.villagelegends.world.World;

import java.util.*;

/**
 * VehicleManager owns all Vehicle instances in the current region.
 *
 * Responsibilities:
 *  - Spawn ambient & player-owned vehicles per region
 *  - Update driving physics each tick
 *  - Locate nearest vehicle for mount queries
 *  - Remove destroyed vehicles and notify Economy
 */
public class VehicleManager {

    private final VillageLegends game;
    private final List<Vehicle>  active   = new ArrayList<>();
    private final List<Vehicle>  playerOwned = new ArrayList<>();

    public VehicleManager(VillageLegends game) {
        this.game = game;
    }

    // ── Region spawn ─────────────────────────────────────────
    public void spawnVehiclesForRegion(String regionId) {
        active.clear();
        switch (regionId) {
            case "main_village":
                spawnVehicle(Vehicle.VehicleType.BICYCLE,     320, 180);
                spawnVehicle(Vehicle.VehicleType.BULLOCK_CART,500, 200);
                spawnVehicle(Vehicle.VehicleType.TRACTOR,     600, 300);
                break;
            case "highway":
                spawnVehicle(Vehicle.VehicleType.BUS,         400, 100);
                spawnVehicle(Vehicle.VehicleType.MOTORCYCLE,  600, 100);
                spawnVehicle(Vehicle.VehicleType.MOTORCYCLE,  700, 140);
                break;
            case "lake":
                spawnVehicle(Vehicle.VehicleType.BOAT,        300, 250);
                spawnVehicle(Vehicle.VehicleType.BOAT,        450, 280);
                break;
            case "town":
                spawnVehicle(Vehicle.VehicleType.BICYCLE,     280, 200);
                spawnVehicle(Vehicle.VehicleType.MOTORCYCLE,  360, 200);
                break;
            case "farmlands":
                spawnVehicle(Vehicle.VehicleType.TRACTOR,     200, 200);
                spawnVehicle(Vehicle.VehicleType.BULLOCK_CART,350, 250);
                break;
            default:
                break;
        }
        // Re-add player-owned vehicles in this region
        active.addAll(playerOwned);
    }

    private Vehicle spawnVehicle(Vehicle.VehicleType type, float x, float y) {
        Vehicle v = new Vehicle(game, type, x, y);
        active.add(v);
        return v;
    }

    /** Spawns a newly purchased vehicle for the player */
    public void spawnPlayerVehicle(String typeId, float x, float y) {
        Vehicle.VehicleType type = Vehicle.VehicleType.valueOf(typeId.toUpperCase());
        Vehicle v = spawnVehicle(type, x, y);
        playerOwned.add(v);
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, com.villagelegends.world.World world) {
        // AI-driven bus follows highway path
        for (Vehicle v : active) {
            if (!v.isOccupied() && v.getType() == Vehicle.VehicleType.BUS) {
                driveBusAI(v, delta, world);
            }
        }
        // Remove destroyed
        active.removeIf(v -> !v.isAlive());
    }

    private void driveBusAI(Vehicle bus, float delta, com.villagelegends.world.World world) {
        // Simple back-and-forth bus route on highway
        float targetX = (bus.getX() > 600) ? 200 : 800;
        float dx = targetX - bus.getX();
        float speed = com.villagelegends.GameConstants.BUS_SPEED * 0.6f;
        float move  = (dx > 0 ? 1 : -1) * speed * delta;
        bus.setPosition(bus.getX() + move, bus.getY());
    }

    // ── Draw ─────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        for (Vehicle v : active) {
            if (v.isVisible()) v.draw(batch, delta);
        }
    }

    // ── Proximity lookup ──────────────────────────────────────
    public Vehicle getNearbyVehicle(float px, float py, float radius) {
        for (Vehicle v : active) {
            if (v.isOccupied()) continue;
            float dx = v.centerX() - px;
            float dy = v.centerY() - py;
            if (dx * dx + dy * dy <= radius * radius) return v;
        }
        return null;
    }

    // ── Getters ───────────────────────────────────────────────
    public List<Vehicle> getActiveVehicles() {
        return Collections.unmodifiableList(active);
    }
}
