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
public final class ThornBushClient {

    private static final Map<UUID, BushVisual> ACTIVE = new ConcurrentHashMap<>();
    private static final double TILE_STEP = 1.35;
    private static final int MAX_TILES_PER_VISUAL = 220;

    public static void show(UUID id, double x, double y, double z, float yRot, double radius, long untilGameTime) {
        ACTIVE.put(id, new BushVisual(x, y, z, yRot, radius, untilGameTime));
    }

    public static void hide(UUID id) {
        ACTIVE.remove(id);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t = mc.level.getGameTime();
        Iterator<Map.Entry<UUID, BushVisual>> it = ACTIVE.entrySet().iterator();
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
        ItemStack stack = new ItemStack(LvlupingItems.RANGER_KORNI_MODEL.get());
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        if (model == null) model = mc.getModelManager().getMissingModel();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;
        long time = mc.level.getGameTime();

        for (BushVisual v : ACTIVE.values()) {
            if (v.untilGameTime <= time) continue;

            double r = Math.max(0.5, v.radius);
            double r2 = r * r;
            int tiles = 0;
            int minIx = (int) Math.floor(-r / TILE_STEP);
            int maxIx = (int) Math.ceil(r / TILE_STEP);

            for (int ix = minIx; ix <= maxIx; ix++) {
                double dx = ix * TILE_STEP;
                for (int iz = minIx; iz <= maxIx; iz++) {
                    double dz = iz * TILE_STEP;
                    if (dx * dx + dz * dz > r2) continue;

                    tiles++;
                    if (tiles > MAX_TILES_PER_VISUAL) break;

                    float extraRot = (float) (((ix * 73428767L) ^ (iz * 912931L) ^ v.idHash) & 63) * (360f / 64f);
                    float scale = 0.82f + (float) ((((ix * 912931L) ^ (iz * 73428767L) ^ (v.idHash * 31L)) & 15) * (0.06f / 15f));

                    pose.pushPose();
                    pose.translate((v.x + dx) - camX, v.y - camY + 0.02, (v.z + dz) - camZ);
                    pose.mulPose(Axis.YP.rotationDegrees(-v.yRot + 180f + extraRot));
                    pose.scale(scale, scale, scale);
                    itemRenderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                    pose.popPose();
                }
                if (tiles > MAX_TILES_PER_VISUAL) break;
            }
        }

        buffer.endBatch();
    }

    private record BushVisual(double x, double y, double z, float yRot, double radius, long untilGameTime, int idHash) {
        BushVisual(double x, double y, double z, float yRot, double radius, long untilGameTime) {
            this(x, y, z, yRot, radius, untilGameTime, java.util.Objects.hash(x, y, z, yRot, radius, untilGameTime));
        }
    }
}
