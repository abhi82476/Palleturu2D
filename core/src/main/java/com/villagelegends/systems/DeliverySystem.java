package com.villagelegends.systems;

import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.Item;
import com.villagelegends.entities.Player;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DeliverySystem manages timed delivery contracts.
 *
 * A DeliveryJob requires the player to:
 *   1. Collect a specific item from a pickup location
 *   2. Carry it to a destination (different region or location)
 *   3. Arrive before the time limit expires
 *
 * Delivery jobs are generated dynamically by NPCs or fixed by side quests.
 * Completing them earns money and reputation.
 *
 * Active deliveries show on HUD as a countdown timer + destination arrow.
 */
public class DeliverySystem {

    // ── Job definition ────────────────────────────────────────
    public static class DeliveryJob {
        public final String  jobId;
        public final String  itemId;
        public final String  itemDisplayName;
        public final String  pickupRegion;
        public final float   pickupX, pickupY;
        public final String  destRegion;
        public final float   destX, destY;
        public final float   timeLimitSeconds;   // real seconds
        public final int     reward;
        public final int     reputationReward;
        public final String  linkedQuestId;      // null if standalone

        public float   elapsed        = 0f;
        public boolean itemCollected  = false;
        public boolean completed      = false;
        public boolean failed         = false;

        public DeliveryJob(String id, String itemId, String itemName,
                           String pickupR, float px, float py,
                           String destR, float dx, float dy,
                           float timeLimit, int reward, int rep, String questId) {
            this.jobId           = id;
            this.itemId          = itemId;
            this.itemDisplayName = itemName;
            this.pickupRegion    = pickupR;
            this.pickupX         = px;
            this.pickupY         = py;
            this.destRegion      = destR;
            this.destX           = dx;
            this.destY           = dy;
            this.timeLimitSeconds= timeLimit;
            this.reward          = reward;
            this.reputationReward= rep;
            this.linkedQuestId   = questId;
        }

        public float getTimeRemainingSeconds() {
            return Math.max(0, timeLimitSeconds - elapsed);
        }

        public float getProgressFraction() {
            return Math.min(1f, elapsed / timeLimitSeconds);
        }

        public boolean isExpired() {
            return elapsed >= timeLimitSeconds;
        }

        public String getTimerText() {
            float rem = getTimeRemainingSeconds();
            int min = (int) (rem / 60);
            int sec = (int) (rem % 60);
            return String.format("%d:%02d", min, sec);
        }
    }

    // ─────────────────────────────────────────────────────────
    private final VillageLegends game;
    private final List<DeliveryJob> activeJobs = new ArrayList<>();
    private final List<DeliveryJob> pendingJobs = new ArrayList<>();  // offered but not started

    public DeliverySystem(VillageLegends game) {
        this.game = game;
        buildStandardDeliveries();
    }

    // ── Standard deliveries (always available from NPCs) ──────
    private void buildStandardDeliveries() {
        float T = GameConstants.TILE_SIZE;

        // Milk run: farm to town dairy (s10)
        pendingJobs.add(new DeliveryJob(
            "milk_run_01", "milk_can", "Milk Can",
            "farmlands",  8*T,  8*T,
            "town",      80*T, 30*T,
            480f,   // 8 real minutes
            200, 10, "s10_milk_delivery"
        ));

        // Medicine delivery (s11)
        pendingJobs.add(new DeliveryJob(
            "medicine_01", "medicine", "Medicine",
            "town",         50*T, 50*T,
            "main_village", 20*T, 60*T,
            300f,   // 5 minutes
            150, 15, "s11_medicine_run"
        ));

        // Fish supply to market
        pendingJobs.add(new DeliveryJob(
            "fish_market_01", "rohu_fish", "Fresh Fish",
            "lake",         20*T, 48*T,
            "main_village", 80*T, 40*T,
            360f,   // 6 minutes
            180, 8, null
        ));

        // Temple donation transport
        pendingJobs.add(new DeliveryJob(
            "temple_donation_01", "jaggery", "Jaggery for Temple",
            "main_village", 80*T, 40*T,
            "main_village", 10*T, 10*T,
            180f,   // 3 minutes
            100, 12, null
        ));

        // School supplies
        pendingJobs.add(new DeliveryJob(
            "school_supplies_01", "misc_notebook", "School Notebooks",
            "town",         70*T, 20*T,
            "main_village", 25*T, 90*T,
            420f,   // 7 minutes
            130, 10, null
        ));
    }

