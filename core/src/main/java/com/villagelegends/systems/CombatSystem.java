package com.villagelegends.systems;

import com.badlogic.gdx.math.MathUtils;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.entities.Entity;
import com.villagelegends.entities.NPC;
import com.villagelegends.entities.Player;
import com.villagelegends.entities.Vehicle;

import java.util.List;

/**
 * CombatSystem resolves all combat interactions for the non-lethal
 * fighting model in Village Legends 2D.
 *
 * "Non-lethal" means enemies are knocked out (stunned/incapacitated)
 * rather than killed; they recover after a cooldown period.
 *
 * Systems managed:
 *  - Player attack sweeps vs nearby NPCs
 *  - NPC attack vs player (damage, knockback)
 *  - Stealth takedowns (from behind, within reach)
 *  - Hit-flash visual feedback via EventBus
 *  - Boss fight phases with special mechanics
 *  - Screen-shake on heavy hits
 */
public class CombatSystem {

    private final VillageLegends game;

    // ── Screen shake state ────────────────────────────────────
    private float shakeTimer     = 0f;
    private float shakeIntensity = 0f;

    // ─────────────────────────────────────────────────────────
    public CombatSystem(VillageLegends game) {
        this.game = game;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.SCREEN_SHAKE, e -> {
            shakeIntensity = e.intData() * 0.5f;
            shakeTimer     = 0.35f;
        });
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, Player player, List<NPC> npcs, List<Vehicle> vehicles) {
        shakeTimer = Math.max(0, shakeTimer - delta);
    }

    // ── Player attacks ────────────────────────────────────────
    /**
     * Called when the player presses the attack button.
     * Sweeps a cone in front of the player; hits all NPCs within range.
     */
    public void playerAttack(Player player) {
        float px = player.centerX();
        float py = player.centerY();
        Entity.Direction dir = player.getFacing();

        // Build an attack hitbox in front of the player
        float ax = px, ay = py;
        float aw = GameConstants.ATTACK_RANGE, ah = GameConstants.ATTACK_RANGE;
        switch (dir) {
            case UP:    ay = py;                              break;
            case DOWN:  ay = py - GameConstants.ATTACK_RANGE; break;
            case LEFT:  ax = px - GameConstants.ATTACK_RANGE; break;
            case RIGHT: ax = px;                              break;
        }
        com.badlogic.gdx.math.Rectangle hitbox =
                new com.badlogic.gdx.math.Rectangle(ax - aw / 2, ay - ah / 2, aw, ah);

        List<NPC> npcs = game.npcManager.getActiveNPCs();
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getBounds().overlaps(hitbox)) {
                int damage = calculateAttackDamage(player, npc);
                applyDamage(npc, damage, player);
            }
        }
        // Screen shake on hit
        game.eventBus.post(GameEventBus.EventType.SCREEN_SHAKE, 4);
    }

    private int calculateAttackDamage(Player attacker, NPC target) {
        int base = attacker.getAttackDmg();
        // Stealth bonus: 3× damage from behind
        if (isFromBehind(attacker, target)) {
            base *= 3;
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, "Stealth takedown!"));
        }
        // Slight random variance ±20%
        float variance = 0.8f + MathUtils.random(0.4f);
        return Math.max(1, (int)(base * variance));
    }

    private boolean isFromBehind(Player attacker, NPC target) {
        // Attacker is "behind" if they approach from the opposite of the NPC's facing
        Entity.Direction tFace = target.getFacing();
        Entity.Direction aDir  = attacker.getFacing();
        return (tFace == Entity.Direction.UP    && aDir == Entity.Direction.UP)    ||
               (tFace == Entity.Direction.DOWN  && aDir == Entity.Direction.DOWN) ||
               (tFace == Entity.Direction.LEFT  && aDir == Entity.Direction.LEFT) ||
               (tFace == Entity.Direction.RIGHT && aDir == Entity.Direction.RIGHT);
    }

    private void applyDamage(NPC npc, int damage, Player attacker) {
        npc.takeDamage(damage);
        // Knockback
        float kbX = attacker.centerX() - npc.centerX();
        float kbY = attacker.centerY() - npc.centerY();
        float dist = (float) Math.sqrt(kbX * kbX + kbY * kbY);
        if (dist > 0) {
            float kbForce = 40f;
            npc.setPosition(npc.getX() - (kbX / dist) * kbForce,
                            npc.getY() - (kbY / dist) * kbForce);
        }
        game.audioManager.playSfx(npc.isAlive() ? "hit_flesh" : "knockout");
        game.eventBus.post(GameEventBus.EventType.SCREEN_SHAKE, damage > 15 ? 6 : 3);
    }

    // ── NPC attacks player ────────────────────────────────────
    public void npcAttackPlayer(NPC npc, Player player) {
        if (!npc.isAlive() || !player.isAlive()) return;
        float dx = player.centerX() - npc.centerX();
        float dy = player.centerY() - npc.centerY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > GameConstants.ATTACK_RANGE * 1.5f) return;

        // Base NPC damage by type
        int dmg = getNPCAttackDamage(npc);
        player.takeDamage(dmg);

        // Stagger player (brief slowdown) – handled by Player's invincibility frames
        game.audioManager.playSfx("player_hurt");
    }

    private int getNPCAttackDamage(NPC npc) {
        switch (npc.type) {
            case GANG_THUG:  return 12 + MathUtils.random(6);
            case SMUGGLER:   return 15 + MathUtils.random(8);
            case OFFICIAL:   return 8  + MathUtils.random(4);
            default:         return 5  + MathUtils.random(3);
        }
    }

    // ── Boss fight helpers ────────────────────────────────────
    /**
     * Boss fight phases for Vikram Reddy (m26) and Narayana Rao (m28).
     * Phase transitions happen at 66% and 33% HP.
     */
    public void updateBossPhase(NPC boss, Player player) {
        float hpPct = (float) boss.getHealth() / boss.getMaxHealth();

        if (hpPct <= 0.33f) {
            // Phase 3: rage mode – double attack speed, recruit 2 thugs
            boss.setHostile(true);
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, boss.getName() + " is enraged!"));
        } else if (hpPct <= 0.66f) {
            // Phase 2: calls for backup
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION, boss.getName() + " calls backup!"));
        }
    }

    // ── Screen shake accessor ──────────────────────────────────
    public float getShakeOffset() {
        if (shakeTimer <= 0) return 0;
        return (float)(Math.sin(shakeTimer * 60) * shakeIntensity * shakeTimer * 2f);
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean isShaking() { return shakeTimer > 0; }
}
