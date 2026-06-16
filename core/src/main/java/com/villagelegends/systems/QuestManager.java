package com.villagelegends.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.villagelegends.VillageLegends;
import com.villagelegends.data.GameSaveData;
import com.villagelegends.data.Quest;
import com.villagelegends.data.QuestObjective;
import com.villagelegends.entities.Player;
import com.villagelegends.world.World;

import java.util.*;

/**
 * QuestManager owns the full quest database (loaded from JSON) and
 * tracks live quest state in the active save.
 *
 * Quest flow:
 *   LOCKED → AVAILABLE → ACTIVE → COMPLETE | FAILED
 *
 * Main missions follow a linear story arc; side quests unlock based on
 * reputation or story flags.  The manager fires EventBus events so the
 * HUD and dialogue system react automatically.
 */
public class QuestManager {

    private final VillageLegends game;

    // ── Database (loaded once from JSON) ──────────────────────
    private final Map<String, Quest> questDB = new LinkedHashMap<>();

    // ── Live state ────────────────────────────────────────────
    private final Map<String, Quest.State> questStates = new HashMap<>();
    private final Map<String, Integer> objectiveProgress = new HashMap<>();
    private final Set<String>  gameFlags  = new HashSet<>();
    private final List<String> activeIds  = new ArrayList<>();

    // ─────────────────────────────────────────────────────────
    public QuestManager(VillageLegends game) {
        this.game = game;
        loadQuestDatabase();
        subscribeToEvents();
    }

    // ── Database loading ──────────────────────────────────────
    private void loadQuestDatabase() {
        // Register all 30 main missions + sample side quests
        registerMainMissions();
        registerSideQuests();
        Gdx.app.log("QuestManager", "Loaded " + questDB.size() + " quests");
    }

