package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.mrutcka.lvluping.data.StatTrainingConfig;

public final class StatTrainingCommand {

    private static final Map<UUID, BlockPos> SPEED_CORNER1 = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> HEALTH_CORNER1 = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var speedZoneCmd = Commands.literal("speed")
                .then(Commands.literal("pos1").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SPEED_CORNER1.put(p.getUUID(), p.blockPosition());
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Угол 1 скорости записан. Встаньте во 2-й угол и §e/lvluptrain zone speed pos2"), false);
                    return 1;
                }))
                .then(Commands.literal("pos2").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    BlockPos a = SPEED_CORNER1.remove(p.getUUID());
                    if (a == null) {
                        ctx.getSource().sendFailure(Component.literal("§cСначала выполните §epos1§c."));
                        return 0;
                    }
                    BlockPos b = p.blockPosition();
                    String dim = p.level().dimension().location().toString();
                    int minX = Math.min(a.getX(), b.getX());
                    int maxX = Math.max(a.getX(), b.getX());
                    int minZ = Math.min(a.getZ(), b.getZ());
                    int maxZ = Math.max(a.getZ(), b.getZ());
                    int minY = Math.min(a.getY(), b.getY());
                    int maxY = Math.max(a.getY(), b.getY());
                    StatTrainingConfig.speedZone = new StatTrainingConfig.ZoneBox(true, dim, minX, maxX, minZ, maxZ, minY, maxY);
                    try {
                        StatTrainingConfig.saveToFile(ctx.getSource().getServer());
                        StatTrainingConfig.load(ctx.getSource().getServer());
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal("§cОшибка сохранения: " + e.getMessage()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§aЗона тренировки скорости сохранена в stat_training.json"), true);
                    return 1;
                }));

        var healthZoneCmd = Commands.literal("healthpvp")
                .then(Commands.literal("pos1").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    HEALTH_CORNER1.put(p.getUUID(), p.blockPosition());
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Угол 1 PvP/здоровья записан. Затем §e/lvluptrain zone healthpvp pos2"), false);
                    return 1;
                }))
                .then(Commands.literal("pos2").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    BlockPos a = HEALTH_CORNER1.remove(p.getUUID());
                    if (a == null) {
                        ctx.getSource().sendFailure(Component.literal("§cСначала выполните §epos1§c."));
                        return 0;
                    }
                    BlockPos b = p.blockPosition();
                    String dim = p.level().dimension().location().toString();
                    int minX = Math.min(a.getX(), b.getX());
                    int maxX = Math.max(a.getX(), b.getX());
                    int minZ = Math.min(a.getZ(), b.getZ());
                    int maxZ = Math.max(a.getZ(), b.getZ());
                    int minY = Math.min(a.getY(), b.getY());
                    int maxY = Math.max(a.getY(), b.getY());
                    StatTrainingConfig.healthPvpZone = new StatTrainingConfig.ZoneBox(true, dim, minX, maxX, minZ, maxZ, minY, maxY);
                    try {
                        StatTrainingConfig.saveToFile(ctx.getSource().getServer());
                        StatTrainingConfig.load(ctx.getSource().getServer());
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal("§cОшибка сохранения: " + e.getMessage()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§aЗона тренировки здоровья (PvP) сохранена."), true);
                    return 1;
                }));

        dispatcher.register(Commands.literal("lvluptrain")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    StatTrainingConfig.load(server);
                    ctx.getSource().sendSuccess(() -> Component.literal("§astat_training.json перезагружен."), true);
                    return 1;
                }))
                .then(Commands.literal("zone").then(speedZoneCmd).then(healthZoneCmd)));
    }

    private StatTrainingCommand() {}
}
