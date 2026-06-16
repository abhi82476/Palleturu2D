package com.villagelegends.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.villagelegends.GameConstants;
import com.villagelegends.VillageLegends;
import com.villagelegends.entities.NPC;
import com.villagelegends.systems.GameEventBus;

import java.util.*;

/**
 * DialogueUI renders the conversation panel at the bottom of the screen.
 *
 * Dialogue is defined as a simple tree stored in JSON under
 *   android/assets/data/dialogues/<npc_id>.json
 *
 * Node format:
 * {
 *   "id": "root",
 *   "speaker": "Elder Ramu",
 *   "text": "Namaste! Welcome to Peddapuram, young one.",
 *   "choices": [
 *     { "label": "Tell me about the village.", "next": "about_village" },
 *     { "label": "What work can I do?",         "next": "offer_quest_m01" },
 *     { "label": "Goodbye.",                    "next": "end" }
 *   ]
 * }
 *
 * A node with "next": "end" closes the dialogue.
 * A node with choice "offer_quest:<questId>" triggers that quest.
 */
public class DialogueUI {

    private static final float VW   = GameConstants.VIRTUAL_WIDTH;
    private static final float VH   = GameConstants.VIRTUAL_HEIGHT;
    private static final float PANEL_H  = 220f;
    private static final float PANEL_Y  = 0f;

    private final VillageLegends game;

    private boolean visible  = false;
    private NPC     speaker  = null;

    // Current dialogue state
    private String  speakerName  = "";
    private String  dialogueText = "";
    private final List<DialogueChoice> choices = new ArrayList<>();

    // Text reveal animation
    private String  displayText  = "";
    private int     charIndex    = 0;
    private float   charTimer    = 0f;
    private static final float CHAR_SPEED = 0.03f;   // seconds per character

    // Choice buttons (up to 4)
    private final Rectangle[] choiceRects = {
        new Rectangle(40, 110, 560, 40),
        new Rectangle(40,  65, 560, 40),
        new Rectangle(40,  20, 560, 40),
        new Rectangle(640, 110, 560, 40),
    };

    private final BitmapFont fontBody;
    private final BitmapFont fontSmall;

    // Hardcoded fallback dialogues (production: load from JSON)
    private static final Map<String, DialogueNode> DIALOGUE_DB = buildDialogueDB();

    // ─────────────────────────────────────────────────────────
    public DialogueUI(VillageLegends game) {
        this.game  = game;
        fontBody   = new BitmapFont(); fontBody.getData().setScale(1.2f);
        fontSmall  = new BitmapFont(); fontSmall.getData().setScale(1.0f);
    }

    // ── Open dialogue for NPC ─────────────────────────────────
    public void openFor(NPC npc) {
        this.speaker = npc;
        visible      = true;
        speakerName  = npc.getName();
        String rootId = npc.getDialogueId() != null ? npc.getDialogueId() : "dlg_generic_villager";
        showNode(rootId);
        game.audioManager.playSfx("ui_open");
    }

    public void close() {
        visible = false;
        speaker = null;
        choices.clear();
        charIndex = 0;
        game.eventBus.post(GameEventBus.EventType.DIALOGUE_END, "");
    }

    // ── Render ────────────────────────────────────────────────
    public void draw(SpriteBatch batch, float delta) {
        if (!visible) return;

        updateTextReveal(delta);

        batch.end();
        ShapeRenderer sr = game.shapeRenderer;
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Panel background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.04f, 0.06f, 0.10f, 0.92f);
        sr.rect(0, PANEL_Y, VW, PANEL_H);
        sr.setColor(0.12f, 0.16f, 0.22f, 0.85f);
        sr.rect(2, PANEL_Y + 2, VW - 4, PANEL_H - 4);

