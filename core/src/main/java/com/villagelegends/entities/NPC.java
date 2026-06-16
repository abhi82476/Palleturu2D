package com.villagelegends.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;

import java.util.List;
import java.util.ArrayList;

/**
 * NPC – Non-Player Character.
 *
 * Behaviour is driven by a 24-hour schedule.  Each time-slot specifies
 * an Activity (IDLE, WALK_TO, WORK, SHOP, TEMPLE, HOME) and a target
 * tile position.  A* pathfinding moves the NPC toward the target.
 *
 * NPCs also carry dialogue trees and quest-trigger flags.
 */
public class NPC extends Entity {

    // ── Activity enum ─────────────────────────────────────────
    public enum Activity {
        IDLE, WALK_TO, WORK, SHOP_STALL, TEMPLE_PRAYER,
        SCHOOL, FARMING, FISHING, FESTIVAL, HOME
    }

    // ── NPC types ────────────────────────────────────────────
    public enum Type {
        FARMER, MERCHANT, STUDENT, ELDER, GUARD,
        GANG_THUG, SMUGGLER, OFFICIAL, FISHERMAN,
        PRIEST, TEACHER, BUS_DRIVER, VILLAGER
    }

    // ── Identity ─────────────────────────────────────────────
    public final String  npcId;
    public final String  name;
    public final Type    type;
    public       String  dialogueId;     // root dialogue node key
    private      boolean questGiver;
    private      String  questId;

    // ── Schedule ─────────────────────────────────────────────
    private final List<ScheduleEntry> schedule = new ArrayList<>();
    private Activity  currentActivity = Activity.IDLE;
    private Vector2   currentTarget   = new Vector2();
    private boolean   atTarget        = true;

    // ── Path ─────────────────────────────────────────────────
    private final List<Vector2> path = new ArrayList<>();
    private int pathIndex = 0;

    // ── Combat (for hostile NPCs) ────────────────────────────
    private float  attackCooldown = 0;
    private boolean hostile       = false;
    private boolean alerted       = false;
    private float   alertRadius;

    // ── State for reputation tinting ─────────────────────────
    private boolean friendly = true;

    private final VillageLegends game;

    // ─────────────────────────────────────────────────────────
    public NPC(VillageLegends game, String id, String name, Type type,
               float x, float y) {
        super(x, y, 20f, 28f);
        this.game   = game;
        this.npcId  = id;
        this.name   = name;
        this.type   = type;
        this.health = this.maxHealth = 80;

        alertRadius = (type == Type.GANG_THUG || type == Type.SMUGGLER)
                ? GameConstants.NPC_DETECT_RADIUS : 0;
        hostile = (type == Type.GANG_THUG || type == Type.SMUGGLER);
        buildDefaultSchedule();
    }

    // ── Schedule building ────────────────────────────────────
    private void buildDefaultSchedule() {
        // Default schedule by NPC type
        switch (type) {
            case FARMER:
                addSchedule(5,  6,  Activity.WALK_TO, 100, 80);  // go to field
                addSchedule(6,  12, Activity.FARMING, 100, 80);
                addSchedule(12, 13, Activity.IDLE,    90, 90);   // lunch
                addSchedule(13, 17, Activity.FARMING, 100, 80);
                addSchedule(17, 19, Activity.SHOP_STALL, 80, 120);
                addSchedule(19, 21, Activity.TEMPLE_PRAYER, 60, 60);
                addSchedule(21, 5,  Activity.HOME,    50, 50);
                break;
            case MERCHANT:
                addSchedule(7,  8,  Activity.WALK_TO, 80, 120);
                addSchedule(8,  20, Activity.SHOP_STALL, 80, 120);
                addSchedule(20, 22, Activity.TEMPLE_PRAYER, 60, 60);
                addSchedule(22, 7,  Activity.HOME,    50, 50);
                break;
            case STUDENT:
                addSchedule(6,  7,  Activity.WALK_TO, 110, 90);
                addSchedule(7,  14, Activity.SCHOOL, 110, 90);
                addSchedule(14, 16, Activity.WALK_TO, 80, 130);
                addSchedule(16, 20, Activity.IDLE, 80, 130);
                addSchedule(20, 6,  Activity.HOME, 50, 60);
                break;
            case GANG_THUG:
                addSchedule(0,  8,  Activity.HOME, x/32, y/32);
                addSchedule(8,  22, Activity.WALK_TO, x/32 + 5, y/32);
                addSchedule(22, 0,  Activity.IDLE, x/32, y/32);
                break;
            default:
                addSchedule(0, 24, Activity.IDLE, x/32, y/32);
        }
    }

    private void addSchedule(int startHour, int endHour, Activity activity, float tx, float ty) {
        schedule.add(new ScheduleEntry(startHour, endHour, activity, tx * 32, ty * 32));
    }

    // ─────────────────────────────────────────────────────────
    public void updateSchedule(float gameHour) {
        int hour = (int) gameHour % 24;
        for (ScheduleEntry entry : schedule) {
            boolean active;
            if (entry.startHour <= entry.endHour) {
                active = hour >= entry.startHour && hour < entry.endHour;
            } else {
                // Wraps midnight
                active = hour >= entry.startHour || hour < entry.endHour;
            }
            if (active && currentActivity != entry.activity) {
                currentActivity = entry.activity;
                currentTarget.set(entry.targetX, entry.targetY);
                atTarget = false;
                buildPath();
            }
        }
    }

