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
import org.mrutcka.lvluping.network.S2CSyncTalents;

import java.util.Collection;

public class LevelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lvl")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int stars = PlayerLevels.getStars(p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Уровень: §b" + PlayerLevels.getLevel(p) + "§7/§3" + PlayerLevels.getMaxLevel(stars)), false);
                    return 1;
                })
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> updateLevel(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), false)))))
                .then(Commands.literal("add")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> updateLevel(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), true)))))
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    for(ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§e" + p.getScoreboardName() + " §7имеет уровень: §b" + PlayerLevels.getLevel(p)), false);
                                    }
                                    return 1;
                                })))
        );
    }

    private static int updateLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int value, boolean isAdd) {
        for (ServerPlayer player : targets) {
            int current = PlayerLevels.getLevel(player);
            int stars = PlayerLevels.getStars(player.getUUID());
            int maxAllowed = PlayerLevels.getMaxLevel(stars);

            int newValue = isAdd ? (current + value) : value;

            newValue = Math.max(0, Math.min(maxAllowed, newValue));

            PlayerLevels.setLevel(player.getUUID(), newValue);
            sync(player);

            int finalValue = newValue;
            source.sendSuccess(() -> Component.literal("§7Уровень игрока §e" + player.getScoreboardName() +
                    " §7изменен на §b" + finalValue + " §8(Макс: " + maxAllowed + ")"), true);
        }

        PlayerLevels.save(source.getServer());
        return targets.size();
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2CSyncTalents(
                PlayerLevels.getLevel(player),
                PlayerLevels.getStars(player.getUUID()),
                PlayerLevels.getPlayerTalents(player.getUUID()),
                PlayerLevels.getPlayerStatsMap(player.getUUID()),
                PlayerLevels.getRace(player.getUUID()).id
        ));
    }
}