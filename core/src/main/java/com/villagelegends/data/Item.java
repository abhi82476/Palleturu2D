package com.villagelegends.data;

/**
 * Item — any object the player can carry in inventory.
 * Used for seeds, crops, tools, weapons, quest items, consumables, keys.
 */
public class Item {

    public enum Category {
        SEED, CROP, TOOL, WEAPON, CONSUMABLE, QUEST_ITEM, FUEL, FISH, MISC
    }

    // ── Fields (public for LibGDX Json) ───────────────────────
    public String   id;
    public String   displayName;
    public Category category;
    public int      price;        // current market value
    public int      quantity;     // stack size
    public boolean  stackable;
    public String   description;
    public String   iconRegion;   // TextureAtlas region name

    /** No-arg constructor for Json deserialisation */
    public Item() {}

    public Item(String id, String displayName, Category category, int price) {
        this.id          = id;
        this.displayName = displayName;
        this.category    = category;
        this.price       = price;
        this.quantity    = 1;
        this.stackable   = (category == Category.SEED  || category == Category.CROP
                         || category == Category.FISH  || category == Category.FUEL);
        this.iconRegion  = "item_" + id;
        this.description = defaultDescription(category, displayName);
    }

    /** Convenience factory for shop items */
    public static Item createShopItem(String id, int price) {
        Category cat = inferCategory(id);
        String   name = idToDisplayName(id);
        return new Item(id, name, cat, price);
    }

    private static Category inferCategory(String id) {
        if (id.endsWith("_seed"))     return Category.SEED;
        if (id.endsWith("_bag"))      return Category.CROP;
        if (id.endsWith("_fish"))     return Category.FISH;
        if (id.endsWith("_can"))      return Category.FUEL;
        if (id.contains("hoe") || id.contains("rod") || id.contains("bucket")
                || id.contains("sickle") || id.contains("can"))  return Category.TOOL;
        if (id.contains("stick") || id.contains("sling") || id.contains("hoe"))
            return Category.WEAPON;
        if (id.equals("medicine") || id.equals("pesticide") || id.equals("fertiliser"))
            return Category.CONSUMABLE;
        return Category.MISC;
    }

    private static String idToDisplayName(String id) {
        // "groundnut_seed" → "Groundnut Seed"
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty())
                sb.append(Character.toUpperCase(p.charAt(0)))
                  .append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String defaultDescription(Category cat, String name) {
        switch (cat) {
            case SEED:       return "Plant " + name + " on a plowed farm plot.";
            case CROP:       return "Sell this bag at the market for profit.";
            case TOOL:       return "A useful tool for village life.";
            case WEAPON:     return "For non-lethal self-defence only.";
            case CONSUMABLE: return "Consumable item with useful effects.";
            case QUEST_ITEM: return "Important quest item. Do not sell!";
            default:         return name;
        }
    }

    public boolean isQuestItem()  { return category == Category.QUEST_ITEM; }
    public boolean isSellable()   { return category != Category.QUEST_ITEM; }
    public boolean isWeapon()     { return category == Category.WEAPON; }

    @Override public String toString() {
        return displayName + (quantity > 1 ? " x" + quantity : "");
    }
}