    // ── Accept / start a delivery job ─────────────────────────
    public boolean acceptJob(String jobId, Player player) {
        DeliveryJob job = getPendingJob(jobId);
        if (job == null) return false;
        if (activeJobs.size() >= 3) {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Max 3 active deliveries!"));
            return false;
        }
        pendingJobs.remove(job);
        activeJobs.add(job);
        game.eventBus.post(new GameEventBus.Event(
                GameEventBus.EventType.SHOW_NOTIFICATION,
                "Delivery started: Go to " + job.pickupRegion));
        return true;
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, Player player, String currentRegion) {
        List<DeliveryJob> toRemove = new ArrayList<>();

        for (DeliveryJob job : activeJobs) {
            if (job.completed || job.failed) { toRemove.add(job); continue; }

            job.elapsed += delta;

            // Check expiry
            if (job.isExpired()) {
                job.failed = true;
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION,
                        "Delivery FAILED: " + job.itemDisplayName));
                player.changeReputation(-5);
                toRemove.add(job);
                continue;
            }

            // Check pickup: player in pickup region and has item
            if (!job.itemCollected && currentRegion.equals(job.pickupRegion)) {
                float px = player.centerX(), py = player.centerY();
                float dx = px - job.pickupX, dy = py - job.pickupY;
                if (dx * dx + dy * dy < 60 * 60) {
                    if (player.hasItem(job.itemId) || attemptAutoGrant(job, player)) {
                        job.itemCollected = true;
                        game.eventBus.post(new GameEventBus.Event(
                                GameEventBus.EventType.SHOW_NOTIFICATION,
                                "Item collected! Take to " + job.destRegion));
                    }
                }
            }

            // Check delivery: player at destination with item
            if (job.itemCollected && currentRegion.equals(job.destRegion)) {
                float px = player.centerX(), py = player.centerY();
                float dx = px - job.destX, dy = py - job.destY;
                if (dx * dx + dy * dy < 60 * 60 && player.hasItem(job.itemId)) {
                    completeJob(job, player);
                    toRemove.add(job);
                }
            }
        }

        activeJobs.removeAll(toRemove);
    }

    private boolean attemptAutoGrant(DeliveryJob job, Player player) {
        // If item not in inventory, add it if available (simulates pickup)
        Item item = new Item(job.itemId, job.itemDisplayName,
                             Item.Category.MISC, 0);
        return player.addItem(item);
    }

    private void completeJob(DeliveryJob job, Player player) {
        job.completed = true;
        player.removeItems(job.itemId, 1);
        player.addMoney(job.reward);
        player.changeReputation(job.reputationReward);

        // Bonus for fast delivery
        float pct = 1f - job.getProgressFraction();
        if (pct > 0.5f) {
            int bonus = (int)(job.reward * 0.25f);
            player.addMoney(bonus);
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Delivered! ₹" + job.reward + " + ₹" + bonus + " speed bonus!"));
        } else {
            game.eventBus.post(new GameEventBus.Event(
                    GameEventBus.EventType.SHOW_NOTIFICATION,
                    "Delivered! Earned ₹" + job.reward));
        }

        game.audioManager.playSfx("coin_jingle");

        // Quest advance
        if (job.linkedQuestId != null) {
            game.questManager.advanceObjective(job.linkedQuestId, "deliver_" + job.itemId, 1);
        }

        // Refresh pending job list (add a new similar job after completion)
        refreshPendingJob(job);
    }

    private void refreshPendingJob(DeliveryJob completed) {
        // Re-add a slightly varied version after completion
        float reward  = completed.reward * (0.9f + MathUtils.random() * 0.2f);
        pendingJobs.add(new DeliveryJob(
                completed.jobId + "_refresh",
                completed.itemId, completed.itemDisplayName,
                completed.pickupRegion, completed.pickupX, completed.pickupY,
                completed.destRegion,   completed.destX,   completed.destY,
                completed.timeLimitSeconds,
                (int) reward, completed.reputationReward,
                null
        ));
    }

    // ── Query helpers ─────────────────────────────────────────
    public DeliveryJob getPendingJob(String jobId) {
        for (DeliveryJob j : pendingJobs) if (j.jobId.equals(jobId)) return j;
        return null;
    }

    public DeliveryJob getActiveJob(String jobId) {
        for (DeliveryJob j : activeJobs) if (j.jobId.equals(jobId)) return j;
        return null;
    }

    public List<DeliveryJob> getActiveJobs()   { return activeJobs; }
    public List<DeliveryJob> getPendingJobs()  { return pendingJobs; }
    public boolean hasActiveJobs()             { return !activeJobs.isEmpty(); }

    /** Get the most urgent active delivery (lowest time remaining). */
    public DeliveryJob getMostUrgent() {
        DeliveryJob urgent = null;
        float min = Float.MAX_VALUE;
        for (DeliveryJob j : activeJobs) {
            if (j.getTimeRemainingSeconds() < min) {
                min = j.getTimeRemainingSeconds();
                urgent = j;
            }
        }
        return urgent;
    }
}
