package pl.deltacraft.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Path;

public final class DeltaCraftShop implements ModInitializer {
    private static Accounts accounts;
    private static ShopCatalog catalog;
    private static Treasury treasury;
    private static int saveTicks;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("deltacraft-shop");
            accounts = new Accounts(path.resolve("accounts"));
            catalog = new ShopCatalog(path.resolve("prices.properties"));
            treasury = new Treasury(path.resolve("treasury.properties"));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (accounts != null) accounts.saveAll(); if (treasury != null) treasury.save(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            saveTicks++;
            if (saveTicks % 50 == 0 && accounts != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PlayerAccount account = accounts.get(player.getUUID());
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.literal("§a✦ DeltaCraft ✦ §8| §eSaldo: §f" + MoneyFormat.format(account.cents)
                            + " §8| §b⛏ LVL §f" + account.level)));
                }
            }
            if (saveTicks >= 1200) { saveTicks = 0; if (accounts != null) accounts.saveAll(); if (treasury != null) treasury.save(); }
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (accounts == null || !(player instanceof ServerPlayer sp) || sp.isCreative() || sp.isSpectator()) return;
            PlayerAccount a = accounts.get(sp.getUUID());
            int before = a.level;
            a.addXp(MultiMine.miningXp(state.getBlock()));
            if (a.level > before) sp.sendSystemMessage(Component.literal("§d[MultiMine] §aNowy poziom: " + a.level));
            if (state.is(Blocks.STONE)) a.earnBaseDollars(1);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (accounts == null || !(damageSource.getEntity() instanceof ServerPlayer killer)) return;
            if (entity instanceof ServerPlayer) return;
            long dollars = mobReward(entity);
            if (dollars > 0) accounts.get(killer.getUUID()).earnBaseDollars(dollars);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }
    private static long mobReward(LivingEntity e) {
        EntityType<?> type = e.getType();
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (id.equals("minecraft:zombie") || id.equals("minecraft:skeleton") || id.equals("minecraft:spider")) return 10;
        if (id.equals("minecraft:creeper")) return 15;
        if (e instanceof net.minecraft.world.entity.monster.Monster) return 30;
        if (e instanceof net.minecraft.world.entity.animal.Animal) return 20;
        return 0;
    }
    private static void registerCommands(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sklep").executes(ctx -> openShop(ctx.getSource())));
        d.register(Commands.literal("shop").executes(ctx -> openShop(ctx.getSource())));
        d.register(Commands.literal("bal").executes(ctx -> showBalance(ctx.getSource()))
            .then(Commands.argument("gracz", StringArgumentType.word()).executes(ctx -> showOtherBalance(ctx.getSource(), StringArgumentType.getString(ctx, "gracz")))));
        d.register(Commands.literal("money").executes(ctx -> showBalance(ctx.getSource())));
        d.register(Commands.literal("multimine").executes(ctx -> showMultiMine(ctx.getSource())));
        d.register(Commands.literal("mm").executes(ctx -> showMultiMine(ctx.getSource())));
        d.register(Commands.literal("sell").executes(ctx -> openSell(ctx.getSource())));
        d.register(Commands.literal("skarbiec").requires(source -> source.permissions().hasPermission(
                                                                        net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN
                                                                    )
                                                                 )
        
        d.register(Commands.literal("pay")
            .then(Commands.argument("gracz", StringArgumentType.word())
            .then(Commands.argument("kwota", DoubleArgumentType.doubleArg(0.01))
            .executes(ctx -> pay(ctx.getSource(), StringArgumentType.getString(ctx, "gracz"), DoubleArgumentType.getDouble(ctx, "kwota"))))));
    }
    private static int openShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (accounts == null || catalog == null) return 0;
        PlayerAccount a = accounts.get(player.getUUID());
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new ShopMenu(id, inv, player, a, catalog, treasury), Component.literal("DeltaCraft Shop")));
        return 1;
    }
    private static int openSell(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (accounts == null || catalog == null) return 0;
        PlayerAccount a = accounts.get(player.getUUID());
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new SellMenu(id, inv, player, a, catalog, treasury), Component.literal("DeltaCraft - /sell (54 sloty)")));
        return 1;
    }
    public static boolean isTreasuryAdmin(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(2);
    }
    private static int openTreasury(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!isTreasuryAdmin(player) || treasury == null || accounts == null) return 0;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (id, inv, p) -> new TreasuryMenu(id, inv, player, accounts.get(player.getUUID()), accounts, treasury),
            Component.literal("DeltaCraft - Skarbiec")));
        return 1;
    }
    private static int pay(CommandSourceStack source, String name, double amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        if (accounts == null || treasury == null || !Double.isFinite(amount) || amount > 1_000_000_000_000d) return 0;
        ServerPlayer recipient = source.getServer().getPlayerList().getPlayerByName(name);
        if (recipient == null || recipient.getUUID().equals(sender.getUUID())) {
            sender.displayClientMessage(Component.literal("§cGracz musi byc online i nie mozesz wyslac do siebie."), true); return 0;
        }
        long cents = Math.round(amount * 100d);
        if (cents <= 0) return 0;
        PlayerAccount from = accounts.get(sender.getUUID());
        PlayerAccount to = accounts.get(recipient.getUUID());
        long tax = Treasury.purchaseTax(cents);
        if (from.cents < cents + tax || to.cents > Treasury.LIMIT - cents || !treasury.canDeposit(tax)) {
            sender.displayClientMessage(Component.literal("§cBrak srodkow lub limit konta odbiorcy."), true); return 0;
        }
        from.cents -= cents + tax;
        to.cents += cents;
        treasury.deposit(tax);
        accounts.saveAll();
        String value = String.format(java.util.Locale.ROOT, "$%.2f", cents / 100d);
        sender.sendSystemMessage(Component.literal("§a[DeltaCraft] Przelano " + value + " do " + recipient.getName().getString() + " §7(podatek: " + MoneyFormat.format(tax) + ")"));
        recipient.sendSystemMessage(Component.literal("§a[DeltaCraft] Otrzymano " + value + " od " + sender.getName().getString()));
        return 1;
    }
    private static int showOtherBalance(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(name);
        if (target == null || accounts == null) return 0;
        player.sendSystemMessage(Component.literal("§a[DeltaCraft] " + name + ": " + MoneyFormat.format(accounts.get(target.getUUID()).cents)));
        return 1;
    }
    private static int showBalance(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (accounts == null) return 0;
        player.sendSystemMessage(Component.literal("§a[DeltaCraft] §fSaldo: §e" + MoneyFormat.format(accounts.get(player.getUUID()).cents)));
        return 1;
    }
    private static int showMultiMine(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (accounts == null) return 0;
        PlayerAccount a = accounts.get(player.getUUID());
        player.sendSystemMessage(Component.literal("§a[MultiMine] §fPoziom: §e" + a.level
                + " §7| §fXP: §e" + a.xp + "/" + MultiMine.requiredXp(a.level)
                + " §7| §fZarobki: §a×" + String.format(java.util.Locale.ROOT, "%.2f", MultiMine.earningsMultiplier(a.level))));
        return 1;
    }
}
