package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.AttributeStat;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.handler.AttributeHandler;
import java.util.Collection;

public class StatCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lvlstat")
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        ctx.getSource().sendSuccess(() -> statReport(p), false);
                                    }
                                    return targets.size();
                                })))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stat", StringArgumentType.string())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                                                .executes(ctx -> updateStat(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        IntegerArgumentType.getInteger(ctx, "value"), false))))))
                .then(Commands.literal("add")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stat", StringArgumentType.string())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> updateStat(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        IntegerArgumentType.getInteger(ctx, "value"), true)))))));
    }

    private static Component statReport(ServerPlayer p) {
        StringBuilder sb = new StringBuilder("§e" + p.getScoreboardName() + "§7: ");
        for (AttributeStat s : AttributeStat.values()) {
            int base = PlayerLevels.getPlayerStatsMap(p.getUUID()).getOrDefault(s.id, 0);
            int total = PlayerLevels.getStatLevel(p.getUUID(), s.id);
            sb.append(String.format("§f%s §7=%d", s.label, total));
            if (total != base) sb.append(String.format(" §8(база %d)", base));
            sb.append("  ");
        }
        return Component.literal(sb.toString());
    }

    private static int updateStat(CommandSourceStack source, Collection<ServerPlayer> targets, String statKey, int value, boolean isAdd) {
        AttributeStat st = AttributeStat.getById(statKey);
        if (st == null) {
            source.sendFailure(Component.literal("§cНеизвестный стат: §f" + statKey + " §7(health, damage, speed, manacost)"));
            return 0;
        }
        for (ServerPlayer player : targets) {
            var map = PlayerLevels.getPlayerStatsMap(player.getUUID());
            int cur = map.getOrDefault(st.id, 0);
            int next = isAdd ? cur + value : value;
            next = Mth.clamp(next, 0, st.maxLevel);
            map.put(st.id, next);
            AttributeHandler.applyStats(player, false);
            sync(player);
            int fi = next;
            source.sendSuccess(() -> Component.literal("§7" + st.label + " §e" + player.getScoreboardName() + " §7→ §f" + fi + "§7/§f" + st.maxLevel), true);
        }
        PlayerLevels.save(source.getServer());
        return targets.size();
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, PlayerLevels.createSyncPayload(player));
    }
}
