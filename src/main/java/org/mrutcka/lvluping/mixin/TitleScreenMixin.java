package org.mrutcka.lvluping.mixin;

import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.mrutcka.lvluping.client.ModSplashTexts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Shadow
    @Mutable
    @Final
    private SplashRenderer splash;

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    private void lvluping$replaceVanillaSplash(boolean fading, LogoRenderer logoRenderer, CallbackInfo ci) {
        this.splash = new SplashRenderer(ModSplashTexts.pickRandom());
    }
}
