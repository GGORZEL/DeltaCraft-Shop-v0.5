package pl.deltacraft.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class ShopCatalog {
    public static final String[] CATEGORIES = {"Natura", "Budowlane", "Redstone", "Nether", "End", "Rudy", "Inne", "Narzedzia i zbroje", "Mikstury"};
    private final List<Item> items = new ArrayList<>();
    private final List<ItemStack> potions = new ArrayList<>();
    private final Properties prices = new Properties();
    private final Path path;

    public ShopCatalog(Path path) {
        this.path = path;
        if (Files.exists(path)) try (var reader = Files.newBufferedReader(path)) { prices.load(reader); }
        catch (IOException e) { System.err.println("[DeltaCraft] Prices read: " + e); }
        for (Item item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (!id.startsWith("minecraft:") || item == Items.AIR || forbidden(id)) continue;
            items.add(item);
            prices.putIfAbsent(id, Long.toString(defaultPriceDollars(id) * 100L));
        }
        items.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        // Vanilla potion holders: standard, extended and level II.
        addPotion(Potions.WATER);
        addPotion(Potions.NIGHT_VISION); addPotion(Potions.LONG_NIGHT_VISION);
        addPotion(Potions.INVISIBILITY); addPotion(Potions.LONG_INVISIBILITY);
        addPotion(Potions.LEAPING); addPotion(Potions.LONG_LEAPING); addPotion(Potions.STRONG_LEAPING);
        addPotion(Potions.FIRE_RESISTANCE); addPotion(Potions.LONG_FIRE_RESISTANCE);
        addPotion(Potions.SWIFTNESS); addPotion(Potions.LONG_SWIFTNESS); addPotion(Potions.STRONG_SWIFTNESS);
        addPotion(Potions.SLOWNESS); addPotion(Potions.LONG_SLOWNESS); addPotion(Potions.STRONG_SLOWNESS);
        addPotion(Potions.WATER_BREATHING); addPotion(Potions.LONG_WATER_BREATHING);
        addPotion(Potions.HEALING); addPotion(Potions.STRONG_HEALING);
        addPotion(Potions.HARMING); addPotion(Potions.STRONG_HARMING);
        addPotion(Potions.POISON); addPotion(Potions.LONG_POISON); addPotion(Potions.STRONG_POISON);
        addPotion(Potions.REGENERATION); addPotion(Potions.LONG_REGENERATION); addPotion(Potions.STRONG_REGENERATION);
        addPotion(Potions.STRENGTH); addPotion(Potions.LONG_STRENGTH); addPotion(Potions.STRONG_STRENGTH);
        addPotion(Potions.WEAKNESS); addPotion(Potions.LONG_WEAKNESS);
        addPotion(Potions.SLOW_FALLING); addPotion(Potions.LONG_SLOW_FALLING);
        addPotion(Potions.TURTLE_MASTER); addPotion(Potions.LONG_TURTLE_MASTER); addPotion(Potions.STRONG_TURTLE_MASTER);
        addPotion(Potions.LUCK);
        save();
    }
    private void addPotion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        potions.add(PotionContents.createItemStack(Items.POTION, potion));
    }
    // Only peaceful/neutral spawn eggs are offered; no hostile mob eggs.
    // All amounts are in dollars, converted to cents by price().
    private static final Map<String, Long> EGG_PRICES = new LinkedHashMap<>();
    static {
        egg("allay", 200000); egg("armadillo", 90000); egg("axolotl", 150000);
        egg("bat", 50000); egg("bee", 75000); egg("camel", 120000);
        egg("cat", 70000); egg("chicken", 50000); egg("cod", 50000);
        egg("cow", 60000); egg("dolphin", 140000); egg("donkey", 80000);
        egg("fox", 90000); egg("frog", 80000); egg("glow_squid", 85000);
        egg("goat", 85000); egg("horse", 100000); egg("iron_golem", 200000);
        egg("llama", 80000); egg("mooshroom", 150000); egg("mule", 85000);
        egg("ocelot", 95000); egg("panda", 140000); egg("parrot", 90000);
        egg("pig", 50000); egg("polar_bear", 130000); egg("pufferfish", 60000);
        egg("rabbit", 60000); egg("salmon", 50000); egg("sheep", 55000);
        egg("sniffer", 200000); egg("snow_golem", 120000); egg("squid", 55000);
        egg("strider", 120000); egg("tadpole", 60000); egg("trader_llama", 120000);
        egg("tropical_fish", 70000); egg("turtle", 95000); egg("villager", 200000);
        egg("wandering_trader", 200000); egg("wolf", 100000);
        egg("copper_golem", 170000); egg("happy_ghast", 200000);
    }
    private static void egg(String name, long dollars) { EGG_PRICES.put(name + "_spawn_egg", dollars); }
    public static Map<String, Long> eggPrices() { return Collections.unmodifiableMap(EGG_PRICES); }

    private boolean forbidden(String id) {
        String s = id.substring(id.indexOf(':') + 1);
        if (s.endsWith("_spawn_egg")) return !EGG_PRICES.containsKey(s);
        if (s.equals("dragon_egg") || s.equals("budding_amethyst") || s.equals("enchanted_book")
                || s.equals("heavy_core")) return true;
        String[] banned = {"command_block", "structure_block", "structure_void", "jigsaw", "barrier",
            "debug_stick", "light", "spawner", "bedrock", "end_portal", "nether_portal",
            "end_gateway", "moving_piston", "reinforced_deepslate", "knowledge_book",
            "petrified_oak_slab", "test_block", "test_instance_block"};
        for (String b : banned) if (s.contains(b)) return true;
        return false;
    }
    private long fixedPriceDollars(String s) {
        if (EGG_PRICES.containsKey(s)) return EGG_PRICES.get(s);
        if (s.equals("conduit")) return 10000;
        if (s.equals("beacon")) return 1000000;
        if (s.endsWith("_armor_trim_smithing_template")) return 10000;
        if (s.equals("enchanted_golden_apple")) return 50000;
        if (s.equals("elytra")) return 1000000;
        if (s.equals("respawn_anchor")) return 500;
        if (s.equals("totem_of_undying")) return 50000;
        if (s.equals("trident")) return 50000;
        if (s.equals("nether_star")) return 950000;
        if (s.equals("obsidian") || s.equals("crying_obsidian")) return 1000;
        if (s.equals("ender_chest")) return 25000;
        if (s.equals("end_crystal")) return 30000;
        if (s.equals("shulker_box") || s.endsWith("_shulker_box")) return 20000;
        if (s.equals("netherite_block")) return 315000;
        if (s.equals("shulker_shell")) return 10000;
        return -1;
    }
    private long defaultPriceDollars(String id) {
        String s = id.substring(id.indexOf(':') + 1);
        if (s.equals("shulker_shell")) return 10000;
        if (s.equals("ender_chest")) return 2500;
        if (s.equals("chorus_fruit")) return 200;
        if (s.equals("chorus_flower")) return 350;
        if (s.equals("chorus_plant")) return 250;
        if (s.equals("end_stone")) return 100;
        if (s.equals("end_stone_bricks")) return 120;
        if (s.equals("purpur_block")) return 150;
        if (s.startsWith("netherite_") && (s.endsWith("_helmet") || s.endsWith("_chestplate") || s.endsWith("_leggings") || s.endsWith("_boots"))) return 80000;
        if (s.equals("netherite_shovel")) return 50000;
        if (s.equals("netherite_hoe")) return 55000;
        if (s.equals("netherite_sword")) return 60000;
        if (s.equals("netherite_axe")) return 65000;
        if (s.equals("netherite_pickaxe")) return 70000;
        if (s.equals("diamond_shovel") || s.equals("diamond_hoe")) return 5000;
        if (s.equals("diamond_sword")) return 6000;
        if (s.equals("diamond_axe")) return 6500;
        if (s.equals("diamond_pickaxe")) return 7000;
        if (s.equals("diamond_boots")) return 7500;
        if (s.equals("diamond_helmet")) return 8000;
        if (s.equals("diamond_leggings")) return 9000;
        if (s.equals("diamond_chestplate")) return 10000;
        if (s.equals("diamond")) return 1500;
        if (s.equals("emerald")) return 2500;
        if (s.equals("ancient_debris")) return 20000;
        if (s.equals("netherite_ingot")) return 35000;
        if (s.equals("netherite_block")) return 315000;
        if (s.equals("netherite_scrap")) return 7500;
        if (s.equals("dirt") || s.equals("cobblestone") || s.equals("netherrack")) return 5;
        if (s.equals("potion") || s.equals("splash_potion") || s.equals("lingering_potion")) return 5000;
        if (s.contains("diamond") || s.contains("emerald")) return 5000;
        if (s.contains("gold")) return 750;
        if (s.contains("iron")) return 500;
        if (s.contains("redstone") || s.contains("piston") || s.contains("hopper")) return 250;
        if (s.contains("ore")) return 250;
        if (s.contains("log") || s.contains("planks") || s.contains("sand")) return 15;
        if (s.contains("stone") || s.contains("brick") || s.contains("glass")) return 25;
        return 100;
    }
    public long price(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        long fixed = fixedPriceDollars(name);
        if (fixed >= 0) return fixed * 100L;
        if (item instanceof BlockItem) return 10000L; // other blocks: $100
        try { return Math.max(500L, Long.parseLong(prices.getProperty(id, "10000"))); }
        catch (NumberFormatException e) { return 10000L; }
    }
    public long price(ItemStack stack) {
        if (stack.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                String name = contents.potion().get().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
                if (name.startsWith("strong_")) return 1000000L;
                if (name.startsWith("long_")) return 750000L;
                return 500000L;
            }
        }
        return price(stack.getItem());
    }
    public long price(Block block) { return price(block.asItem()); }
    public long sale(Item item) { return price(item) * 80L / 100L; }
    public long sale(ItemStack stack) { return price(stack) * 80L / 100L; }
    public boolean sellable(ItemStack stack) {
        if (stack.isEmpty() || !items.contains(stack.getItem())) return false;
        if (stack.has(DataComponents.POTION_CONTENTS)) {
            for (ItemStack potion : potions)
                if (ItemStack.isSameItemSameComponents(stack, potion)) return true;
            return false;
        }
        return stack.getComponentsPatch().isEmpty();
    }
    public void save() {
        try { Files.createDirectories(path.getParent()); try (var out = Files.newBufferedWriter(path)) {
            prices.store(out, "Prices in cents; minimum 500 = $5; sale price = 80%. Existing prices are preserved.");
        }} catch (IOException e) { System.err.println("[DeltaCraft] Prices save: " + e); }
    }
    public List<Item> category(int category) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) if (category(item) == category && category != 8) result.add(item);
        return result;
    }
    public List<ItemStack> categoryStacks(int category) {
        List<ItemStack> result = new ArrayList<>();
        if (category == 8) {
            for (ItemStack potion : potions) result.add(potion.copy());
            return result;
        }
        List<Item> sorted = category(category);
        sorted.sort(Comparator.comparing((Item i) -> sortKey(i, category)));
        if (category == 7) {
            // Page 1: armor (top-to-bottom); page 2+: tools and weapons.
            List<Item> armor = new ArrayList<>(), tools = new ArrayList<>();
            for (Item i : sorted) {
                if (armorSlot(id(i)) < 9) armor.add(i); else tools.add(i);
            }
            for (Item i : armor) result.add(new ItemStack(i));
            while (result.size() < 45) result.add(ItemStack.EMPTY);
            for (Item i : tools) result.add(new ItemStack(i));
        } else {
            for (Item i : sorted) result.add(new ItemStack(i));
        }
        return result;
    }
    private String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).getPath(); }
    private int armorSlot(String s) {
        if (s.endsWith("_helmet")) return 0;
        if (s.endsWith("_chestplate")) return 1;
        if (s.endsWith("_leggings")) return 2;
        if (s.endsWith("_boots")) return 3;
        if (s.equals("turtle_helmet")) return 0;
        return 99;
    }
    private int materialOrder(String s) {
        String[] mats = {"leather", "chainmail", "golden", "iron", "diamond", "netherite"};
        for (int i = 0; i < mats.length; i++) if (s.startsWith(mats[i] + "_")) return i;
        return 7;
    }
    // Sort by Minecraft block families rather than alphabetically mixing unrelated variants.
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"};
    private static final String[] STONES = {"stone", "cobblestone", "mossy_cobblestone", "smooth_stone", "stone_bricks", "mossy_stone_bricks", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite", "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks", "deepslate_tiles", "tuff", "polished_tuff", "tuff_bricks", "calcite", "dripstone", "sandstone", "red_sandstone", "prismarine", "prismarine_bricks", "dark_prismarine", "blackstone", "polished_blackstone", "polished_blackstone_bricks", "basalt", "smooth_basalt", "quartz", "purpur", "end_stone", "nether_bricks", "red_nether_bricks", "mud_bricks", "bricks"};
    private static final String[] COLORS = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
    private static final String[] COLOR_FAMILIES = {"wool", "carpet", "concrete", "concrete_powder", "terracotta", "glazed_terracotta", "stained_glass", "stained_glass_pane", "shulker_box", "bed", "banner", "candle"};
    private static final String[] MINERALS = {"coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "quartz", "amethyst", "netherite"};
    private static final String[] REDSTONE = {"redstone", "repeater", "comparator", "observer", "piston", "dispenser", "dropper", "hopper", "crafter", "rail", "lever", "button", "pressure_plate", "target", "tripwire", "daylight", "note_block", "sculk_sensor", "tnt"};
    private static String num(int n) { return String.format(Locale.ROOT, "%03d", n); }
    private static String variant(String s, String base) {
        if (s.equals(base)) return "00";
        String tail = s.startsWith(base + "_") ? s.substring(base.length() + 1) : s;
        String[] endings = {"planks", "log", "wood", "stem", "hyphae", "stripped_log", "stripped_wood", "stripped_stem", "stripped_hyphae", "leaves", "sapling", "propagule", "stairs", "slab", "wall", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate", "sign", "hanging_sign"};
        for (int i = 0; i < endings.length; i++) if (tail.equals(endings[i])) return num(i + 1);
        if (tail.equals("stairs")) return "20";
        if (tail.equals("slab")) return "21";
        if (tail.equals("wall")) return "22";
        return "90";
    }
    private static String woodKey(String s) {
        for (int i = 0; i < WOODS.length; i++) {
            String w = WOODS[i];
            if (s.equals(w) || s.startsWith(w + "_") || s.startsWith("stripped_" + w + "_")) {
                String normal = s.startsWith("stripped_") ? s.substring(9) : s;
                String v = variant(normal, w);
                if (s.startsWith("stripped_")) v = "005" + v;
                return "10-" + num(i) + "-" + v + "-" + s;
            }
        }
        return null;
    }
    private static String stoneKey(String s) {
        // Prefer the longest matching family so stone_bricks do not mix into stone.
        int best = -1;
        for (int i = 0; i < STONES.length; i++) {
            String f = STONES[i];
            if ((s.equals(f) || s.startsWith(f + "_")) && (best < 0 || f.length() > STONES[best].length())) best = i;
        }
        if (best < 0) return null;
        String f = STONES[best];
        return "20-" + num(best) + "-" + variant(s, f) + "-" + s;
    }
    private static String colorKey(String s) {
        for (int f = 0; f < COLOR_FAMILIES.length; f++) {
            String family = COLOR_FAMILIES[f];
            if (s.equals(family)) return "30-" + num(f) + "-000-" + s;
            for (int c = 0; c < COLORS.length; c++)
                if (s.equals(COLORS[c] + "_" + family)) return "30-" + num(f) + "-" + num(c + 1) + "-" + s;
        }
        return null;
    }
    private static String blockKey(String s) {
        String color = colorKey(s);
        if (color != null) return color;
        String wood = woodKey(s);
        if (wood != null) return wood;
        String stone = stoneKey(s);
        if (stone != null) return stone;
        String[] groups = {"dirt", "grass", "moss", "mud", "sand", "gravel", "snow", "ice", "glass", "clay", "terracotta", "concrete", "copper", "iron", "gold", "diamond", "emerald", "coal", "lapis", "redstone", "amethyst", "sculk", "coral", "mushroom", "flower", "shulker", "end", "nether"};
        for (int i = 0; i < groups.length; i++) if (s.contains(groups[i])) return "40-" + num(i) + "-" + s;
        return "80-" + s;
    }
    private String sortKey(Item item, int category) {
        String s = id(item);
        if (category == 7) {
            int slot = armorSlot(s);
            if (slot < 9) return String.format(Locale.ROOT, "00-%02d-%02d-%s", materialOrder(s), slot, s);
            String[] types = {"_sword", "_pickaxe", "_axe", "_shovel", "_hoe"};
            for (int i = 0; i < types.length; i++) if (s.endsWith(types[i]))
                return String.format(Locale.ROOT, "10-%02d-%02d-%s", materialOrder(s), i, s);
            if (s.endsWith("_armor_trim_smithing_template")) return "20-armor_trim-" + s;
            if (s.equals("netherite_upgrade_smithing_template")) return "20-upgrade-" + s;
            if (s.contains("arrow")) return "30-arrows-" + s;
            return "40-" + s;
        }
        if (category == 5) {
            for (int i = 0; i < MINERALS.length; i++) if (s.contains(MINERALS[i]) || (MINERALS[i].equals("netherite") && s.equals("ancient_debris"))) {
                int stage = s.equals("ancient_debris") ? 0
                    : s.contains("deepslate_") && s.endsWith("_ore") ? 1
                    : s.endsWith("_ore") ? 0 : s.startsWith("raw_") && !s.endsWith("_block") ? 2
                    : s.equals("netherite_scrap") ? 2
                    : s.endsWith("_block") ? 5 : s.contains("ingot") ? 4 : 3;
                return "00-" + num(i) + "-" + num(stage) + "-" + s;
            }
        }
        if (category == 6) {
            if (s.endsWith("_spawn_egg")) return "00-" + s;
            if (s.contains("map") || s.equals("compass") || s.equals("recovery_compass") || s.equals("clock")) return "01-" + s;
            if (s.contains("book") || s.equals("paper")) return "02-" + s;
            if (s.contains("bucket")) return "03-" + s;
            if (s.contains("music_disc") || s.equals("disc_fragment_5")) return "04-" + s;
            if (s.contains("saddle") || s.contains("horse_armor") || s.contains("harness")) return "05-" + s;
            if (s.contains("beef") || s.contains("porkchop") || s.contains("mutton") || s.contains("chicken") || s.contains("rabbit") || s.contains("fish") || s.contains("bread") || s.contains("apple") || s.contains("stew") || s.contains("cookie") || s.contains("cake") || s.contains("pie") || s.contains("carrot") || s.contains("potato")) return "06-" + s;
        }
        if (category == 2) {
            for (int i = 0; i < REDSTONE.length; i++) if (s.contains(REDSTONE[i])) return "00-" + num(i) + "-" + s;
        }
        if (item instanceof BlockItem) return "10-" + blockKey(s);
        return "90-" + s;
    }
    private static boolean oneOf(String s, String... names) {
        for (String name : names) if (s.equals(name)) return true;
        return false;
    }
    private static boolean endsWithAny(String s, String... endings) {
        for (String ending : endings) if (s.endsWith(ending)) return true;
        return false;
    }
    private static boolean containsAny(String s, String... fragments) {
        for (String part : fragments) if (s.contains(part)) return true;
        return false;
    }
    private static boolean mineral(String s) {
        if (oneOf(s, "ancient_debris", "netherite_scrap", "netherite_ingot", "netherite_block",
                "coal", "charcoal", "iron_ingot", "gold_ingot", "copper_ingot", "gold_nugget",
                "iron_nugget", "diamond", "emerald", "lapis_lazuli", "redstone", "quartz",
                "amethyst_shard", "raw_iron", "raw_gold", "raw_copper", "flint")) return true;
        if (s.endsWith("_ore")) return true;
        for (String mineral : MINERALS) {
            if (s.equals(mineral + "_block") || s.equals("raw_" + mineral + "_block")) return true;
        }
        return oneOf(s, "block_of_amethyst", "amethyst_block", "raw_iron_block", "raw_gold_block", "raw_copper_block");
    }
    private static boolean redstone(String s) {
        if (oneOf(s, "redstone_torch", "redstone_lamp", "redstone_wire", "repeater", "comparator",
                "observer", "piston", "sticky_piston", "dispenser", "dropper", "hopper",
                "crafter", "lever", "target", "tripwire_hook", "daylight_detector", "note_block",
                "jukebox", "tnt", "sculk_sensor", "calibrated_sculk_sensor", "lightning_rod",
                "redstone_block", "redstone_dust", "detector_rail", "powered_rail", "activator_rail",
                "rail", "minecart", "chest_minecart", "hopper_minecart", "tnt_minecart",
                "furnace_minecart", "command_block_minecart")) return true;
        return containsAny(s, "_button", "_pressure_plate", "_rail", "minecart", "copper_bulb")
                || s.endsWith("_piston") || s.endsWith("_sculk_sensor");
    }
    private static boolean equipment(String s) {
        return endsWithAny(s, "_helmet", "_chestplate", "_leggings", "_boots", "_sword",
                "_pickaxe", "_axe", "_shovel", "_hoe", "_armor_trim_smithing_template")
                || oneOf(s, "bow", "crossbow", "shield", "fishing_rod", "shears", "trident", "elytra",
                    "mace", "brush", "flint_and_steel", "carrot_on_a_stick", "warped_fungus_on_a_stick",
                    "netherite_upgrade_smithing_template") || s.contains("arrow");
    }
    private static boolean nether(String s) {
        return containsAny(s, "nether", "basalt", "blackstone", "soul_", "crimson", "warped",
                "netherrack", "glowstone", "magma", "blaze", "ghast", "weeping_vines", "twisting_vines")
                || oneOf(s, "quartz_bricks", "quartz_block", "smooth_quartz", "chiseled_quartz_block",
                    "quartz_pillar", "respawn_anchor", "nether_wart", "nether_wart_block");
    }
    private static boolean end(String s) {
        return containsAny(s, "end_stone", "end_rod", "purpur", "chorus", "shulker", "ender")
                || oneOf(s, "end_crystal", "dragon_breath", "dragon_head", "elytra");
    }
    private static boolean nature(String s) {
        if (containsAny(s, "leaves", "sapling", "propagule", "flower", "grass", "dirt", "moss",
                "mushroom", "sand", "snow", "coral", "kelp", "seagrass", "lily_pad",
                "vine", "roots", "azalea", "bamboo", "cactus", "sugar_cane", "wheat",
                "beetroot", "carrot", "potato", "pumpkin", "melon", "cocoa", "fern",
                "mud", "clay", "gravel", "ice", "dripleaf", "dripstone", "spore_blossom")) return true;
        return endsWithAny(s, "_log", "_wood", "_stem", "_hyphae")
                || s.startsWith("stripped_") || oneOf(s, "hay_block", "dried_kelp_block", "sponge", "wet_sponge");
    }
    private static boolean building(String s, Item item) {
        if (item instanceof BlockItem) return true;
        return endsWithAny(s, "_sign", "_hanging_sign", "_door", "_trapdoor", "_banner")
                || oneOf(s, "item_frame", "glow_item_frame", "painting", "flower_pot", "armor_stand");
    }
    public int category(Item item) {
        String s = id(item);
        if (oneOf(s, "potion", "splash_potion", "lingering_potion", "tipped_arrow")) return 8;
        // Precedence matters: mineral blocks and Netherite must not fall into Nether/building.
        if (mineral(s)) return 5;
        if (equipment(s)) return 7;
        if (redstone(s)) return 2;
        if (end(s)) return 4;
        if (nether(s)) return 3;
        if (nature(s)) return 0;
        if (building(s, item)) return 1;
        return 6;
    }
}
