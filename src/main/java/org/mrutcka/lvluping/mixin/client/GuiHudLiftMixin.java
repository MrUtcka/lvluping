package org.mrutcka.lvluping.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.mrutcka.lvluping.client.HudLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Gui.class)
public abstract class GuiHudLiftMixin {

    @Inject(method = "renderHealthLevel", at = @At("HEAD"))
    private void lvluping$healthLevelPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, HudLayout.VANILLA_STATUS_STACK_LIFT_PX, 0f);
    }

    @Inject(method = "renderHealthLevel", at = @At("RETURN"))
    private void lvluping$healthLevelPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderArmorLevel", at = @At("HEAD"))
    private void lvluping$armorLevelPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, HudLayout.VANILLA_STATUS_STACK_LIFT_PX, 0f);
    }

    @Inject(method = "renderArmorLevel", at = @At("RETURN"))
    private void lvluping$armorLevelPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderFoodLevel", at = @At("HEAD"))
    private void lvluping$foodLevelPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, HudLayout.VANILLA_STATUS_STACK_LIFT_PX, 0f);
    }

    @Inject(method = "renderFoodLevel", at = @At("RETURN"))
    private void lvluping$foodLevelPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderAirLevel", at = @At("HEAD"))
    private void lvluping$airLevelPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, HudLayout.VANILLA_STATUS_STACK_LIFT_PX, 0f);
    }

    @Inject(method = "renderAirLevel", at = @At("RETURN"))
    private void lvluping$airLevelPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"))
    private void lvluping$vehicleHealthPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, HudLayout.VANILLA_STATUS_STACK_LIFT_PX, 0f);
    }

    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    private void lvluping$vehicleHealthPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }
}
