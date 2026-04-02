package org.mrutcka.lvluping.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class IllusionPlayerRender {
    private static final String NAME_PREFIX = "lvluping_illusion:";
    private static final String CLONE_PREFIX = "lvluping_clone:";
    private static final Map<UUID, RemotePlayer> FAKE_PLAYERS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity le && isIllusionEntity(le)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level) || mc.player == null) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;
        float pt = event.getPartialTick().getGameTimeDeltaTicks();

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(96, 48, 96))) {
            if (!isIllusionEntity(e)) continue;
            UUID ownerUuid = ownerUuidFromName(e);
            if (ownerUuid == null) continue;

            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(ownerUuid) : null;
            if (info == null) continue;
            String name = info.getProfile() != null ? info.getProfile().getName() : ownerUuid.toString();
            GameProfile profile = new GameProfile(ownerUuid, name);
            RemotePlayer fake = FAKE_PLAYERS.computeIfAbsent(ownerUuid, id -> new RemotePlayer(level, profile));
            fake.tickCount = e.tickCount;
            fake.setPos(e.getX(), e.getY(), e.getZ());
            fake.yBodyRot = e.getYRot();
            fake.setYRot(e.getYRot());
            fake.setXRot(e.getXRot());
            fake.yHeadRot = e.getYRot();
            fake.setInvisible(false);

            if (!(dispatcher.getRenderer(fake) instanceof PlayerRenderer playerRenderer)) continue;
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(e.getX(), e.getY() + 1.0, e.getZ()));
            pose.pushPose();
            pose.translate(e.getX() - camX, e.getY() - camY, e.getZ() - camZ);
            playerRenderer.render((AbstractClientPlayer) fake, fake.getYRot(), pt, pose, buffer, light);
            pose.popPose();
        }
        buffer.endBatch();
    }

    private static boolean isIllusionEntity(LivingEntity e) {
        return ownerUuidFromName(e) != null;
    }

    private static UUID ownerUuidFromName(LivingEntity e) {
        if (e.getCustomName() == null) return null;
        String raw = e.getCustomName().getString();
        String prefix = raw.startsWith(NAME_PREFIX) ? NAME_PREFIX : (raw.startsWith(CLONE_PREFIX) ? CLONE_PREFIX : null);
        if (prefix == null) return null;
        try {
            return UUID.fromString(raw.substring(prefix.length()));
        } catch (Exception ignored) {
            return null;
        }
    }
}