    private void registerMainMissions() {
        // Act 1 – Student Life Begins
        addQuest("m01_intro",       "Welcome to Peddapuram",
            "Arrive at the village and meet the village elder.",
            Quest.Type.MAIN, 0, 0,
            obj("talk", "elder_ramu", 1, "Talk to Elder Ramu"),
            obj("visit", "panchayat_office", 1, "Visit the Panchayat office")
        ).setReward(100, 5, "bicycle");

        addQuest("m02_first_harvest","First Harvest",
            "Help your father harvest the groundnut crop.",
            Quest.Type.MAIN, 100, 0,
            obj("harvest", "groundnut", 10, "Harvest 10 groundnut bags"),
            obj("sell", "market_stall", 1, "Sell at the village market")
        ).setReward(200, 10, null);

        addQuest("m03_school_day",  "School's In Session",
            "Complete your first week at the village school.",
            Quest.Type.MAIN, 200, 0,
            obj("attend", "school", 5, "Attend school for 5 days"),
            obj("talk", "teacher_lakshmi", 1, "Speak with Teacher Lakshmi")
        ).setReward(50, 8, null).setRequiredFlag("intro_done");

        addQuest("m04_strange_smoke","Strange Smoke",
            "Investigate the mysterious smoke in the forest at night.",
            Quest.Type.MAIN, 300, 0,
            obj("visit", "forest_north", 1, "Reach the northern forest"),
            obj("collect", "evidence_crate", 1, "Find the crate of evidence"),
            obj("escape", "forest", 1, "Escape before sunrise")
        ).setReward(150, 15, null).setRequiredFlag("school_complete");

        addQuest("m05_first_theft", "Night of Thefts",
            "Three farms were robbed overnight. Find clues.",
            Quest.Type.MAIN, 300, 1,
            obj("investigate", "crime_scene", 3, "Investigate 3 crime scenes"),
            obj("talk", "witness_parvati", 1, "Talk to witness Parvati"),
            obj("deliver", "report_panchayat", 1, "Report findings to Panchayat")
        ).setReward(300, 20, null);

        addQuest("m06_gang_spotted","Gang Spotted",
            "The Redstone Gang has been seen near the highway.",
            Quest.Type.MAIN, 400, 1,
            obj("tail", "gang_thug_01", 1, "Follow a gang member unseen"),
            obj("photograph", "hideout", 1, "Locate and photograph the hideout")
        ).setReward(250, 25, null);

        addQuest("m07_temple_robbery","Temple in Danger",
            "The gang is planning to steal the temple gold.",
            Quest.Type.MAIN, 500, 1,
            obj("defend", "temple", 1, "Defend the temple through the night"),
            obj("defeat", "gang_thug", 5, "Defeat 5 thugs"),
            obj("recover", "temple_idol", 1, "Recover the stolen idol")
        ).setReward(500, 30, "motorcycle").setRequiredFlag("gang_known");

        addQuest("m08_market_extortion","Market Extortion",
            "Merchants are being forced to pay the gang. Stop it.",
            Quest.Type.MAIN, 600, 2,
            obj("talk", "merchant_ravi", 3, "Talk to 3 threatened merchants"),
            obj("defeat", "extortion_thug", 4, "Beat the extortionists"),
            obj("collect", "extortion_money", 1, "Recover stolen payments")
        ).setReward(400, 30, null);

        addQuest("m09_corrupt_official","Dirty Hands",
            "The village panchayat officer is working with the gang.",
            Quest.Type.MAIN, 800, 2,
            obj("collect", "bribe_ledger", 1, "Steal the bribe ledger"),
            obj("photograph", "meeting_evidence", 1, "Photograph a secret meeting"),
            obj("deliver", "evidence_journalist", 1, "Give evidence to journalist Meena")
        ).setReward(600, 40, null).setRequiredFlag("first_arrest");

        addQuest("m10_lake_smugglers","Lake Smugglers",
            "Contraband is being moved across the lake at night.",
            Quest.Type.MAIN, 900, 2,
            obj("drive", "boat_patrol", 1, "Patrol the lake by boat"),
            obj("intercept", "smuggler_boat", 2, "Intercept 2 smuggler boats"),
            obj("deliver", "contraband_police", 1, "Deliver evidence to police")
        ).setReward(700, 40, "boat");

        // Act 2 – Rising Hero (m11–m20)
        addQuest("m11_forest_treasure","Forest Secrets",
            "A local elder speaks of buried smuggler treasure in the forest.",
            Quest.Type.MAIN, 1000, 3,
            obj("find", "treasure_map", 1, "Find the treasure map"),
            obj("explore", "forest_deep", 3, "Explore 3 deep forest locations"),
            obj("collect", "buried_treasure", 1, "Unearth the treasure")
        ).setReward(1000, 50, null);

        addQuest("m12_bus_chase",   "Highway Chase",
            "The gang is transporting kidnapped villagers on the highway.",
            Quest.Type.MAIN, 1100, 3,
            obj("drive", "motorcycle_chase", 1, "Chase the gang's bus"),
            obj("disable", "bus_engine", 1, "Disable the bus"),
            obj("rescue", "kidnapped_villager", 6, "Free all 6 villagers")
        ).setReward(800, 60, null);

        addQuest("m13_farming_sabotage","Poisoned Fields",
            "Someone is poisoning the water supply for the farms.",
            Quest.Type.MAIN, 1200, 3,
            obj("investigate", "water_source", 1, "Find the poison source"),
            obj("collect", "poison_sample", 2, "Collect water samples"),
            obj("stop", "saboteur", 1, "Confront the saboteur")
        ).setReward(600, 55, null);

        addQuest("m14_festival_protection","Festival Night",
            "Protect the Dasara festival from gang disruption.",
            Quest.Type.MAIN, 1400, 4,
            obj("patrol", "festival_ground", 1, "Patrol the festival area"),
            obj("defeat", "disguised_thug", 8, "Unmask and defeat 8 disguised thugs"),
            obj("protect", "festival_chariot", 1, "Keep the chariot safe")
        ).setReward(900, 70, null).setRequiredFlag("temple_saved");

        addQuest("m15_smuggler_chief","The Chief's Identity",
            "Discover who is the Smuggler Chief controlling the gang.",
            Quest.Type.MAIN, 1600, 4,
            obj("infiltrate", "gang_hideout", 1, "Infiltrate the gang hideout"),
            obj("photograph", "chief_face", 1, "Photograph the chief's face"),
            obj("escape", "hideout", 1, "Escape unseen")
        ).setReward(800, 80, null);

        addQuest("m16_witness_protection","Protect the Witness",
            "Journalist Meena has been threatened. Keep her safe for 3 days.",
            Quest.Type.MAIN, 1700, 4,
            obj("escort", "journalist_meena", 3, "Escort Meena on 3 trips"),
            obj("defeat", "assassin", 3, "Stop 3 assassination attempts")
        ).setReward(1000, 90, null);

        addQuest("m17_police_station","Truth at the Station",
            "Break into the police station to expose the corrupt inspector.",
            Quest.Type.MAIN, 1900, 5,
            obj("stealth", "police_station", 1, "Enter unseen"),
            obj("collect", "police_ledger", 1, "Steal the ledger"),
            obj("escape", "station", 1, "Escape without being caught")
        ).setReward(1200, 100, null);

        addQuest("m18_town_siege",  "Siege on Town Market",
            "The gang has taken the town market hostage.",
            Quest.Type.MAIN, 2100, 5,
            obj("defeat", "gang_lieutenant", 1, "Defeat Lieutenant Sharma"),
            obj("rescue", "hostage_merchant", 10, "Free all 10 hostages"),
            obj("secure", "market", 1, "Secure the market perimeter")
        ).setReward(1500, 110, null);

        addQuest("m19_expose_gang", "Front Page News",
            "Help journalist Meena broadcast the gang's crimes to the nation.",
            Quest.Type.MAIN, 2300, 5,
            obj("deliver", "all_evidence", 1, "Deliver all collected evidence"),
            obj("protect", "broadcast_tower", 1, "Guard the broadcast tower"),
            obj("survive", "gang_assault", 1, "Survive the gang's final assault")
        ).setReward(2000, 120, null).setRequiredFlag("chief_exposed");

        // Act 3 – Final Showdown (m20–m30)
        addQuest("m20_village_army","Rise of the Village",
            "Rally villagers to form a defence against the final gang push.",
            Quest.Type.MAIN, 2500, 6,
            obj("recruit", "ally_villager", 15, "Recruit 15 villager allies"),
            obj("train", "militia", 3, "Complete 3 training sessions")
        ).setReward(1500, 130, "tractor");

        addQuest("m21_highway_blockade","Blockade",
            "Block the gang's escape route on the national highway.",
            Quest.Type.MAIN, 2700, 6,
            obj("drive", "tractor_blockade", 1, "Position tractor blockade"),
            obj("defeat", "escape_convoy", 3, "Stop 3 convoy vehicles")
        ).setReward(1800, 140, null);

        addQuest("m22_forest_base", "Destroy the Base",
            "Assault and destroy the gang's forest base camp.",
            Quest.Type.MAIN, 2900, 6,
            obj("defeat", "base_guard", 12, "Defeat all 12 guards"),
            obj("destroy", "supply_depot", 1, "Blow up their supply depot"),
            obj("rescue", "captive_elder", 1, "Rescue captured Elder Ramu")
        ).setReward(2000, 150, null);

        addQuest("m23_flood_crisis","Flood Crisis",
            "Monsoon flooding threatens the village. Coordinate evacuation.",
            Quest.Type.MAIN, 3000, 6,
            obj("evacuate", "flood_villager", 20, "Evacuate 20 villagers"),
            obj("drive", "boat_rescue", 5, "Rescue 5 stranded by boat"),
            obj("repair", "dam_breach", 1, "Temporarily seal the dam breach")
        ).setReward(1200, 100, null);

        addQuest("m24_final_mole",  "Mole in the Panchayat",
            "One more traitor sits on the Panchayat council. Root them out.",
            Quest.Type.MAIN, 3200, 7,
            obj("investigate", "council_member", 4, "Investigate all 4 council members"),
            obj("collect", "proof_mole", 1, "Collect irrefutable proof"),
            obj("confront", "mole", 1, "Confront the traitor publicly")
        ).setReward(2500, 160, null);

        addQuest("m25_gang_army",   "War at the Gates",
            "The full gang army attacks the village at dawn.",
            Quest.Type.MAIN, 3500, 7,
            obj("defend", "village_gate", 3, "Hold all 3 village gates"),
            obj("defeat", "gang_soldier", 30, "Defeat 30 gang soldiers"),
            obj("survive", "wave", 5, "Survive 5 attack waves")
        ).setReward(3000, 200, null);

        addQuest("m26_boss_lieutenant","The Lieutenant Falls",
            "Face the gang's second-in-command, Vikram Reddy.",
            Quest.Type.MAIN, 3800, 7,
            obj("defeat", "boss_vikram", 1, "Defeat Vikram Reddy")
        ).setReward(2000, 150, null);

        addQuest("m27_lake_final",  "Last Run",
            "Intercept the chief's escape across the lake.",
            Quest.Type.MAIN, 4000, 7,
            obj("drive", "final_boat_chase", 1, "Chase the escape boat"),
            obj("disable", "enemy_boat", 1, "Disable the chief's vessel")
        ).setReward(2500, 180, null);

        addQuest("m28_final_boss",  "Face of Evil",
            "Confront the Smuggler Chief – Narayana Rao – at the town centre.",
            Quest.Type.MAIN, 4200, 8,
            obj("defeat", "boss_narayana", 1, "Defeat Narayana Rao"),
            obj("protect", "villagers_boss_fight", 1, "Keep bystanders safe")
        ).setReward(5000, 250, null);

        addQuest("m29_court_testimony","Justice Served",
            "Testify at the district court with all your evidence.",
            Quest.Type.MAIN, 4500, 8,
            obj("deliver", "full_evidence_court", 1, "Submit full evidence package"),
            obj("talk", "judge", 1, "Testify before the judge")
        ).setReward(3000, 200, null);

        addQuest("m30_village_hero","Village Legend",
            "Return to Peddapuram as its celebrated hero on Ugadi day.",
            Quest.Type.MAIN, 5000, 8,
            obj("attend", "celebration", 1, "Attend the village celebration"),
            obj("talk", "all_quest_givers", 8, "Speak with all 8 story characters")
        ).setReward(0, 500, null).setRequiredFlag("game_complete");
    }

