package com.villagelegends.data;

import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────────────────────
//  Quest
// ─────────────────────────────────────────────────────────────
public class Quest {

    public enum Type  { MAIN, SIDE, FESTIVAL, RACE, DELIVERY }
    public enum State { LOCKED, AVAILABLE, ACTIVE, COMPLETE, FAILED }

    // ── Identity ──────────────────────────────────────────────
    public final String id;
    public final String title;
    public final String description;
    public final Type   questType;

    // ── Requirements ─────────────────────────────────────────
    public final int  moneyRequirement;
    public final int  reputationTier;    // 0=any, 1=20rep, 2=50rep …
    public       String requiredFlag;   // game-flag that must be set first

    // ── Rewards ───────────────────────────────────────────────
    public int    rewardMoney = 0;
    public int    rewardRep   = 0;
    public String rewardItem  = null;

    // ── Objectives ────────────────────────────────────────────
    private final List<QuestObjective> objectives = new ArrayList<>();

    // ─────────────────────────────────────────────────────────
    public Quest(String id, String title, String description,
                 Type type, int moneyReq, int repTier) {
        this.id                = id;
        this.title             = title;
        this.description       = description;
        this.questType         = type;
        this.moneyRequirement  = moneyReq;
        this.reputationTier    = repTier;
    }

    public Quest setReward(int money, int rep, String itemId) {
        this.rewardMoney = money;
        this.rewardRep   = rep;
        this.rewardItem  = itemId;
        return this;
    }

    public Quest setRequiredFlag(String flag) {
        this.requiredFlag = flag;
        return this;
    }

    public Quest addObjective(QuestObjective obj) {
        objectives.add(obj);
        return this;
    }

    public boolean allObjectivesComplete() {
        for (QuestObjective o : objectives) if (!o.isComplete()) return false;
        return !objectives.isEmpty();
    }

    public QuestObjective getObjective(String objId) {
        for (QuestObjective o : objectives) if (o.objectiveId.equals(objId)) return o;
        return null;
    }

    public List<QuestObjective> getObjectives() { return objectives; }
    public int getProgress() {
        int done = 0;
        for (QuestObjective o : objectives) if (o.isComplete()) done++;
        return done;
    }
    public int getTotalObjectives() { return objectives.size(); }
}
