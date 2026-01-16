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

public class StarCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("star")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int stars = PlayerLevels.getStars(player.getUUID());
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("§7Ваш ранг: §e" + "★".repeat(stars) + " §7(" + stars + ")"), false);
                    return 1;
                })
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        int stars = PlayerLevels.getStars(p.getUUID());
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§e" + p.getScoreboardName() + " §7имеет ранг: §e" + "★".repeat(stars)), false);
                                    }
                                    return targets.size();
                                })))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 7))
                                        .executes(ctx -> updateStars(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), false)))))
                .then(Commands.literal("add")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-7, 7))
                                        .executes(ctx -> updateStars(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), true)))))
        );
    }

    private static int updateStars(CommandSourceStack source, Collection<ServerPlayer> targets, int value, boolean isAdd) {
        for (ServerPlayer player : targets) {
            int current = PlayerLevels.getStars(player.getUUID());
            int newValue = isAdd ? (current + value) : value;

            newValue = Math.max(1, Math.min(7, newValue));

            PlayerLevels.setStars(player.getUUID(), newValue);

            int currentLvl = PlayerLevels.getLevel(player);
            PlayerLevels.setLevel(player.getUUID(), currentLvl);

            sync(player);

            int finalStars = newValue;
            source.sendSuccess(() -> Component.literal("§7Ранг игрока §e" + player.getScoreboardName() +
                    " §7изменен на §e" + "★".repeat(finalStars) + " §8(" + finalStars + ")"), true);
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