    private void registerSideQuests() {
        // Farming side-quests
        addQuest("s01_rice_harvest","Golden Rice",
            "Grow and sell 50 bags of premium rice.",
            Quest.Type.SIDE, 0, 0,
            obj("harvest", "rice", 50, "Harvest 50 bags of rice"),
            obj("sell", "rice_market", 50, "Sell 50 bags at market")
        ).setReward(800, 20, null);

        addQuest("s02_irrigation","Water Work",
            "Build 3 irrigation channels for nearby farms.",
            Quest.Type.SIDE, 100, 0,
            obj("build", "irrigation_channel", 3, "Build 3 channels")
        ).setReward(400, 15, null);

        addQuest("s03_seeds","Heirloom Seeds",
            "Collect rare vegetable seeds for the village seed bank.",
            Quest.Type.SIDE, 200, 1,
            obj("collect", "heirloom_seed", 8, "Find 8 heirloom seed packets")
        ).setReward(300, 10, null);

        // Fishing side-quests
        addQuest("s04_big_fish",   "Legend of the Lake",
            "Catch the legendary catfish said to weigh 20kg.",
            Quest.Type.SIDE, 0, 1,
            obj("fish", "legendary_catfish", 1, "Catch the legendary catfish")
        ).setReward(600, 25, null);

        addQuest("s05_fish_supply","Village Fish Supply",
            "Provide 30 fish to the village before the festival.",
            Quest.Type.SIDE, 0, 0,
            obj("catch", "any_fish", 30, "Catch 30 fish")
        ).setReward(350, 15, null);

        // Racing side-quests
        addQuest("s06_tractor_race","Tractor Derby",
            "Win the annual Peddapuram tractor race.",
            Quest.Type.SIDE, 0, 1,
            obj("win", "tractor_race", 1, "Finish first in the tractor race")
        ).setReward(500, 30, null);

        addQuest("s07_bike_race",  "Two-Wheel King",
            "Win a motorcycle race against the town riders.",
            Quest.Type.SIDE, 0, 2,
            obj("win", "motorcycle_race", 1, "Win the motorcycle race")
        ).setReward(700, 35, null);

        // Mini-game quests
        addQuest("s08_kabaddi",    "Kabaddi Champion",
            "Win 5 kabaddi matches for the village team.",
            Quest.Type.SIDE, 0, 0,
            obj("win", "kabaddi_match", 5, "Win 5 kabaddi matches")
        ).setReward(300, 20, null);

        addQuest("s09_cricket",    "Village Cricket Cup",
            "Lead the village team to the district cricket final.",
            Quest.Type.SIDE, 0, 1,
            obj("win", "cricket_match", 4, "Win 4 cricket matches")
        ).setReward(500, 25, null);

        // Delivery quests
        addQuest("s10_milk_delivery","Morning Milk Run",
            "Deliver milk from 5 farms to the town dairy by 8 AM.",
            Quest.Type.SIDE, 0, 0,
            obj("deliver", "milk_can", 5, "Deliver 5 milk cans before 8 AM")
        ).setReward(200, 10, null);

        addQuest("s11_medicine_run","Emergency Medicine",
            "Deliver medicine to the sick elder before nightfall.",
            Quest.Type.SIDE, 100, 1,
            obj("collect", "medicine", 1, "Collect medicine from town doctor"),
            obj("deliver", "elder_house", 1, "Deliver medicine to elder's house")
        ).setReward(150, 15, null);

        // Treasure hunts
        addQuest("s12_old_map",    "Ancient Treasure",
            "Follow the old map and find the Zamindar's buried gold.",
            Quest.Type.SIDE, 0, 2,
            obj("find", "ancient_treasure_1", 1, "Find treasure location 1"),
            obj("find", "ancient_treasure_2", 1, "Find treasure location 2"),
            obj("dig",  "final_treasure", 1, "Unearth the Zamindar's treasure")
        ).setReward(2000, 50, null);

        addQuest("s13_ruins",      "Temple Ruins",
            "Explore the old temple ruins and recover artefacts.",
            Quest.Type.SIDE, 0, 2,
            obj("explore", "temple_ruin", 4, "Explore 4 ruin chambers"),
            obj("collect", "artefact", 6, "Collect 6 artefacts")
        ).setReward(900, 30, null);

        // Animal / wildlife
        addQuest("s14_wild_boar",  "Boar Problem",
            "A wild boar is destroying crops. Drive it away non-lethally.",
            Quest.Type.SIDE, 0, 0,
            obj("scare", "wild_boar", 1, "Drive away the wild boar")
        ).setReward(250, 12, null);

        addQuest("s15_leopard",    "Spotted Danger",
            "A leopard has been spotted near the forest school path.",
            Quest.Type.SIDE, 100, 1,
            obj("escort", "school_children", 1, "Escort children home safely"),
            obj("report", "forest_officer", 1, "Report sighting to forest officer")
        ).setReward(300, 20, null);

        // Festival quests (add more on each festival)
        addQuest("s16_sankranti",  "Sankranti Sweets",
            "Collect ingredients and make traditional Pongal sweets.",
            Quest.Type.FESTIVAL, 0, 0,
            obj("collect", "jaggery", 5, "Collect 5 jaggery blocks"),
            obj("collect", "rice", 10, "Collect 10 rice bags"),
            obj("craft", "pongal", 20, "Cook 20 bowls of Pongal")
        ).setReward(400, 25, null);

        addQuest("s17_kite_contest","Kite King",
            "Win the Sankranti kite-flying contest.",
            Quest.Type.FESTIVAL, 0, 0,
            obj("win", "kite_contest", 1, "Win the kite competition")
        ).setReward(200, 15, null);

        // Add more side quests up to 100...
        // For brevity, stub remaining quests
        for (int i = 18; i <= 100; i++) {
            String id = String.format("s%02d_stub", i);
            addQuest(id, "Side Quest " + i, "A side quest for the village.",
                Quest.Type.SIDE, i * 30, i / 10,
                obj("collect", "item_" + i, 1, "Collect required item")
            ).setReward(100 + i * 10, 5 + i, null);
        }
    }

