package pl.deltacraft.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** 54-slot sell basket. Settled exactly once on close. */
public final class SellMenu extends ChestMenu {
    private final SimpleContainer basket;
    private final ServerPlayer owner;
    private final PlayerAccount account;
    private final ShopCatalog catalog;
    private boolean settled = false;

    public SellMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog) {
        this(id, inv, owner, account, catalog, new SimpleContainer(54));
    }
    private SellMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog, SimpleContainer basket) {
        super(MenuType.GENERIC_9x6, id, inv, basket, 6);
        this.basket = basket;
        this.owner = owner;
        this.account = account;
        this.catalog = catalog;
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (player != owner) return;
        // Disallow drag/drop/quick-move and other mass operations; normal clicks still work.
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.QUICK_CRAFT || input == ContainerInput.PICKUP_ALL) return;
        super.clicked(slot, button, input, player);
        long value = 0;
        for (int i = 0; i < basket.getContainerSize(); i++) {
            ItemStack stack = basket.getItem(i);
            if (catalog.sellable(stack)) value += catalog.sale(stack.getItem()) * stack.getCount();
        }
        // Value is calculated server-side; no chat messages.
    }
    @Override public void removed(Player player) {
        if (settled) return;
        settled = true;
        long revenue = 0;
        long xp = 0;
        for (int i = 0; i < basket.getContainerSize(); i++) {
            ItemStack stack = basket.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            if (!catalog.sellable(stack)) {
                // Invalid or custom stacks must never be destroyed.
                owner.getInventory().add(stack);
                continue;
            }
            revenue += catalog.sale(stack.getItem()) * stack.getCount();
            // Selling does not grant mining XP.
        }
        account.cents = Math.min(1_000_000_000_000_000L, account.cents + revenue);

        // Closing the basket is silent; use /bal to inspect the updated balance.
        // No super.removed(): vanilla chest removal would return already-sold contents.
    }
    private static String money(long cents) { return String.format(Locale.ROOT, "$%.2f", cents / 100d); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
