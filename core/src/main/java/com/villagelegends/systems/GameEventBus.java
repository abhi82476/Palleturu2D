package com.villagelegends.systems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight synchronous event bus.
 *
 * Managers subscribe to specific EventTypes during init.
 * Any system can post an event; all listeners are called immediately
 * (same frame, same thread — safe for LibGDX's single GL thread).
 *
 * Usage:
 *   game.eventBus.subscribe(EventType.QUEST_COMPLETE, e -> showRewardUI(e));
 *   game.eventBus.post(new Event(EventType.QUEST_COMPLETE, questId));
 */
public class GameEventBus {

    // ─── All game event types ────────────────────────────────
    public enum EventType {
        // Player
        PLAYER_HIT, PLAYER_DIED, PLAYER_RESPAWN,
        MONEY_CHANGED, REPUTATION_CHANGED,
        ITEM_PICKED_UP, ITEM_USED, ITEM_DROPPED,
        STAMINA_LOW,

        // Quest
        QUEST_STARTED, QUEST_OBJECTIVE_COMPLETE,
        QUEST_COMPLETE, QUEST_FAILED,

        // World
        REGION_CHANGED, TIME_CHANGED, NEW_DAY,
        WEATHER_CHANGED,

        // NPC
        DIALOGUE_START, DIALOGUE_END,
        NPC_ALERTED, NPC_DEFEATED,

        // Vehicle
        VEHICLE_MOUNTED, VEHICLE_DISMOUNTED,
        VEHICLE_FUEL_LOW, VEHICLE_DESTROYED,

        // Farming
        CROP_PLANTED, CROP_WATERED, CROP_HARVESTED,
        CROP_DIED, PEST_ATTACK,

        // Combat
        COMBAT_START, COMBAT_END, BOSS_DEFEATED,

        // Economy
        PURCHASE_MADE, SALE_MADE, TRADE_COMPLETE,

        // Festival
        FESTIVAL_START, FESTIVAL_END,
        MINIGAME_START, MINIGAME_END,

        // UI
        OPEN_SHOP, OPEN_INVENTORY, OPEN_QUEST_LOG,
        SHOW_NOTIFICATION, SCREEN_SHAKE,

        // Save
        GAME_SAVED, GAME_LOADED
    }

    // ─── Event object ────────────────────────────────────────
    public static class Event {
        public final EventType type;
        public final Object    data;      // can be int, String, Item, etc.
        public       boolean   consumed = false;

        public Event(EventType type, Object data) {
            this.type = type;
            this.data = data;
        }

        public int    intData()    { return data instanceof Number ? ((Number)data).intValue() : 0; }
        public String stringData() { return data instanceof String ? (String) data : ""; }
    }

    // ─── Listener interface ───────────────────────────────────
    public interface Listener {
        void onEvent(Event e);
    }

    // ─── Internal storage ─────────────────────────────────────
    private final Map<EventType, List<Listener>> listeners =
            new EnumMap<>(EventType.class);

    public GameEventBus() {
        for (EventType t : EventType.values()) {
            listeners.put(t, new ArrayList<>());
        }
    }

    // ─── API ─────────────────────────────────────────────────
    public void subscribe(EventType type, Listener listener) {
        listeners.get(type).add(listener);
    }

    public void unsubscribe(EventType type, Listener listener) {
        listeners.get(type).remove(listener);
    }

    /** Fire event; calls all listeners in subscription order. */
    public void post(Event event) {
        List<Listener> ls = listeners.get(event.type);
        for (int i = 0, n = ls.size(); i < n; i++) {
            ls.get(i).onEvent(event);
            if (event.consumed) break;
        }
    }

    /** Convenience overload for simple string-payload events. */
    public void post(EventType type, String data) {
        post(new Event(type, data));
    }

    /** Convenience overload for numeric events. */
    public void post(EventType type, int data) {
        post(new Event(type, data));
    }
}