    // ── Helper to register quests ─────────────────────────────
    private Quest addQuest(String id, String title, String description,
                           Quest.Type type, int moneyReq, int repTier,
                           QuestObjective... objectives) {
        Quest q = new Quest(id, title, description, type, moneyReq, repTier);
        for (QuestObjective o : objectives) q.addObjective(o);
        questDB.put(id, q);
        questStates.put(id, Quest.State.LOCKED);
        return q;
    }

    private QuestObjective obj(String action, String target, int count, String label) {
        return new QuestObjective(action, target, count, label);
    }

    // ── Event subscriptions ───────────────────────────────────
    private void subscribeToEvents() {
        game.eventBus.subscribe(GameEventBus.EventType.ITEM_PICKED_UP, e -> {
            checkObjectiveProgress("collect", e.stringData());
        });
        game.eventBus.subscribe(GameEventBus.EventType.NPC_DEFEATED, e -> {
            checkObjectiveProgress("defeat", e.stringData());
        });
        game.eventBus.subscribe(GameEventBus.EventType.CROP_HARVESTED, e -> {
            checkObjectiveProgress("harvest", e.stringData());
        });
        game.eventBus.subscribe(GameEventBus.EventType.NEW_DAY, e -> {
            unlockAvailableQuests();
        });
    }

