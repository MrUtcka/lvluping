package org.mrutcka.lvluping.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.Collection;

public class LevelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("lvl")
                        .executes(ctx -> {
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int level = PlayerLevels.getLevel(player);

                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("Your level is " + level),
                                        false
                                );
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("This command can only be used by a player."));
                            }
                            return 1;
                        })
                        .then(
                                Commands.literal("add")
                                        .requires(cs -> cs.hasPermission(2))
                                        .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                        .then(
                                                                Commands.argument("amount", IntegerArgumentType.integer())
                                                                        .executes(ctx -> {
                                                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                                            for (ServerPlayer player : players) {
                                                                                for (int i = 0; i < amount; i++) {
                                                                                    PlayerLevels.addLevel(player);
                                                                                }
                                                                                ctx.getSource().sendSuccess(
                                                                                        () -> Component.literal(
                                                                                                "Added " + amount + " level(s) to " + player.getName().getString()
                                                                                        ), true
                                                                                );
                                                                            }
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("set")
                                        .requires(cs -> cs.hasPermission(2))
                                        .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                        .then(
                                                                Commands.argument("level", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> {
                                                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                                                            int level = IntegerArgumentType.getInteger(ctx, "level");

                                                                            for (ServerPlayer player : players) {
                                                                                PlayerLevels.setLevel(player, level);
                                                                                ctx.getSource().sendSuccess(
                                                                                        () -> Component.literal(
                                                                                                "Set level of " + player.getName().getString() + " to " + level
                                                                                        ), true
                                                                                );
                                                                            }
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("get")
                                        .requires(cs -> cs.hasPermission(2))
                                        .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                        .executes(ctx -> {
                                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");

                                                            for (ServerPlayer player : players) {
                                                                int level = PlayerLevels.getLevel(player);
                                                                ctx.getSource().sendSuccess(
                                                                        () -> Component.literal(
                                                                                player.getName().getString() + " is level " + level
                                                                        ), true
                                                                );
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
