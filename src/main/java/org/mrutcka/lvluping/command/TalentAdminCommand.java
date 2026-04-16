package org.mrutcka.lvluping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.data.Talent;
import org.mrutcka.lvluping.handler.AttributeHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class TalentAdminCommand {

    private static final SuggestionProvider<CommandSourceStack> TALENT_IDS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        List<String> ids = new ArrayList<>();
        for (Talent t : Talent.values()) {
            if (rem.isEmpty() || t.id.toLowerCase().startsWith(rem)) {
                ids.add(t.id);
            }
        }
        ids.sort(Comparator.naturalOrder());
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lvl_talent")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : targets) {
                                        var owned = PlayerLevels.getPlayerTalents(p.getUUID());
                                        List<String> sorted = new ArrayList<>(owned);
                                        sorted.sort(Comparator.naturalOrder());
                                        String list = sorted.isEmpty() ? "§8(нет)" : String.join("§7, §e", sorted);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§e" + p.getScoreboardName() + " §7таланты (" + sorted.size() + "): §e" + list), false);
                                    }
                                    return targets.size();
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests(TALENT_IDS)
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            String id = StringArgumentType.getString(ctx, "id");
                                            Talent t = Talent.getById(id);
                                            if (t == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cНеизвестный талант: §f" + id));
                                                return 0;
                                            }
                                            for (ServerPlayer p : targets) {
                                                PlayerLevels.adminGrantTalent(p.getUUID(), id);
                                                AttributeHandler.applyStats(p, false);
                                                PlayerLevels.syncTalentsToClient(p);
                                                String pid = p.getScoreboardName();
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§7Выдан талант §e" + id + " §7→ §e" + pid), true);
                                            }
                                            PlayerLevels.save(ctx.getSource().getServer());
                                            return targets.size();
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests(TALENT_IDS)
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            String id = StringArgumentType.getString(ctx, "id");
                                            if (Talent.getById(id) == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cНеизвестный талант: §f" + id));
                                                return 0;
                                            }
                                            int ok = 0;
                                            for (ServerPlayer p : targets) {
                                                if (PlayerLevels.adminRemoveTalent(p.getUUID(), id)) {
                                                    ok++;
                                                    AttributeHandler.applyStats(p, false);
                                                    PlayerLevels.syncTalentsToClient(p);
                                                    String pid = p.getScoreboardName();
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§7Снят талант §e" + id + " §7у §e" + pid), true);
                                                }
                                            }
                                            if (ok == 0) {
                                                ctx.getSource().sendFailure(Component.literal("§cНи у кого из целей нет таланта §f" + id));
                                                return 0;
                                            }
                                            PlayerLevels.save(ctx.getSource().getServer());
                                            return ok;
                                        })))));
    }
}
