package pl.deltacraft.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** 54-slot sell basket. Shift+left-click transfers stacks in both directions. */
public final class SellMenu extends ChestMenu {
    private final SimpleContainer basket;
    private final ServerPlayer owner;
    private final PlayerAccount account;
    private final ShopCatalog catalog;
    private final Treasury treasury;
    private boolean settled = false;

    public SellMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog, Treasury treasury) {
        this(id, inv, owner, account, catalog, treasury, new SimpleContainer(54));
    }
    private SellMenu(int id, Inventory inv, ServerPlayer owner, PlayerAccount account, ShopCatalog catalog, Treasury treasury, SimpleContainer basket) {
        super(MenuType.GENERIC_9x6, id, inv, basket, 6);
        this.basket = basket;
        this.owner = owner;
        this.account = account;
        this.catalog = catalog;
        this.treasury = treasury;
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (player != owner) return;
        if (input == ContainerInput.QUICK_MOVE) {
            if (button == 0) quickMoveStack(player, slot);
            updatePreview();
            return;
        }
        if (input == ContainerInput.QUICK_CRAFT || input == ContainerInput.PICKUP_ALL) return;
        super.clicked(slot, button, input, player);
        updatePreview();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (player != owner || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack original = source.getItem();
        if (index >= 54 && !catalog.sellable(original)) return ItemStack.EMPTY;
        ItemStack moving = original.copy();
        ItemStack snapshot = original.copy();
        boolean moved = index < 54
            ? moveItemStackTo(moving, 54, slots.size(), true)
            : moveItemStackTo(moving, 0, 54, false);
        if (!moved) return ItemStack.EMPTY;
        if (moving.isEmpty()) source.set(ItemStack.EMPTY);
        else source.set(moving);
        source.setChanged();
        broadcastChanges();
        return snapshot;
    }
    private void updatePreview() {
        long value = 0;
        for (int i = 0; i < basket.getContainerSize(); i++) {
            ItemStack stack = basket.getItem(i);
            if (catalog.sellable(stack)) value += catalog.sale(stack) * stack.getCount();
        }
        owner.connection.send(
            new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                Component.literal(message)
            )
        );
    }
    @Override public void removed(Player player) {
        if (settled) return;
        settled = true;
        long revenue = 0;
        long full = 0;
        for (int i = 0; i < basket.getContainerSize(); i++) {
            ItemStack stack = basket.getItem(i);
            if (catalog.sellable(stack)) {
                revenue += catalog.sale(stack) * stack.getCount();
                full += catalog.price(stack) * stack.getCount();
            }
        }
        long tax = Treasury.saleTax(revenue);
        long reserve = Treasury.saleReserve(full);
        long gain = tax + reserve;
        boolean valid = treasury.canDeposit(gain) && account.cents <= Treasury.LIMIT - (revenue - tax);
        if (valid) {
            account.cents += revenue - tax;
            treasury.deposit(gain);
        }
        for (int i = 0; i < basket.getContainerSize(); i++) {
            ItemStack stack = basket.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            if (!valid || !catalog.sellable(stack)) {
                // Use vanilla inventory insertion; return any remainder to the player.
                owner.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.literal(
                            valid
                                ? "§a[DeltaCraft] Sprzedano za "
                                    + MoneyFormat.format(revenue - tax)
                                    + " §7(podatek: "
                                    + MoneyFormat.format(tax) + ")"
                                : "§c[DeltaCraft] Nie udalo sie sprzedac przedmiotow."
                        )
                    )
                );
    }

}
