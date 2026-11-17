package pl.deltacraft.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Server-side shop: potions retain their actual potion components. */
public final class ShopMenu extends ChestMenu {
    private final SimpleContainer icons;
    private final ServerPlayer owner;
    private final PlayerAccount account;
    private final ShopCatalog catalog;
    private int category = -1, page = 0, quantity = 1;
    private ItemStack selected = ItemStack.EMPTY;
    private String noticeText = "";
    private final Map<Integer, ItemStack> visible = new HashMap<>();

    public ShopMenu(int id, Inventory inventory, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog) {
        this(id, inventory, owner, account, catalog, new SimpleContainer(54));
    }
    private ShopMenu(int id, Inventory inventory, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog, SimpleContainer icons) {
        super(MenuType.GENERIC_9x6, id, inventory, icons, 6);
        this.icons = icons;
        this.owner = owner;
        this.account = account;
        this.catalog = catalog;
        for (int i = 0; i < 54; i++) {
            final int index = i;
            this.slots.set(i, new Slot(icons, index, 8 + index % 9 * 18, 18 + index / 9 * 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        render();
    }
    private void put(int slot, Item item, String name) {
        put(slot, new ItemStack(item), name);
    }
    private void put(int slot, ItemStack original, String name) {
        ItemStack stack = original.copy();
        stack.setCount(1);
        // Preserve vanilla potion tooltip (effects, strength, duration).
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (slot != 4 || selected.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7Kliknij, aby otworzyc"))));
        }
        icons.setItem(slot, stack);
    }
    private void render() {
        visible.clear();
        for (int i = 0; i < 54; i++) icons.setItem(i, ItemStack.EMPTY);
        if (!selected.isEmpty()) {
            put(4, selected, "§b" + selected.getHoverName().getString());
            put(10, Items.REDSTONE, "§c-64");
            put(11, Items.REDSTONE, "§c-10");
            put(12, Items.REDSTONE, "§c-1");
            put(14, Items.EMERALD, "§a+1");
            put(15, Items.EMERALD, "§a+10");
            put(16, Items.EMERALD, "§a+64");
            put(22, Items.EMERALD_BLOCK, "§aKup " + quantity + " za " + dollars(catalog.price(selected) * quantity));
            put(31, Items.PAPER, "§eIlosc: " + quantity + " | sztuka " + dollars(catalog.price(selected)));
            put(49, Items.ARROW, "§ePowrot");
        } else if (category == -1) {
            put(4, Items.NETHER_STAR, "§dMultiMine LVL " + account.level + " | XP " + account.xp + "/" + MultiMine.requiredXp(account.level) + " | x" + String.format(Locale.ROOT, "%.2f", MultiMine.earningsMultiplier(account.level)));
            Item[] categoryIcons = {Items.OAK_SAPLING, Items.BRICKS, Items.REDSTONE, Items.NETHERRACK, Items.END_STONE, Items.DIAMOND, Items.CHEST, Items.DIAMOND_PICKAXE, Items.POTION};
            for (int i = 0; i < categoryIcons.length; i++) put(18 + i, categoryIcons[i], "§b" + ShopCatalog.CATEGORIES[i]);
        } else {
            List<ItemStack> entries = catalog.categoryStacks(category);
            for (int i = 0; i < 45; i++) {
                int idx = page * 45 + i;
                if (idx >= entries.size()) break;
                ItemStack item = entries.get(idx);
                put(i, item, "§b" + item.getHoverName().getString() + " §aKup: " + dollars(catalog.price(item)) + " §6Sprzedaz: " + dollars(catalog.sale(item)) + " §7[LPM kup / PPM sprzedaj]");
                visible.put(i, item.copy());
            }
            put(48, Items.ARROW, "§ePoprzednia strona");
            put(49, Items.BARRIER, "§cKategorie");
            put(50, Items.ARROW, "§eNastepna strona");
        }
        if (!noticeText.isEmpty()) {
            ItemStack notice = new ItemStack(Items.PAPER);
            notice.set(DataComponents.CUSTOM_NAME, Component.literal(noticeText));
            icons.setItem(46, notice);
        }
        broadcastChanges();
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot >= 0 && slot < 54) {
            if (player != owner) return;
            if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE) return;
            if (!selected.isEmpty()) {
                if (input != ContainerInput.PICKUP || button != 0) return;
                switch (slot) {
                    case 10 -> quantity = Math.max(1, quantity - 64);
                    case 11 -> quantity = Math.max(1, quantity - 10);
                    case 12 -> quantity = Math.max(1, quantity - 1);
                    case 14 -> quantity = Math.min(2304, quantity + 1);
                    case 15 -> quantity = Math.min(2304, quantity + 10);
                    case 16 -> quantity = Math.min(2304, quantity + 64);
                    case 22 -> buy();
                    case 49 -> selected = ItemStack.EMPTY;
                }
                render();
                return;
            }
            if (category == -1) {
                if (input == ContainerInput.PICKUP && button == 0 && slot >= 18 && slot <= 26) {
                    category = slot - 18; page = 0; render();
                }
                return;
            }
            if (slot == 48 && input == ContainerInput.PICKUP && button == 0) { page = Math.max(0, page - 1); render(); return; }
            if (slot == 50 && input == ContainerInput.PICKUP && button == 0) {
                if ((page + 1) * 45 < catalog.categoryStacks(category).size()) page++;
                render(); return;
            }
            if (slot == 49 && input == ContainerInput.PICKUP && button == 0) { category = -1; render(); return; }
            ItemStack item = visible.get(slot);
            if (item == null) return;
            if (input == ContainerInput.PICKUP && button == 0) { selected = item.copy(); quantity = 1; render(); }
            else if (input == ContainerInput.PICKUP && button == 1) sell(item, false);
            else if (input == ContainerInput.QUICK_MOVE && button == 1) sell(item, true);
            return;
        }
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.PICKUP_ALL || input == ContainerInput.QUICK_CRAFT) return;
        super.clicked(slot, button, input, player);
    }
    private void info(String message) {
        noticeText = message;
        ItemStack notice = new ItemStack(Items.PAPER);
        notice.set(DataComponents.CUSTOM_NAME, Component.literal(message));
        icons.setItem(46, notice);
        broadcastChanges();
    }
    private void buy() {
        long unit = catalog.price(selected);
        long total = unit * quantity;
        if (account.cents < total) { info("§cZa malo pieniedzy: " + dollars(total)); return; }
        int capacity = 0;
        ItemStack sample = selected.copy();
        sample.setCount(1);
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (stack.isEmpty()) capacity += sample.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, sample)) capacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
        }
        if (capacity < quantity) { info("§cBrak miejsca w ekwipunku"); return; }
        int left = quantity;
        while (left > 0) {
            int count = Math.min(left, sample.getMaxStackSize());
            ItemStack stack = sample.copy();
            stack.setCount(count);
            if (!owner.getInventory().add(stack)) {
                // The capacity check above should normally prevent this.
                if (!stack.isEmpty()) owner.drop(stack, false);
            }
            left -= count;
        }
        account.cents -= total;
        info("§aKupiono " + quantity + " za " + dollars(total));
    }
    private void sell(ItemStack item, boolean all) {
        int available = 0;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(s, item) && catalog.sellable(s)) available += s.getCount();
        }
        int amount = all ? available : Math.min(1, available);
        if (amount == 0) { info("§cBrak zwyklych przedmiotow do sprzedazy"); return; }
        long revenue = catalog.sale(item) * amount;
        int left = amount;
        for (int i = 0; i < owner.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (!ItemStack.isSameItemSameComponents(s, item) || !catalog.sellable(s)) continue;
            int count = Math.min(left, s.getCount());
            s.shrink(count);
            left -= count;
        }
        account.cents = Math.min(1_000_000_000_000_000L, account.cents + revenue);
        info("§aSprzedano " + amount + " za " + dollars(revenue));
    }
    private String dollars(long cents) { return String.format(Locale.ROOT, "$%.2f", cents / 100d); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean canDragTo(Slot slot) { return slot.index >= 54; }
}
