package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.ModNetworking;
import org.mrutcka.lvluping.network.S2CSyncTalents;

import java.util.Collection;

public class StarCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("star")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int stars = PlayerLevels.getStars(p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("Ваши звезды: §e" + "★".repeat(stars)), false);
                    return 1;
                })
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int stars = PlayerLevels.getStars(p.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("Ваши звезды: §e" + "★".repeat(stars)), false);
                            return 1;
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        int stars = PlayerLevels.getStars(p.getUUID());
                                        ctx.getSource().sendSuccess(() -> Component.literal("Звезды " + p.getScoreboardName() + ": §e" + "★".repeat(stars)), false);
                                    }
                                    return targets.size();
                                })
                        )
                )
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 7))
                                        .executes(ctx -> updateStars(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), false)))))
                .then(Commands.literal("add")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> updateStars(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "value"), true)))))
        );
    }

    private static int updateStars(CommandSourceStack source, Collection<ServerPlayer> targets, int value, boolean add) {
        for (ServerPlayer player : targets) {
            int current = PlayerLevels.getStars(player.getUUID());
            PlayerLevels.setStars(player.getUUID(), Math.min(7, add ? current + value : value));
            sync(player);
        }
        PlayerLevels.save(source.getServer());

        return targets.size();
    }

    private static void sync(ServerPlayer player) {
        ModNetworking.CHANNEL.send(new S2CSyncTalents(
                PlayerLevels.getLevel(player),
                PlayerLevels.getStars(player.getUUID()),
                PlayerLevels.getPlayerTalents(player.getUUID()),
                PlayerLevels.getPlayerStatsMap(player.getUUID())
        ), PacketDistributor.PLAYER.with(player));
    }
}