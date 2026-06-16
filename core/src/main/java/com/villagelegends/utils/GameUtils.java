package com.villagelegends.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * GameUtils — stateless utility methods used across all systems.
 */
public final class GameUtils {

    private GameUtils() {}

    // ── Math ──────────────────────────────────────────────────
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * MathUtils.clamp(t, 0f, 1f);
    }

    public static float distanceSq(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public static boolean withinRadius(float x1, float y1, float x2, float y2, float radius) {
        return distanceSq(x1, y1, x2, y2) <= radius * radius;
    }

    public static float angleToTarget(float fromX, float fromY, float toX, float toY) {
        return MathUtils.atan2(toY - fromY, toX - fromX) * MathUtils.radiansToDegrees;
    }

    public static Vector2 directionTo(float fromX, float fromY, float toX, float toY) {
        float dx = toX - fromX, dy = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return new Vector2(0, 0);
        return new Vector2(dx / len, dy / len);
    }

    /** Clamp angle to 0..360 range */
    public static float normaliseAngle(float angleDeg) {
        return ((angleDeg % 360) + 360) % 360;
    }

    /** Map a value from one range to another */
    public static float remap(float value, float fromMin, float fromMax,
                              float toMin, float toMax) {
        if (fromMax == fromMin) return toMin;
        return toMin + (toMax - toMin) * ((value - fromMin) / (fromMax - fromMin));
    }

    // ── Time formatting ───────────────────────────────────────
    public static String formatGameTime(float gameHour) {
        int h = (int) gameHour % 24;
        int m = (int) ((gameHour - (int) gameHour) * 60);
        String period = h < 12 ? "AM" : "PM";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return String.format("%d:%02d %s", h12, m, period);
    }

    public static String formatRealTime(float totalSeconds) {
        int h = (int) (totalSeconds / 3600);
        int m = (int) ((totalSeconds % 3600) / 60);
        int s = (int) (totalSeconds % 60);
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    /** Format money with Indian number system (e.g. 1,00,000) */
    public static String formatMoney(int amount) {
        if (amount < 1000) return "₹" + amount;
        if (amount < 100000) {
            int thousands = amount / 1000;
            int remainder = amount % 1000;
            return "₹" + thousands + "," + String.format("%03d", remainder);
        }
        int lakhs  = amount / 100000;
        int rest   = amount % 100000;
        int thousands = rest / 1000;
        int units  = rest % 1000;
        return "₹" + lakhs + "," + String.format("%02d", thousands)
                + "," + String.format("%03d", units);
    }

    // ── Tile / pixel conversions ──────────────────────────────
    public static float tileToPixel(int tile) {
        return tile * com.villagelegends.GameConstants.TILE_SIZE;
    }

    public static int pixelToTile(float pixel) {
        return (int) (pixel / com.villagelegends.GameConstants.TILE_SIZE);
    }

    // ── Random helpers ────────────────────────────────────────
    public static float randomRange(float min, float max) {
        return min + MathUtils.random() * (max - min);
    }

    public static int randomRange(int min, int max) {
        return min + MathUtils.random(max - min);
    }

    public static boolean chance(float probability) {
        return MathUtils.random() < probability;
    }

    // ── String helpers ────────────────────────────────────────
    public static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String idToDisplay(String id) {
        // "groundnut_bag" → "Groundnut Bag"
        if (id == null || id.isEmpty()) return id;
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(capitalise(p)).append(" ");
        }
        return sb.toString().trim();
    }

    /** Wrap text to given max characters per line (simple word wrap) */
    public static String wordWrap(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        StringBuilder sb  = new StringBuilder();
        String[]       words = text.split(" ");
        int            lineLen = 0;
        for (String word : words) {
            if (lineLen + word.length() + 1 > maxChars) {
                sb.append('\n');
                lineLen = 0;
            } else if (lineLen > 0) {
                sb.append(' ');
                lineLen++;
            }
            sb.append(word);
            lineLen += word.length();
        }
        return sb.toString();
    }

    // ── Colour helpers ────────────────────────────────────────
    public static float[] hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1f / 3f);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1f / 3f);
        }
        return new float[]{r, g, b};
    }

    private static float hue2rgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6f) return p + (q - p) * 6 * t;
        if (t < 1f / 2f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6;
        return p;
    }
}