    // ── Core quest logic ──────────────────────────────────────
    public void loadFromSave(GameSaveData save) {
        questStates.clear();
        questStates.putAll(save.questStates);
        objectiveProgress.clear();
        objectiveProgress.putAll(save.objectiveProgress);
        gameFlags.clear();
        gameFlags.addAll(save.gameFlags);
        rebuildActiveList();
        unlockAvailableQuests();
    }

    private void rebuildActiveList() {
        activeIds.clear();
        for (Map.Entry<String, Quest.State> e : questStates.entrySet()) {
            if (e.getValue() == Quest.State.ACTIVE) activeIds.add(e.getKey());
        }
    }

    public void tick(float delta, Player player, NPCManager npcManager, World world) {
        // Check proximity-based objectives (visit, escort, patrol)
        for (String qid : activeIds) {
            Quest q = questDB.get(qid);
            if (q == null) continue;
            for (QuestObjective obj : q.getObjectives()) {
                if (obj.isComplete()) continue;
                if ("visit".equals(obj.action) || "attend".equals(obj.action)) {
                    String region = world.getCurrentRegionId();
                    if (region.contains(obj.target)) {
                        advanceObjective(qid, obj.objectiveId, 1);
                    }
                }
            }
        }
    }

    public void triggerByFlag(String flag) {
        gameFlags.add(flag);
        unlockAvailableQuests();
        // Sync to save
        if (game.activeSave != null) {
            game.activeSave.gameFlags.add(flag);
        }
    }

