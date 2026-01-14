package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.ModNetworking;
import org.mrutcka.lvluping.network.S2CSyncTalents;

import java.util.Collection;

public class LevelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("lvl")
                        .executes(ctx -> {
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int level = PlayerLevels.getLevel(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Ваш уровень: " + level), false);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("Эту команду может использовать только игрок."));
                            }
                            return 1;
                        })
                        .then(Commands.literal("add")
                                .requires(cs -> cs.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                    for (ServerPlayer player : players) {
                                                        for (int i = 0; i < amount; i++) {
                                                            PlayerLevels.addLevel(player);
                                                        }
                                                        syncToClient(player);

                                                        ctx.getSource().sendSuccess(() -> Component.literal("Добавлено " + amount + " ур. игроку " + player.getName().getString()), true);
                                                    }
                                                    return 1;
                                                }))))
                        .then(Commands.literal("set")
                                .requires(cs -> cs.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                                    int level = IntegerArgumentType.getInteger(ctx, "level");

                                                    for (ServerPlayer player : players) {
                                                        PlayerLevels.setLevel(player, level);
                                                        syncToClient(player);

                                                        ctx.getSource().sendSuccess(() -> Component.literal("Установлен уровень " + level + " для " + player.getName().getString()), true);
                                                    }
                                                    return 1;
                                                }))))
                        .then(Commands.literal("get")
                                .requires(cs -> cs.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            for (ServerPlayer player : players) {
                                                int level = PlayerLevels.getLevel(player);
                                                ctx.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " имеет " + level + " уровень"), true);
                                            }
                                            return 1;
                                        })))
        );
    }

    private static void syncToClient(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                new S2CSyncTalents(
                        PlayerLevels.getLevel(player),
                        PlayerLevels.getPlayerTalents(player.getUUID())
                ),
                PacketDistributor.PLAYER.with(player)
        );
    }
}