    @Override
    public void update(float delta) {
        stateTime   += delta;
        attackCooldown = Math.max(0, attackCooldown - delta);

        if (!atTarget && !path.isEmpty()) {
            walkAlongPath(delta);
        }

        // Simple idle bob animation
        if (currentActivity == Activity.IDLE) {
            // handled visually in draw()
        }
    }

    public void updateAI(float delta, float gameHour, Player player) {
        updateSchedule(gameHour);
        update(delta);

        if (hostile && alerted) {
            chasePlayer(delta, player);
        } else if (hostile && alertRadius > 0) {
            float dist = distanceTo(player);
            if (dist <= alertRadius && !player.isInVehicle()) {
                alerted = true;
            }
        }
    }

    // ── Pathfinding (simplified A*) ───────────────────────────
    private void buildPath() {
        path.clear();
        pathIndex = 0;
        // In production: use proper A* grid pathfinder.
        // Simplified: direct line with waypoints.
        float stepSize = 32f;
        float dx = currentTarget.x - x;
        float dy = currentTarget.y - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = (int)(dist / stepSize) + 1;
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            path.add(new Vector2(x + dx * t, y + dy * t));
        }
    }

    private void walkAlongPath(float delta) {
        if (pathIndex >= path.size()) { atTarget = true; return; }

        Vector2 wp = path.get(pathIndex);
        float dx = wp.x - x;
        float dy = wp.y - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 4f) {
            pathIndex++;
            return;
        }

        float speed = GameConstants.NPC_WALK_SPEED;
        float nx = dx / dist * speed * delta;
        float ny = dy / dist * speed * delta;
        x += nx;  y += ny;
        updateBounds();

        facing = Math.abs(dx) > Math.abs(dy)
                ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                : (dy > 0 ? Direction.UP   : Direction.DOWN);
    }

    private void chasePlayer(float delta, Player player) {
        float dx = player.centerX() - centerX();
        float dy = player.centerY() - centerY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < GameConstants.ATTACK_RANGE) {
            if (attackCooldown <= 0) {
                game.combatSystem.npcAttackPlayer(this, player);
                attackCooldown = 1.2f;
            }
        } else {
            x += (dx / dist) * GameConstants.NPC_WALK_SPEED * 1.1f * delta;
            y += (dy / dist) * GameConstants.NPC_WALK_SPEED * 1.1f * delta;
            updateBounds();
        }
    }

    // ── Draw ─────────────────────────────────────────────────
    @Override
    public void draw(SpriteBatch batch, float delta) {
        if (!visible) return;

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Body colour by type
        Color body = getTypeColor();
        if (!alive) body = Color.DARK_GRAY;
        sr.setColor(body);
        sr.rect(x, y, width, height);

        // Shadow
        sr.setColor(0f, 0f, 0f, 0.25f);
        sr.ellipse(x + 2, y - 3, width - 4, 7);

        // Name indicator dot (quest giver = yellow star)
        if (questGiver && alive) {
            sr.setColor(Color.YELLOW);
            sr.circle(x + width / 2f, y + height + 8, 5);
        }
        sr.end();
        batch.begin();
    }

    private Color getTypeColor() {
        switch (type) {
            case FARMER:     return new Color(0.5f, 0.7f, 0.3f, 1f);
            case MERCHANT:   return new Color(0.8f, 0.6f, 0.2f, 1f);
            case STUDENT:    return new Color(0.4f, 0.6f, 0.9f, 1f);
            case ELDER:      return new Color(0.7f, 0.7f, 0.7f, 1f);
            case GANG_THUG:  return new Color(0.7f, 0.1f, 0.1f, 1f);
            case SMUGGLER:   return new Color(0.4f, 0.1f, 0.5f, 1f);
            case OFFICIAL:   return new Color(0.2f, 0.4f, 0.8f, 1f);
            case GUARD:      return new Color(0.2f, 0.6f, 0.2f, 1f);
            case PRIEST:     return new Color(0.9f, 0.8f, 0.6f, 1f);
            case FISHERMAN:  return new Color(0.3f, 0.5f, 0.7f, 1f);
            default:         return new Color(0.7f, 0.65f, 0.6f, 1f);
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public String      getName()          { return name; }
    public String      getDialogueId()    { return dialogueId; }
    public boolean     isQuestGiver()     { return questGiver; }
    public String      getQuestId()       { return questId; }
    public boolean     isHostile()        { return hostile; }
    public Activity    getActivity()      { return currentActivity; }

    public void setQuestGiver(String qId) {
        this.questGiver = true;
        this.questId    = qId;
    }
    public void setDialogueId(String id)  { this.dialogueId = id; }
    public void setHostile(boolean h)     { this.hostile = h; alerted = h; }

    // ── Inner class ───────────────────────────────────────────
    private static class ScheduleEntry {
        int startHour, endHour;
        Activity activity;
        float targetX, targetY;
        ScheduleEntry(int s, int e, Activity a, float tx, float ty) {
            startHour = s; endHour = e; activity = a;
            targetX = tx; targetY = ty;
        }
    }
}
