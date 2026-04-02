package org.mrutcka.lvluping.client;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.mrutcka.lvluping.LvlupingItems;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class UnbreakableShieldClient {
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final int SHIELDS = 4;
    private static final double RADIUS = 1.2;
    private static final double Y_OFFSET = 1.1;

    public static void addEffect(UUID targetUuid, long untilGameTime) {
        ACTIVE.put(targetUuid, untilGameTime);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ItemRenderer itemRenderer = mc.getItemRenderer();
        ItemStack stack = new ItemStack(LvlupingItems.UNBREAKABLE_SHIELD_ORBIT.get());
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        if (model == null) model = mc.getModelManager().getMissingModel();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;
        long time = mc.level.getGameTime();
        float partial = event.getPartialTick().getGameTimeDeltaTicks();

        Iterator<Map.Entry<UUID, Long>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() <= time) {
                it.remove();
                continue;
            }
            Entity target = findEntityByUuid(mc.level, e.getKey(), mc.player.getX(), mc.player.getZ());
            if (target == null) continue;

            double cx = target.getX();
            double cy = target.getY() + Y_OFFSET;
            double cz = target.getZ();
            float baseDeg = (time + partial) * 6.0f;
            for (int i = 0; i < SHIELDS; i++) {
                float a = baseDeg + i * (360.0f / SHIELDS);
                double rad = Math.toRadians(a);
                double x = cx + Math.cos(rad) * RADIUS;
                double z = cz + Math.sin(rad) * RADIUS;

                pose.pushPose();
                pose.translate(x - camX, cy - camY, z - camZ);
                pose.mulPose(Axis.YP.rotationDegrees(-a + 90.0f));
                pose.mulPose(Axis.XP.rotationDegrees(180.0f));
                pose.scale(0.3f, 0.3f, 0.3f);
                itemRenderer.render(stack, ItemDisplayContext.FIXED, false, pose, buffer, 0xF000F0, LightTexture.FULL_BRIGHT, model);
                pose.popPose();
            }
        }
        buffer.endBatch();
    }

    private static Entity findEntityByUuid(Level level, UUID uuid, double centerX, double centerZ) {
        AABB search = new AABB(centerX - 128, -64, centerZ - 128, centerX + 128, 320, centerZ + 128);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, search)) {
            if (entity.getUUID().equals(uuid)) return entity;
        }
        return null;
    }
}
