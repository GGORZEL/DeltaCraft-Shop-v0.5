package pl.deltacraft.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** All ordinary registered Minecraft items, with configurable buy prices in cents. */
public final class ShopCatalog {
    public static final String[] CATEGORIES = {"Natura", "Budowlane", "Redstone", "Nether", "End", "Rudy", "Inne", "Narzedzia i zbroje", "Mikstury"};
    private final List<Item> items = new ArrayList<>();
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
        save();
    }
    private boolean forbidden(String id) {
        String[] banned = {"command_block", "structure_block", "structure_void", "jigsaw", "barrier", "debug_stick", "light", "spawner", "bedrock", "end_portal", "nether_portal", "end_gateway", "moving_piston", "reinforced_deepslate", "knowledge_book", "petrified_oak_slab", "test_block", "test_instance_block"};
        for (String b : banned) if (id.contains(b)) return true;
        return false;
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
        if (s.equals("netherite_scrap")) return 7500;
        if (s.equals("dirt") || s.equals("cobblestone") || s.equals("netherrack")) return 5;
        if (s.equals("potion")) return 5000;
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
        try { return Math.max(500L, Long.parseLong(prices.getProperty(id, "10000"))); }
        catch (NumberFormatException e) { return 10000L; }
    }
    public long price(Block block) { return price(block.asItem()); }
    public long sale(Item item) { return price(item) * 80L / 100L; }
    public boolean sellable(ItemStack stack) {
        if (stack.isEmpty() || !items.contains(stack.getItem())) return false;
        // Preserve any named, enchanted, filled or otherwise customized stacks.
        return stack.getComponentsPatch().isEmpty();
    }
    public void save() {
        try { Files.createDirectories(path.getParent()); try (var out = Files.newBufferedWriter(path)) {
            prices.store(out, "Prices in cents; minimum 500 = $5; sale price = 80%. Existing prices are preserved.");
        }} catch (IOException e) { System.err.println("[DeltaCraft] Prices save: " + e); }
    }
    public List<Item> category(int category) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) if (category(item) == category) result.add(item);
        return result;
    }
    public int category(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (id.equals("potion") || id.equals("splash_potion") || id.equals("lingering_potion")) return 8;
        if (id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings") || id.endsWith("_boots") || id.endsWith("_sword") || id.endsWith("_pickaxe") || id.endsWith("_axe") || id.endsWith("_shovel") || id.endsWith("_hoe") || id.equals("bow") || id.equals("crossbow") || id.equals("shield") || id.equals("fishing_rod") || id.equals("shears")) return 7;
        if (id.contains("ore") || id.contains("ancient_debris") || id.equals("diamond") || id.equals("emerald") || id.equals("iron_ingot") || id.equals("gold_ingot") || id.equals("copper_ingot")) return 5;
        if (id.contains("redstone") || id.contains("piston") || id.contains("observer") || id.contains("hopper") || id.contains("repeater") || id.contains("comparator") || id.contains("dispenser") || id.contains("dropper") || id.contains("rail") || id.contains("lever")) return 2;
        if (id.contains("nether") || id.contains("basalt") || id.contains("blackstone") || id.contains("soul_") || id.contains("crimson") || id.contains("warped") || id.contains("netherrack") || id.contains("quartz")) return 3;
        if (id.contains("end_") || id.contains("purpur") || id.contains("chorus") || id.contains("shulker") || id.equals("ender_chest")) return 4;
        if (id.contains("leaves") || id.contains("log") || id.contains("sapling") || id.contains("flower") || id.contains("grass") || id.contains("dirt") || id.contains("moss") || id.contains("mushroom") || id.contains("sand") || id.contains("snow")) return 0;
        if (id.contains("planks") || id.contains("brick") || id.contains("concrete") || id.contains("terracotta") || id.contains("glass") || id.contains("stairs") || id.contains("slab") || id.contains("wall") || id.contains("fence") || id.contains("wool")) return 1;
        return 6;
    }
}
