package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.mrutcka.lvluping.LvlupingItems;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class RootsTargetClient {

    private static final Map<Integer, Long> ACTIVE = new ConcurrentHashMap<>();

    public static void show(int entityId, long untilGameTime) {
        ACTIVE.put(entityId, untilGameTime);
    }

    public static void hide(int entityId) {
        ACTIVE.remove(entityId);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t = mc.level.getGameTime();
        ACTIVE.entrySet().removeIf(e -> e.getValue() <= t);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getItemRenderer() == null) return;

        long time = mc.level.getGameTime();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        ItemStack stack = new ItemStack(LvlupingItems.RANGER_KORNI_MODEL.get());
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        if (model == null) model = mc.getModelManager().getMissingModel();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        for (Map.Entry<Integer, Long> en : ACTIVE.entrySet()) {
            if (en.getValue() <= time) continue;
            Entity entity = mc.level.getEntity(en.getKey());
            if (entity == null) continue;

            Vec3 p = entity.position();
            float yRot = entity.getYRot();

            pose.pushPose();
            pose.translate(p.x - camX, p.y - camY + 0.02, p.z - camZ);
            pose.mulPose(Axis.YP.rotationDegrees(-yRot + 180f));
            pose.scale(0.9f, 0.9f, 0.9f);
            itemRenderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
        }

        buffer.endBatch();
    }
}