    public boolean hasFlag(String flag) { return gameFlags.contains(flag); }

    private void unlockAvailableQuests() {
        Player p = null; // quests check save data, not live player ref
        int money = game.activeSave != null ? game.activeSave.playerData.money : 0;
        int rep   = game.activeSave != null ? game.activeSave.playerData.reputation : 0;

        for (Map.Entry<String, Quest> entry : questDB.entrySet()) {
            String id = entry.getKey();
            Quest  q  = entry.getValue();
            if (questStates.get(id) != Quest.State.LOCKED) continue;

            boolean reqMet = money >= q.moneyRequirement
                          && rep   >= q.reputationTier * 50
                          && (q.requiredFlag == null || gameFlags.contains(q.requiredFlag));
            if (reqMet) {
                questStates.put(id, Quest.State.AVAILABLE);
                game.eventBus.post(new GameEventBus.Event(
                        GameEventBus.EventType.SHOW_NOTIFICATION, "New quest: " + q.title));
            }
        }
    }

    public boolean startQuest(String questId) {
        Quest.State s = questStates.get(questId);
        if (s != Quest.State.AVAILABLE) return false;
        questStates.put(questId, Quest.State.ACTIVE);
        activeIds.add(questId);
        syncStatesToSave();
        game.eventBus.post(GameEventBus.EventType.QUEST_STARTED, questId);
        return true;
    }

