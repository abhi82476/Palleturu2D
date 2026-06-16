package com.villagelegends.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * LocalizationManager — lightweight runtime string table.
 *
 * Supports EN (English), TE (Telugu), HI (Hindi).
 * Strings are bundled here for offline access without Android resource system.
 * The game defaults to EN; switches take effect immediately.
 *
 * Usage:
 *   LocalizationManager.setLanguage(game.language);
 *   String text = LocalizationManager.get("crop_rice");  // "Rice" / "వరి" / "चावल"
 */
public class LocalizationManager {

    private static String language = "en";

    private static final Map<String, String[]> STRINGS = new HashMap<>();
    // Indices: [0]=EN  [1]=TE  [2]=HI

    static {
        // UI labels
        put("new_game",       "New Game",       "కొత్త గేమ్",         "नया खेल");
        put("load_game",      "Load Game",      "గేమ్ లోడ్",          "खेल लोड करें");
        put("save_game",      "Save Game",      "గేమ్ సేవ్",          "खेल सेव करें");
        put("settings",       "Settings",       "అమరికలు",            "सेटिंग्स");
        put("quit",           "Quit",           "నిష్క్రమించు",        "बाहर निकलें");
        put("resume",         "Resume",         "కొనసాగించు",         "फिर से शुरू");
        put("inventory",      "Inventory",      "సామాను",             "इन्वेंटरी");
        put("quests",         "Quests",         "పనులు",              "अभियान");
        put("map",            "Map",            "పటం",                "नक्शा");
        put("pause",          "Paused",         "ఆపబడింది",           "रुका हुआ");
        put("attack",         "Attack",         "దాడి",               "हमला");
        put("action",         "Action",         "చర్య",               "कार्य");
        put("sprint",         "Sprint",         "పరిగెత్తు",          "तेज दौड़ो");
        put("dodge",          "Dodge",          "తప్పించుకో",         "चकमा दो");
        put("buy",            "Buy",            "కొనుగోలు",           "खरीदें");
        put("sell",           "Sell",           "అమ్ముము",            "बेचें");
        put("close",          "Close",          "మూసివేయి",           "बंद करें");

        // HUD
        put("hp",             "HP",             "ఆరోగ్యం",            "स्वास्थ्य");
        put("sta",            "STA",            "శక్తి",              "शक्ति");
        put("day",            "Day",            "రోజు",               "दिन");
        put("money",          "₹",              "₹",                  "₹");
        put("reputation",     "Reputation",     "ప్రతిష్ఠ",           "प्रतिष्ठा");

        // Crop names
        put("crop_rice",        "Rice",         "వరి",                "चावल");
        put("crop_groundnut",   "Groundnut",    "వేరుసెనగ",           "मूंगफली");
        put("crop_cotton",      "Cotton",       "పత్తి",              "कपास");
        put("crop_sugarcane",   "Sugarcane",    "చెరకు",              "गन्ना");
        put("crop_tomato",      "Tomato",       "టమాటా",              "टमाटर");
        put("crop_brinjal",     "Brinjal",      "వంకాయ",              "बैंगन");
        put("crop_chilli",      "Chilli",       "మిరపకాయ",            "मिर्च");
        put("crop_onion",       "Onion",        "ఉల్లిపాయ",           "प्याज");

        // Farm states
        put("farm_empty",       "Empty Plot",   "ఖాళీ పొలం",          "खाली खेत");
        put("farm_plowed",      "Plowed",       "దున్నిన",            "जुते हुए");
        put("farm_planted",     "Planted",      "నాటిన",              "बोया हुआ");
        put("farm_growing",     "Growing",      "పెరుగుతోంది",        "बढ़ रहा है");
        put("farm_mature",      "Harvest Ready","కోత సిద్ధం",         "कटाई तैयार");
        put("farm_dead",        "Dead Crop",    "చనిపోయిన పంట",      "मृत फसल");

        // Regions
        put("region_main_village", "Main Village",  "ముఖ్య గ్రామం",   "मुख्य गाँव");
        put("region_farmlands",    "Farmlands",     "పొలాలు",         "खेत");
        put("region_forest",       "Forest",        "అడవి",           "जंगल");
        put("region_lake",         "Lake",          "చెరువు",         "झील");
        put("region_town",         "Town",          "పట్టణం",         "शहर");
        put("region_highway",      "Highway",       "హైవే",           "राजमार्ग");

        // Vehicles
        put("vehicle_bicycle",     "Bicycle",       "సైకిల్",         "साइकिल");
        put("vehicle_motorcycle",  "Motorcycle",    "మోటార్ సైకిల్",   "मोटरसाइकिल");
        put("vehicle_tractor",     "Tractor",       "ట్రాక్టర్",       "ट्रेक्टर");
        put("vehicle_bullock_cart","Bullock Cart",  "ఎద్దుల బండి",    "बैलगाड़ी");
        put("vehicle_boat",        "Boat",          "పడవ",            "नाव");
        put("vehicle_bus",         "Bus",           "బస్సు",          "बस");

        // Festivals
        put("festival_sankranti",  "Sankranti",     "సంక్రాంతి",      "मकर संक्रांति");
        put("festival_ugadi",      "Ugadi",         "ఉగాది",          "उगादि");
        put("festival_dasara",     "Dasara",        "దసరా",           "दशहरा");
        put("festival_deepavali",  "Deepavali",     "దీపావళి",        "दीपावली");

        // Notifications
        put("notif_quest_done",    "Quest Complete!",  "పని పూర్తయింది!",  "अभियान पूरा!");
        put("notif_saved",         "Game Saved",       "సేవ్ అయింది",      "सेव हो गया");
        put("notif_low_fuel",      "Low Fuel!",        "ఇంధనం తక్కువ!",   "ईंधन कम है!");
        put("notif_market_closed", "Market Closed",    "మార్కెట్ మూసివేసారు","बाजार बंद है");
        put("notif_crop_ready",    "Crop Ready!",      "పంట సిద్ధం!",     "फसल तैयार!");
        put("notif_pest_attack",   "Pest Attack!",     "పురుగుల దాడి!",   "कीट हमला!");
        put("notif_level_up",      "Reputation Up!",   "పేరు పెరిగింది!",  "प्रतिष्ठा बढ़ी!");

        // Combat
        put("combat_stealth",     "Stealth Takedown!", "చాటు దాడి!",      "चोरी हमला!");
        put("combat_blocked",     "Blocked!",          "నిరోధించారు!",     "रोका गया!");
        put("combat_dodged",      "Dodged!",           "తప్పించుకున్నారు!","चकमा दिया!");
        put("combat_knocked_out", "Knocked Out!",      "బయటపడ్డారు!",     "बेहोश!");

        // Quest status
        put("quest_locked",      "Locked",     "లాక్ అయింది",    "बंद है");
        put("quest_available",   "Available",  "అందుబాటులో",     "उपलब्ध");
        put("quest_active",      "Active",     "చురుకుగా",       "सक्रिय");
        put("quest_complete",    "Complete",   "పూర్తయింది",     "पूर्ण");
        put("quest_failed",      "Failed",     "విఫలమైంది",      "असफल");

        // Times of day
        put("time_morning",  "Morning",   "ఉదయం",       "सुबह");
        put("time_afternoon","Afternoon", "మధ్యాహ్నం",   "दोपहर");
        put("time_evening",  "Evening",   "సాయంత్రం",   "शाम");
        put("time_night",    "Night",     "రాత్రి",      "रात");
        put("time_midnight", "Midnight",  "అర్థరాత్రి",  "आधी रात");

        // Dialogue
        put("dlg_talk",    "Talk",        "మాట్లాడు",     "बात करो");
        put("dlg_goodbye", "Goodbye",     "వెళ్ళొస్తాను", "अलविदा");
        put("dlg_accept",  "I Accept",    "అంగీకరిస్తాను","मैं स्वीकार करता हूँ");
        put("dlg_decline", "Not now",     "ఇప్పుడు కాదు", "अभी नहीं");
    }

    private static void put(String key, String en, String te, String hi) {
        STRINGS.put(key, new String[]{en, te, hi});
    }

    // ── API ───────────────────────────────────────────────────
    public static void setLanguage(String lang) {
        language = lang != null ? lang.toLowerCase() : "en";
    }

    public static String get(String key) {
        String[] vals = STRINGS.get(key);
        if (vals == null) return key;
        switch (language) {
            case "te": return vals[1];
            case "hi": return vals[2];
            default:   return vals[0];
        }
    }

    /** Get with fallback text if key is missing */
    public static String get(String key, String fallback) {
        String[] vals = STRINGS.get(key);
        if (vals == null) return fallback;
        switch (language) {
            case "te": return vals[1].isEmpty() ? fallback : vals[1];
            case "hi": return vals[2].isEmpty() ? fallback : vals[2];
            default:   return vals[0];
        }
    }

    public static String getLanguage() { return language; }
    public static boolean isTelugu()   { return "te".equals(language); }
    public static boolean isHindi()    { return "hi".equals(language); }
}
