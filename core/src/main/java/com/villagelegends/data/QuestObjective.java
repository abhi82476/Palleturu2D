package com.villagelegends.data;

/**
 * A single measurable objective inside a Quest.
 * action: "collect" | "defeat" | "talk" | "visit" | "harvest" | "sell" |
 *         "deliver" | "escort" | "win" | "fish" | "build" | "photograph" |
 *         "infiltrate" | "escape" | "patrol" | "recruit" | "repair" | "protect"
 */
public class QuestObjective {

    public String objectiveId;
    public String action;
    public String target;
    public int    requiredCount;
    public String label;
    public int    progress = 0;

    /** No-arg constructor required for LibGDX Json deserialisation */
    public QuestObjective() {}

    public QuestObjective(String action, String target, int count, String label) {
        this.objectiveId   = action + "_" + target;
        this.action        = action;
        this.target        = target;
        this.requiredCount = count;
        this.label         = label;
    }

    public void    setProgress(int p)  { progress = Math.min(p, requiredCount); }
    public int     getProgress()       { return progress; }
    public boolean isComplete()        { return progress >= requiredCount; }
    public String  getStatusText()     {
        return label + " (" + progress + "/" + requiredCount + ")";
    }
}
