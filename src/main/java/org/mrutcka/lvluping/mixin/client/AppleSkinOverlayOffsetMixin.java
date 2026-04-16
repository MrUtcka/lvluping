package org.mrutcka.lvluping.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import org.mrutcka.lvluping.client.HudLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(targets = "squeek.appleskin.client.HUDOverlayHandler$Overlay")
public abstract class AppleSkinOverlayOffsetMixin {

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"))
    private int lvluping$shiftAppleSkinOverlayBaseY(GuiGraphics graphics) {
        return graphics.guiHeight() + (int) HudLayout.VANILLA_STATUS_STACK_LIFT_PX;
    }
}
