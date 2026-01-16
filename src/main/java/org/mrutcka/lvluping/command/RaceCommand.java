package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.data.Race;
import org.mrutcka.lvluping.handler.AttributeHandler;
import org.mrutcka.lvluping.network.S2CSyncTalents;

import java.util.Arrays;
import java.util.Collection;

public class RaceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("race")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Race race = PlayerLevels.getRace(player.getUUID());
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("§7Ваша текущая раса: §6" + race.label), false);
                    return 1;
                })
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        Race r = PlayerLevels.getRace(p.getUUID());
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§e" + p.getScoreboardName() + " §7имеет расу: §6" + r.label), false);
                                    }
                                    return targets.size();
                                })))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("raceId", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Race.values()).map(r -> r.id), builder))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            String raceId = StringArgumentType.getString(ctx, "raceId");
                                            Race race = Race.getById(raceId);

                                            if (race == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cРаса с ID '" + raceId + "' не найдена!"));
                                                return 0;
                                            }

                                            for (ServerPlayer p : targets) {
                                                PlayerLevels.setRace(p.getUUID(), race);
                                                AttributeHandler.applyStats(p, false);
                                                sync(p);

                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("§7Раса для §e" + p.getScoreboardName() + " §7изменена на §6" + race.label), true);
                                            }

                                            PlayerLevels.save(ctx.getSource().getServer());
                                            return targets.size();
                                        }))))
        );
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