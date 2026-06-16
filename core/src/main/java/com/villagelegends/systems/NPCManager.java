package com.villagelegends.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.data.Quest;
import com.villagelegends.entities.NPC;
import com.villagelegends.entities.Player;
import com.villagelegends.world.World;

import java.util.*;

/**
 * NPCManager owns all active NPC instances for the current region.
 *
 * It handles:
 *  - Spawning the correct NPCs per region (loaded from a region config)
 *  - Updating their AI and schedules each tick
 *  - Starting and driving dialogue sessions
 *  - Saving and restoring NPC states (defeated flags, quest-giver status)
 */
public class NPCManager {

    private final VillageLegends game;
    private final List<NPC> activeNPCs = new ArrayList<>();

    // ── Dialogue session ─────────────────────────────────────
    private NPC          dialogueNPC    = null;
    private Player       dialoguePlayer = null;
    private String       currentNode    = null;

    // ── Defeated hostile NPCs (persist across sessions) ───────
    private final Set<String> defeatedIds = new HashSet<>();

    // ─────────────────────────────────────────────────────────
    public NPCManager(VillageLegends game) {
        this.game = game;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.DIALOGUE_END, e -> {
            if (dialoguePlayer != null) dialoguePlayer.exitDialogue();
            dialogueNPC    = null;
            dialoguePlayer = null;
            currentNode    = null;
        });
    }

    // ── Region load ───────────────────────────────────────────
    public void spawnNPCsForRegion(World world, String regionId) {
        activeNPCs.clear();

        switch (regionId) {
            case "main_village":   spawnMainVillage();   break;
            case "farmlands":      spawnFarmlands();     break;
            case "forest":         spawnForest();        break;
            case "lake":           spawnLake();          break;
            case "town":           spawnTown();          break;
            case "highway":        spawnHighway();       break;
            case "panchayat":      spawnPanchayat();     break;
            default:               break;
        }

        // Remove already-defeated hostile NPCs
        activeNPCs.removeIf(npc -> npc.isHostile() && defeatedIds.contains(npc.npcId));
    }

    private void spawnMainVillage() {
        // Elder and story characters
        NPC elder = new NPC(game, "elder_ramu", "Elder Ramu", NPC.Type.ELDER, 400, 320);
        elder.setDialogueId("dlg_elder_ramu");
        elder.setQuestGiver("m01_intro");
        activeNPCs.add(elder);

        NPC teacher = new NPC(game, "teacher_lakshmi", "Lakshmi Madam", NPC.Type.TEACHER, 560, 480);
        teacher.setDialogueId("dlg_teacher_lakshmi");
        teacher.setQuestGiver("m03_school_day");
        activeNPCs.add(teacher);

        NPC priest = new NPC(game, "priest_subramaniam", "Priest Subramaniam", NPC.Type.PRIEST, 300, 200);
        priest.setDialogueId("dlg_priest");
        activeNPCs.add(priest);

        // Villagers – ambient NPCs
        for (int i = 0; i < 8; i++) {
            NPC v = new NPC(game, "villager_" + i, "Villager", NPC.Type.VILLAGER,
                    200 + i * 60, 150 + (i % 3) * 80);
            v.setDialogueId("dlg_generic_villager");
            activeNPCs.add(v);
        }

        // Gang patrol – 2 thugs
        NPC thug1 = new NPC(game, "gang_thug_01", "Gang Member", NPC.Type.GANG_THUG, 700, 250);
        thug1.setHostile(false);   // not hostile until m06 quest flag
        activeNPCs.add(thug1);

        NPC thug2 = new NPC(game, "gang_thug_02", "Gang Member", NPC.Type.GANG_THUG, 750, 260);
        thug2.setHostile(game.questManager.hasFlag("gang_known"));
        activeNPCs.add(thug2);
    }

    private void spawnFarmlands() {
        for (int i = 0; i < 6; i++) {
            NPC farmer = new NPC(game, "farmer_" + i, "Farmer", NPC.Type.FARMER,
                    100 + i * 80, 100 + (i % 2) * 120);
            farmer.setDialogueId("dlg_generic_farmer");
            activeNPCs.add(farmer);
        }
        NPC witness = new NPC(game, "witness_parvati", "Parvati", NPC.Type.VILLAGER, 350, 200);
        witness.setDialogueId("dlg_parvati");
        witness.setQuestGiver("m05_first_theft");
        activeNPCs.add(witness);
    }

    private void spawnForest() {
        // Smugglers in forest
        for (int i = 0; i < 5; i++) {
            NPC smug = new NPC(game, "smuggler_f" + i, "Smuggler", NPC.Type.SMUGGLER,
                    200 + i * 90, 300 + (i % 3) * 70);
            smug.setHostile(true);
            activeNPCs.add(smug);
        }
        NPC forester = new NPC(game, "forest_officer", "Forest Officer", NPC.Type.GUARD, 150, 150);
        forester.setDialogueId("dlg_forest_officer");
        activeNPCs.add(forester);
    }

    private void spawnLake() {
        for (int i = 0; i < 4; i++) {
            NPC fisherman = new NPC(game, "fisherman_" + i, "Fisherman", NPC.Type.FISHERMAN,
                    150 + i * 100, 100);
            fisherman.setDialogueId("dlg_fisherman");
            activeNPCs.add(fisherman);
        }
        // Smuggler boats spawn dynamically at night (managed by FestivalManager/EventManager)
    }

    private void spawnTown() {
        // Journalist Meena
        NPC meena = new NPC(game, "journalist_meena", "Journalist Meena", NPC.Type.VILLAGER, 400, 300);
        meena.setDialogueId("dlg_meena");
        meena.setQuestGiver("m09_corrupt_official");
        activeNPCs.add(meena);

        // Town merchants
        for (int i = 0; i < 5; i++) {
            NPC m = new NPC(game, "merchant_town_" + i, "Merchant", NPC.Type.MERCHANT,
                    250 + i * 70, 200 + (i % 2) * 60);
            m.setDialogueId("dlg_shop_" + i);
            activeNPCs.add(m);
        }

        // Corrupt official
        NPC official = new NPC(game, "official_corrupt", "Officer Krishnamurthy",
                NPC.Type.OFFICIAL, 600, 400);
        official.setDialogueId("dlg_official");
        official.setHostile(game.questManager.hasFlag("chief_exposed"));
        activeNPCs.add(official);

        // Gang lieutenant (boss, appears after m15)
        if (game.questManager.hasFlag("gang_known") && !defeatedIds.contains("boss_vikram")) {
            NPC vikram = new NPC(game, "boss_vikram", "Vikram Reddy", NPC.Type.GANG_THUG, 700, 350);
            vikram.setHostile(true);
            vikram.setHealth(250, 250);
            activeNPCs.add(vikram);
        }
    }

    private void spawnHighway() {
        NPC busDriver = new NPC(game, "bus_driver_01", "Bus Driver", NPC.Type.BUS_DRIVER, 300, 100);
        busDriver.setDialogueId("dlg_bus_driver");
        activeNPCs.add(busDriver);

        for (int i = 0; i < 3; i++) {
            NPC thug = new NPC(game, "highway_thug_" + i, "Roadblock Thug", NPC.Type.GANG_THUG,
                    400 + i * 80, 200);
            thug.setHostile(true);
            activeNPCs.add(thug);
        }
    }

    private void spawnPanchayat() {
        NPC headman = new NPC(game, "headman_venkat", "Headman Venkat", NPC.Type.OFFICIAL, 350, 300);
        headman.setDialogueId("dlg_headman");
        headman.setQuestGiver("m05_first_theft");
        activeNPCs.add(headman);

        for (int i = 0; i < 4; i++) {
            NPC council = new NPC(game, "council_" + i, "Council Member " + (i + 1),
                    NPC.Type.OFFICIAL, 250 + i * 80, 200);
            council.setDialogueId("dlg_council_" + i);
            activeNPCs.add(council);
        }
    }

    // ── Update ────────────────────────────────────────────────
    public void update(float delta, float gameHour, Player player, World world) {
        for (NPC npc : activeNPCs) {
            npc.updateAI(delta, gameHour, player);
        }
        // Remove dead hostile NPCs and record them as defeated
        activeNPCs.removeIf(npc -> {
            if (!npc.isAlive() && npc.isHostile()) {
                defeatedIds.add(npc.npcId);
                game.eventBus.post(GameEventBus.EventType.NPC_DEFEATED, npc.npcId);
                player.changeReputation(5);
                return true;
            }
            return false;
        });
    }

    // ── Draw ─────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        for (NPC npc : activeNPCs) {
            npc.draw(batch, delta);
        }
    }

    // ── Proximity queries ─────────────────────────────────────
    public NPC getNearbyNPC(float x, float y, float radius) {
        for (NPC npc : activeNPCs) {
            float dx = npc.centerX() - x;
            float dy = npc.centerY() - y;
            if (dx * dx + dy * dy <= radius * radius) return npc;
        }
        return null;
    }

    // ── Dialogue ──────────────────────────────────────────────
    public void startDialogue(NPC npc, Player player) {
        dialogueNPC    = npc;
        dialoguePlayer = player;
        currentNode    = npc.getDialogueId();
        game.eventBus.post(GameEventBus.EventType.DIALOGUE_START, npc.npcId);
        // Quest auto-offer
        if (npc.isQuestGiver()) {
            String qid = npc.getQuestId();
            if (game.questManager.getState(qid) == com.villagelegends.data.Quest.State.AVAILABLE) {
                game.questManager.startQuest(qid);
            }
        }
    }

    // ── Save / Load ───────────────────────────────────────────
    public void loadFromSave(GameSaveData save) {
        defeatedIds.clear();
        defeatedIds.addAll(save.defeatedNPCIds);
    }

    public void syncToSave(GameSaveData save) {
        save.defeatedNPCIds.clear();
        save.defeatedNPCIds.addAll(defeatedIds);
    }

    // ── Getters ───────────────────────────────────────────────
    public List<NPC> getActiveNPCs() { return Collections.unmodifiableList(activeNPCs); }
    public NPC       getDialogueNPC(){ return dialogueNPC; }
    public boolean   isInDialogue()  { return dialogueNPC != null; }
}