        // Choice button backgrounds
        for (int i = 0; i < choices.size() && i < choiceRects.length; i++) {
            Rectangle r = choiceRects[i];
            sr.setColor(0.15f, 0.35f, 0.20f, 0.88f);
            sr.rect(r.x, r.y, r.width, r.height);
        }
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.4f, 0.6f, 0.9f, 0.8f);
        sr.rect(0, PANEL_Y, VW, PANEL_H);
        for (int i = 0; i < choices.size() && i < choiceRects.length; i++) {
            Rectangle r = choiceRects[i];
            sr.setColor(0.3f, 0.7f, 0.4f, 0.9f);
            sr.rect(r.x, r.y, r.width, r.height);
        }
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Speaker name
        fontBody.setColor(Color.YELLOW);
        fontBody.getData().setScale(1.4f);
        fontBody.draw(batch, speakerName + ":", 20f, PANEL_H - 14f);
        fontBody.getData().setScale(1.2f);

        // Dialogue text (animated reveal)
        fontBody.setColor(Color.WHITE);
        fontBody.draw(batch, displayText, 20f, PANEL_H - 42f, VW * 0.55f, 1, true);

        // Choice labels
        fontSmall.getData().setScale(1.0f);
        for (int i = 0; i < choices.size() && i < choiceRects.length; i++) {
            Rectangle r = choiceRects[i];
            fontSmall.setColor(Color.CYAN);
            fontSmall.draw(batch, (i + 1) + ". " + choices.get(i).label,
                    r.x + 10, r.y + r.height - 10);
        }

        // Tap to continue hint
        if (charIndex < dialogueText.length()) {
            fontSmall.setColor(0.6f, 0.6f, 0.6f, 0.8f);
            fontSmall.draw(batch, "▼ tap to skip", VW - 150, 15f);
        }

        handleInput();
    }

    // ── Text reveal ───────────────────────────────────────────
    private void updateTextReveal(float delta) {
        if (charIndex < dialogueText.length()) {
            charTimer += delta;
            while (charTimer >= CHAR_SPEED && charIndex < dialogueText.length()) {
                charTimer -= CHAR_SPEED;
                charIndex++;
            }
            displayText = dialogueText.substring(0, charIndex);
        }
    }

    // ── Input ─────────────────────────────────────────────────
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float sx = Gdx.input.getX();
        float sy = Gdx.graphics.getHeight() - Gdx.input.getY();
        float scaleX = VW / Gdx.graphics.getWidth();
        float scaleY = VH / Gdx.graphics.getHeight();
        float vx = sx * scaleX, vy = sy * scaleY;

        // Skip text reveal
        if (charIndex < dialogueText.length()) {
            charIndex = dialogueText.length();
            displayText = dialogueText;
            return;
        }

        // Touch on choice buttons
        for (int i = 0; i < choices.size() && i < choiceRects.length; i++) {
            if (choiceRects[i].contains(vx, vy)) {
                selectChoice(choices.get(i));
                game.audioManager.playSfx("ui_click");
                return;
            }
        }

        // If no choices, tap anywhere to close
        if (choices.isEmpty() && vy < PANEL_H) {
            close();
        }
    }

    private void selectChoice(DialogueChoice choice) {
        if ("end".equals(choice.next)) {
            close();
            return;
        }
        if (choice.next.startsWith("offer_quest:")) {
            String qid = choice.next.substring("offer_quest:".length());
            game.questManager.startQuest(qid);
            close();
            return;
        }
        showNode(choice.next);
    }

    private void showNode(String nodeId) {
        DialogueNode node = DIALOGUE_DB.get(nodeId);
        if (node == null) node = DIALOGUE_DB.get("dlg_generic_villager");
        if (node == null) { close(); return; }

        dialogueText = node.text;
        displayText  = "";
        charIndex    = 0;
        charTimer    = 0f;

        choices.clear();
        if (node.choices != null) choices.addAll(node.choices);
    }

    // ── Hardcoded dialogue database ───────────────────────────
    private static Map<String, DialogueNode> buildDialogueDB() {
        Map<String, DialogueNode> db = new HashMap<>();

        db.put("dlg_generic_villager", node(
            "Namaste! Life in Peddapuram is peaceful, but strange things have been happening lately...",
            choice("What strange things?", "villager_strange"),
            choice("Goodbye.", "end")
        ));
        db.put("villager_strange", node(
            "Crops are going missing, people are scared at night. Stay safe, friend.",
            choice("I'll look into it.", "end"),
            choice("Goodbye.", "end")
        ));

        db.put("dlg_elder_ramu", node(
            "Welcome to Peddapuram! I am Elder Ramu. You must be the farmer's child. We need strong young people now more than ever.",
            choice("Tell me about the village.", "elder_about"),
            choice("What work can I do?",        "offer_quest:m01_intro"),
            choice("I'll come back later.",       "end")
        ));
        db.put("elder_about", node(
            "Our village has rice fields, a temple, market, and a school. But a gang has been eyeing our resources. Be careful.",
            choice("I'll protect the village!", "offer_quest:m01_intro"),
            choice("Goodbye.",                 "end")
        ));

        db.put("dlg_teacher_lakshmi", node(
            "Ah, a new student! Education and hard work are the foundation of a good life. Will you attend my class?",
            choice("Yes, I'll attend school.", "offer_quest:m03_school_day"),
            choice("Not today, Madam.",        "end")
        ));

        db.put("dlg_parvati", node(
            "Those men came in the middle of the night and took everything from our barn! I saw one of them – he had a red bandana.",
            choice("Can you describe them more?", "parvati_describe"),
            choice("I'll find them. Don't worry.", "offer_quest:m05_first_theft")
        ));
        db.put("parvati_describe", node(
            "There were three of them. They came from the highway direction and loaded goods into a truck.",
            choice("Thank you. I'll investigate.", "offer_quest:m05_first_theft")
        ));

        db.put("dlg_meena", node(
            "I'm journalist Meena. I've been gathering evidence against the Redstone Gang for months. Will you help me expose them?",
            choice("Yes, I'll help.", "offer_quest:m09_corrupt_official"),
            choice("Not yet.",        "end")
        ));

        db.put("dlg_fisherman", node(
            "Good day! The fish are biting well today. Want to try your luck at the lake?",
            choice("Tell me about fishing.", "fishing_tips"),
            choice("I'll try fishing.",      "end")
        ));
        db.put("fishing_tips", node(
            "Use the right bait, wait for the bobber to dip, then swipe up quickly! The legendary catfish only bites at dawn.",
            choice("Thanks for the tip!", "end")
        ));

        db.put("dlg_priest", node(
            "May Venkateswara bless you, child. The temple is always open for prayer. Donate to earn divine blessings.",
            choice("I'd like to pray.", "end"),
            choice("Goodbye.",         "end")
        ));

        db.put("dlg_generic_farmer", node(
            "Hard work in the field, good rains, and fair prices at the market – that's all a farmer asks for.",
            choice("Tell me about farming.", "farmer_tips"),
            choice("Goodbye.",              "end")
        ));
        db.put("farmer_tips", node(
            "Always water your crops twice a day. Watch out for pests after the rains. Groundnut and cotton fetch the best prices.",
            choice("Thanks!", "end")
        ));

        db.put("dlg_bus_driver", node(
            "This bus runs between the village and town. Costs 20 rupees. Hop in when you're ready!",
            choice("Take me to town.", "end"),
            choice("Maybe later.",    "end")
        ));

        db.put("dlg_headman", node(
            "As Panchayat headman, I urge all villagers to report suspicious activity immediately. The rule of law must prevail!",
            choice("What suspicious activity?", "offer_quest:m05_first_theft"),
            choice("I agree. Goodbye.",         "end")
        ));

        db.put("dlg_official", node(
            "I am Officer Krishnamurthy. Everything is under control. You have nothing to worry about.",
            choice("Really? I've heard otherwise.", "official_deny"),
            choice("Goodbye.",                      "end")
        ));
        db.put("official_deny", node(
            "Rumours! All rumours! Now stop asking questions and go home. Nothing to see here.",
            choice("Very suspicious...", "end")
        ));

        db.put("dlg_forest_officer", node(
            "These forest trails can be dangerous at night. Strange trucks have been spotted on the old logging road.",
            choice("I'll investigate.", "end"),
            choice("Thank you.",        "end")
        ));

        return db;
    }

    private static DialogueNode node(String text, DialogueChoice... choices) {
        DialogueNode n = new DialogueNode();
        n.text = text;
        n.choices = Arrays.asList(choices);
        return n;
    }

    private static DialogueChoice choice(String label, String next) {
        return new DialogueChoice(label, next);
    }

    public boolean isVisible() { return visible; }

    public void dispose() { fontBody.dispose(); fontSmall.dispose(); }

    // ── Inner data classes ────────────────────────────────────
    private static class DialogueNode {
        String text;
        List<DialogueChoice> choices;
    }

    private static class DialogueChoice {
        String label, next;
        DialogueChoice(String l, String n) { label = l; next = n; }
    }
}