    private void checkObjectiveProgress(String action, String target) {
        for (String qid : activeIds) {
            Quest q = questDB.get(qid);
            if (q == null) continue;
            for (QuestObjective obj : q.getObjectives()) {
                if (!obj.isComplete() && action.equals(obj.action)
                        && target.contains(obj.target)) {
                    advanceObjective(qid, obj.objectiveId, 1);
                }
            }
        }
    }

    public void advanceObjective(String questId, String objectiveId, int amount) {
        String key = questId + "." + objectiveId;
        int prev   = objectiveProgress.getOrDefault(key, 0);
        Quest q    = questDB.get(questId);
        if (q == null) return;
        QuestObjective obj = q.getObjective(objectiveId);
        if (obj == null || obj.isComplete()) return;
        int now = Math.min(prev + amount, obj.requiredCount);
        objectiveProgress.put(key, now);
        obj.setProgress(now);
        syncStatesToSave();
        game.eventBus.post(GameEventBus.EventType.QUEST_OBJECTIVE_COMPLETE, objectiveId);
        if (q.allObjectivesComplete()) completeQuest(questId);
    }

    private void completeQuest(String questId) {
        questStates.put(questId, Quest.State.COMPLETE);
        activeIds.remove(questId);
        Quest q = questDB.get(questId);
        // Grant rewards
        if (q.rewardMoney > 0) game.activeSave.playerData.money += q.rewardMoney;
        if (q.rewardRep   > 0) game.activeSave.playerData.reputation += q.rewardRep;
        if (q.rewardItem  != null) {
            // game.economyManager.grantItem(q.rewardItem);
        }
        // Set quest completion flag
        gameFlags.add(questId + "_done");
        if (questId.equals("m30_village_hero")) {
            gameFlags.add(com.villagelegends.GameConstants.FLAG_GAME_COMPLETE);
        }
        syncStatesToSave();
        game.eventBus.post(GameEventBus.EventType.QUEST_COMPLETE, questId);
        Gdx.app.log("QuestManager", "Quest complete: " + q.title);
        unlockAvailableQuests();
    }

    private void syncStatesToSave() {
        if (game.activeSave == null) return;
        game.activeSave.questStates.clear();
        game.activeSave.questStates.putAll(questStates);
        game.activeSave.objectiveProgress.clear();
        game.activeSave.objectiveProgress.putAll(objectiveProgress);
        game.activeSave.gameFlags.clear();
        game.activeSave.gameFlags.addAll(gameFlags);
    }

    // ── Getters ───────────────────────────────────────────────
    public Quest            getQuest(String id)    { return questDB.get(id); }
    public Quest.State      getState(String id)    { return questStates.getOrDefault(id, Quest.State.LOCKED); }
    public List<String>     getActiveQuestIds()    { return Collections.unmodifiableList(activeIds); }
    public Collection<Quest>getAllQuests()          { return questDB.values(); }
    public int              getTotalQuests()        { return questDB.size(); }
    public int              getCompletedCount() {
        int c = 0;
        for (Quest.State s : questStates.values()) if (s == Quest.State.COMPLETE) c++;
        return c;
    }
}
