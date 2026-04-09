package org.mrutcka.lvluping.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.entity.AssassinBarricadeEntity;

public class AssassinBarricadeEntityRenderer extends EntityRenderer<AssassinBarricadeEntity> {
    public AssassinBarricadeEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(AssassinBarricadeEntity entity) {
        return ResourceLocation.withDefaultNamespace("missingno");
    }
}
