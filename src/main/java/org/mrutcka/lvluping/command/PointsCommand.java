package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.Collection;

public final class PointsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lvlpoint")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int bonus = PlayerLevels.getBonusPoints(p.getUUID());
                    int avail = PlayerLevels.getAvailableUpgradePoints(p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§7Свободных очков: §b" + avail + " §8|§7 бонус: §e" + bonus), false);
                    return 1;
                })
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
                                        int bonus = PlayerLevels.getBonusPoints(p.getUUID());
                                        int avail = PlayerLevels.getAvailableUpgradePoints(p.getUUID());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§e" + p.getScoreboardName() + " §7— свободно §b" + avail + " §8|§7 бонус §e" + bonus), false);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> updateBonus(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "value"), false)))))
                .then(Commands.literal("add")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-1_000_000, 1_000_000))
                                        .executes(ctx -> updateBonus(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "value"), true)))))
        );
    }

    private static int updateBonus(CommandSourceStack source, Collection<ServerPlayer> targets, int value, boolean isAdd) {
        for (ServerPlayer player : targets) {
            int current = PlayerLevels.getBonusPoints(player.getUUID());
            int next = isAdd ? current + value : value;
            PlayerLevels.setBonusPoints(player.getUUID(), next);
            PacketDistributor.sendToPlayer(player, PlayerLevels.createSyncPayload(player));
            int finalBonus = PlayerLevels.getBonusPoints(player.getUUID());
            int finalAvail = PlayerLevels.getAvailableUpgradePoints(player.getUUID());
            source.sendSuccess(() -> Component.literal("§7Бонус §e" + player.getScoreboardName() + " §7→ §e" + finalBonus
                    + " §8(§7свободно §b" + finalAvail + "§8)"), true);
        }
        PlayerLevels.save(source.getServer());
        return targets.size();
    }

    private PointsCommand() {}
}
