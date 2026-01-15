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
import java.util.Collections;

public class LevelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lvl")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("Ваш уровень: §b" + PlayerLevels.getLevel(p)), false);
                    return 1;
                })
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.literal("Ваш уровень: §b" + PlayerLevels.getLevel(p)), false);
                            return 1;
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Уровень " + p.getScoreboardName() + ": §b" + PlayerLevels.getLevel(p)), false);
                                    }
                                    return targets.size();
                                })
                        )
                )
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
        );
    }

    private static int updateLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int value, boolean add) {
        for (ServerPlayer player : targets) {
            int current = PlayerLevels.getLevel(player);
            PlayerLevels.setLevel(player.getUUID(), add ? current + value : value);
            sync(player);
        }
        PlayerLevels.save(source.getServer());
        source.sendSuccess(() -> Component.literal("Уровни обновлены для " + targets.size() + " игр."), true);
        return targets.size();
    }

    private static void sync(ServerPlayer player) {
        ModNetworking.CHANNEL.send(new S2CSyncTalents(
                PlayerLevels.getLevel(player),
                PlayerLevels.getStars(player.getUUID()),
                PlayerLevels.getPlayerTalents(player.getUUID())
        ), PacketDistributor.PLAYER.with(player));
    }
}