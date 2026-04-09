package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.mrutcka.lvluping.LvlupingItems;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class HunterTrapClient {

    private static final Map<UUID, TrapVisual> ACTIVE = new ConcurrentHashMap<>();

    public static void show(UUID id, double x, double y, double z, float yRot, long untilGameTime) {
        ACTIVE.put(id, new TrapVisual(x, y, z, yRot, untilGameTime));
    }

    public static void hide(UUID id) {
        ACTIVE.remove(id);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t = mc.level.getGameTime();
        Iterator<Map.Entry<UUID, TrapVisual>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().untilGameTime <= t) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getItemRenderer() == null) return;

        ItemRenderer itemRenderer = mc.getItemRenderer();
        ItemStack stack = new ItemStack(LvlupingItems.HUNTER_TRAP_MODEL.get());
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        if (model == null) model = mc.getModelManager().getMissingModel();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;
        long time = mc.level.getGameTime();

        for (TrapVisual v : ACTIVE.values()) {
            if (v.untilGameTime <= time) continue;

            pose.pushPose();
            pose.translate(v.x - camX, v.y - camY + 0.06, v.z - camZ);
            pose.mulPose(Axis.YP.rotationDegrees(-v.yRot + 180f));
            pose.mulPose(Axis.ZP.rotationDegrees(90f));
            pose.scale(0.55f, 0.55f, 0.55f);
            itemRenderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
        }

        buffer.endBatch();
    }

    private record TrapVisual(double x, double y, double z, float yRot, long untilGameTime) {}
}
