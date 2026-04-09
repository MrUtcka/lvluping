package org.mrutcka.lvluping.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import org.mrutcka.lvluping.util.MobDeathMessageHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMessageMixin {

    private static final ThreadLocal<DamageSource> LVLUPING_DEATH_SOURCE = new ThreadLocal<>();

    @Inject(method = "die", at = @At("HEAD"))
    private void lvluping$captureDeathSource(DamageSource source, CallbackInfo ci) {
        LVLUPING_DEATH_SOURCE.set(source);
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void lvluping$clearDeathSource(CallbackInfo ci) {
        LVLUPING_DEATH_SOURCE.remove();
    }

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component lvluping$customDeathText(CombatTracker tracker) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        DamageSource damageSource = LVLUPING_DEATH_SOURCE.get();
        Component vanilla = tracker.getDeathMessage();
        if (damageSource == null
                || self.isFakePlayer()
                || !MobDeathMessageHelper.isDeathFromMob(damageSource)) {
            return vanilla;
        }
        return MobDeathMessageHelper.expeditionDeath(self);
    }

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void lvluping$broadcastDeathExcludingSelf(PlayerList playerList, Component message, boolean overlay) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        for (ServerPlayer player : playerList.getPlayers()) {
            if (player != self) {
                player.sendSystemMessage(message, overlay);
            }
        }
    }
}
