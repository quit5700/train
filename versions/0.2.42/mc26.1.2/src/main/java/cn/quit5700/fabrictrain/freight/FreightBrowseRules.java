package cn.quit5700.fabrictrain.freight;

import java.util.List;
import java.util.Set;

public final class FreightBrowseRules {
    public static final List<String> CATEGORIES = List.of(
            "all", "build", "nature", "ore", "functional", "redstone", "tools",
            "equipment", "food", "magic", "drops", "transport", "other");

    private FreightBrowseRules() {}

    /** The final pixel of each cell is a visual gap and does not activate its neighbor. */
    public static int cell(double offset, int stride, int extent, int count) {
        if (offset < 0 || offset >= (double) stride * count) return -1;
        int index = (int) (offset / stride);
        return offset - index * stride < extent ? index : -1;
    }

    public static boolean visible(String itemId, boolean block, String category,
                                  boolean selectedOnly, Set<String> selections) {
        return (!selectedOnly || selections.contains(itemId))
                && (category.equals("all") || category.equals(category(itemId, block)));
    }

    public static String category(String itemId, boolean block) {
        String s = itemId.substring(itemId.indexOf(':') + 1);
        if (s.endsWith("_ore") || s.endsWith("_ingot") || s.endsWith("_nugget") || s.startsWith("raw_")
                || Set.of("diamond", "emerald", "coal", "charcoal", "lapis_lazuli", "quartz", "amethyst_shard").contains(s)) return "ore";
        if (s.contains("redstone") || s.contains("piston") || s.contains("repeater") || s.contains("comparator")
                || s.endsWith("_observer") || s.equals("observer")) return "redstone";
        if (s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_shovel") || s.endsWith("_hoe")
                || s.contains("shears") || s.contains("fishing_rod") || s.equals("flint_and_steel")) return "tools";
        if (s.endsWith("_sword") || s.endsWith("_helmet") || s.endsWith("_chestplate") || s.endsWith("_leggings")
                || s.endsWith("_boots") || s.equals("bow") || s.equals("crossbow") || s.equals("shield") || s.equals("elytra")
                || s.equals("trident") || s.equals("mace")) return "equipment";
        if (s.contains("potion") || s.equals("enchanted_book") || s.equals("book") || s.startsWith("blaze_")
                || s.equals("brewing_stand") || s.equals("enchanting_table")) return "magic";
        if (s.contains("minecart") || s.endsWith("rail") || s.contains("boat") || s.contains("raft")
                || Set.of("saddle", "locomotive", "passenger_car", "cargo_car").contains(s)) return "transport";
        if (s.contains("furnace") || s.contains("crafting_table") || s.contains("chest") || s.contains("hopper")
                || s.contains("anvil") || s.contains("barrel") || s.contains("smoker")
                || Set.of("freight_yard", "train_depot", "station_marker").contains(s)) return "functional";
        if (s.contains("apple") || s.contains("bread") || s.contains("wheat") || s.contains("seeds")
                || s.contains("carrot") || s.contains("potato") || s.contains("beef") || s.contains("porkchop")
                || s.contains("chicken") || s.contains("berries") || s.contains("melon")) return "food";
        if (Set.of("bone", "rotten_flesh", "string", "gunpowder", "ender_pearl", "spider_eye",
                "feather", "leather", "slime_ball").contains(s)) return "drops";
        if (s.endsWith("_log") || s.endsWith("_sapling") || s.endsWith("_leaves") || s.contains("flower")
                || s.equals("dirt") || s.contains("sand") || s.contains("grass")) return "nature";
        return block ? "build" : "other";
    }
}
