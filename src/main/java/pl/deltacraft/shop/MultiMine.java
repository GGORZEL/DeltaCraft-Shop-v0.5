package pl.deltacraft.shop;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** MultiMine progression. Level zero requires 100 XP for level one. */
public final class MultiMine {
    private MultiMine() {}

    public static long requiredXp(int level) {
        // Capped to keep arithmetic and stored data safe at very high levels.
        return Math.max(1L, (long) Math.ceil(Math.min(1_000_000_000d, 100d * Math.pow(2.10d, level))));
    }

    public static double earningsMultiplier(int level) {
        return Math.min(1_000_000d, Math.pow(1.10d, level));
    }

    public static int miningXp(Block block) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return 3;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return 4;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return 5;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return 7;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return 8;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return 10;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 15;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return 20;
        if (block == Blocks.ANCIENT_DEBRIS) return 30;
        return 1;
    }
}
