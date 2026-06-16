package com.villagelegends.data;

import com.villagelegends.data.Quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root serialisable save object written by SaveManager to LibGDX Preferences.
 *
 * All sub-objects must have no-arg constructors for LibGDX Json to work.
 * Keep field types simple: primitives, String, List, Map, Set, or
 * other serialisable classes.
 */
public class GameSaveData {

    // ── Slot metadata ─────────────────────────────────────────
    public int    slot                  = 0;
    public String saveName              = "Save 1";
    public long   lastSaveTimestamp     = 0L;
    public float  totalPlayTime         = 0f;     // real seconds

    // ── World time ────────────────────────────────────────────
    public float  gameHour              = 6f;     // 0..24
    public int    currentDay            = 1;

    // ── Player ────────────────────────────────────────────────
    public PlayerData playerData        = new PlayerData();

    // ── Quests ────────────────────────────────────────────────
    /** questId → State name (LOCKED/AVAILABLE/ACTIVE/COMPLETE/FAILED) */
    public Map<String, Quest.State>  questStates       = new HashMap<>();
    /** "questId.objectiveId" → progress count */
    public Map<String, Integer>      objectiveProgress = new HashMap<>();
    /** Story flags set by triggers and quest completions */
    public Set<String>               gameFlags         = new HashSet<>();

    // ── NPCs ──────────────────────────────────────────────────
    /** IDs of hostile NPCs permanently knocked out */
    public Set<String>               defeatedNPCIds    = new HashSet<>();

    // ── Farming ───────────────────────────────────────────────
    public List<FarmPlot>            farmPlots         = new ArrayList<>();

    // ── Festival ─────────────────────────────────────────────
    public String activeFestival        = "NONE";
    public float  festivalTimer         = 0f;

    // ── Settings (stored per-save so each profile can differ) ─
    public float  musicVolume           = 0.7f;
    public float  sfxVolume             = 1.0f;
    public String language              = "en";

    // ── Completed mini-games high-scores ──────────────────────
    public Map<String, Integer>      miniGameScores    = new HashMap<>();

    /** No-arg constructor required by LibGDX Json */
    public GameSaveData() {}
}
