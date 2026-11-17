package pl.deltacraft.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Read-only 9-slot admin treasury GUI. */
public final class TreasuryMenu extends ChestMenu {
    private final ServerPlayer owner;
    private final PlayerAccount account;
    private final Accounts accounts;
    private final Treasury treasury;
    private final SimpleContainer icons;

    public TreasuryMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, Accounts accounts, Treasury treasury) {
        this(id, inv, owner, account, accounts, treasury, new SimpleContainer(9));
    }
    private TreasuryMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, Accounts accounts, Treasury treasury, SimpleContainer icons) {
        super(MenuType.GENERIC_9x1, id, inv, icons, 1);
        this.owner = owner; this.account = account; this.accounts = accounts; this.treasury = treasury; this.icons = icons;
        for (int i = 0; i < 9; i++) {
            final int slot = i;
            this.slots.set(i, new Slot(icons, slot, 8 + slot * 18, 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        render();
    }
    private void icon(int slot, Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        icons.setItem(slot, stack);
    }
    private void render() {
        icon(2, Items.CHEST, "§6Skarbiec: §e" + MoneyFormat.format(treasury.balance()));
        icon(6, Items.EMERALD_BLOCK, "§aWyplac wszystko na swoje konto");
        broadcastChanges();
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (player != owner) return;
        if (slot >= 0 && slot < 9) {
            if (input != ContainerInput.PICKUP || button != 0 || slot != 6) return;
            // Permission checked again on click, not just when opening.
            if (!DeltaCraftShop.isTreasuryAdmin(owner)) { owner.closeContainer(); return; }
            long amount = treasury.balance();
            if (amount == 0) { owner.sendSystemMessage(Component.literal("§e[DeltaCraft] Skarbiec jest pusty.")); return; }
            if (account.cents > Treasury.LIMIT - amount) {
                owner.sendSystemMessage(Component.literal("§c[DeltaCraft] Za malo miejsca na koncie na wyplate.")); return;
            }
            treasury.withdrawTo(account);
            accounts.saveAll();
            owner.sendSystemMessage(Component.literal("§a[DeltaCraft] Wyplacono ze skarbca: §e" + MoneyFormat.format(amount)));
            render();
            return;
        }
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.QUICK_CRAFT || input == ContainerInput.PICKUP_ALL) return;
        super.clicked(slot, button, input, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean canDragTo(Slot slot) { return slot.index >= 9; }
}
