package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = org.mrutcka.lvluping.LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class JudgementHammerClient {

    private static final int TOTAL_TICKS = 60;
    private static final int VISIBLE_TICKS = 20;
    private static final int FALL_TICKS = (int) 30;
    private static final double FALL_HEIGHT = 40.0;
    private static final int GROUND_TICKS = 80;
    private static final double GROUND_Y_OFFSET = 1.0;
    private static final List<HammerEffect> EFFECTS = new ArrayList<>();

    public static void addEffect(double targetX, double targetY, double targetZ, int ticksRemaining, UUID targetUuid) {
        EFFECTS.add(new HammerEffect(targetX, targetY, targetZ, Math.min(ticksRemaining, TOTAL_TICKS), targetUuid));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        List<HammerEffect> toRemove = new ArrayList<>();
        for (HammerEffect e : EFFECTS) {
            if (e.groundTicksRemaining > 0) {
                e.groundTicksRemaining--;
                if (e.groundTicksRemaining <= 0) toRemove.add(e);
                continue;
            }
            e.ticksRemaining--;
            if (e.ticksRemaining <= 0) {
                if (level != null) {
                    e.enterGroundPhase(level);
                } else {
                    toRemove.add(e);
                }
                continue;
            }
            if (level != null && e.ticksRemaining <= VISIBLE_TICKS && e.ticksRemaining > 0) {
                double tx = e.getTargetX(level), ty = e.getTargetY(level), tz = e.getTargetZ(level);
                double progress = Math.min(1.0, (double) (VISIBLE_TICKS - e.ticksRemaining) / FALL_TICKS);
                double y = ty + FALL_HEIGHT * (1.0 - progress);
                spawnTrailParticles(level, tx, y, tz);
            }
        }
        EFFECTS.removeAll(toRemove);
    }

    private static void spawnTrailParticles(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, -0.05, 0);
        level.addParticle(ParticleTypes.CRIT, x, y, z, 0.1, -0.1, 0.1);
        level.addParticle(ParticleTypes.CRIT, x, y, z, -0.1, -0.1, -0.1);
        level.addParticle(ParticleTypes.ENCHANT, x, y + 0.5, z, 0, 0, 0);
        if (level.random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.02, -0.02, 0.02);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (EFFECTS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getItemRenderer() == null) return;

        ItemRenderer itemRenderer = mc.getItemRenderer();
        ItemStack stack = new ItemStack(LvlupingItems.JUDGEMENT_HAMMER.get());
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        if (model == null) model = mc.getModelManager().getMissingModel();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        for (HammerEffect e : EFFECTS) {
            double x, y, z;
            if (e.groundTicksRemaining > 0) {
                x = e.groundX;
                y = e.groundY + GROUND_Y_OFFSET;
                z = e.groundZ;
            } else {
                if (e.ticksRemaining > VISIBLE_TICKS) continue;
                double tx = e.getTargetX(mc.level), ty = e.getTargetY(mc.level), tz = e.getTargetZ(mc.level);
                double progress = Math.min(1.0, (double) (VISIBLE_TICKS - e.ticksRemaining) / FALL_TICKS);
                double fallY = ty + FALL_HEIGHT * (1.0 - progress);
                y = (progress >= 1.0) ? ty + GROUND_Y_OFFSET : fallY;
                x = tx; z = tz;
            }

            pose.pushPose();
            pose.translate(x - camX, y - camY, z - camZ);
            pose.mulPose(Axis.XP.rotationDegrees(180f));
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            pose.scale(2f, 2f, 2f);
            itemRenderer.render(stack, ItemDisplayContext.FIXED, false, pose, buffer, 0xF000F0, LightTexture.FULL_BRIGHT, model);
            pose.popPose();
        }

        buffer.endBatch();
    }

    private static final class HammerEffect {
        final double fallbackX, fallbackY, fallbackZ;
        final UUID targetUuid;
        int ticksRemaining;
        double groundX, groundY, groundZ;
        int groundTicksRemaining;

        HammerEffect(double fallbackX, double fallbackY, double fallbackZ, int ticksRemaining, UUID targetUuid) {
            this.fallbackX = fallbackX; this.fallbackY = fallbackY; this.fallbackZ = fallbackZ;
            this.ticksRemaining = ticksRemaining;
            this.targetUuid = targetUuid;
            this.groundTicksRemaining = -1;
        }

        void enterGroundPhase(Level level) {
            groundX = getTargetX(level);
            groundY = getTargetY(level);
            groundZ = getTargetZ(level);
            groundTicksRemaining = GROUND_TICKS;
        }

        double getTargetX(Level level) {
            if (level == null || targetUuid == null) return fallbackX;
            Entity entity = findEntityByUuid(level, targetUuid, fallbackX, fallbackZ);
            return entity != null ? entity.getX() : fallbackX;
        }
        double getTargetY(Level level) {
            if (level == null || targetUuid == null) return fallbackY;
            Entity entity = findEntityByUuid(level, targetUuid, fallbackX, fallbackZ);
            return entity != null ? entity.getY() : fallbackY;
        }
        double getTargetZ(Level level) {
            if (level == null || targetUuid == null) return fallbackZ;
            Entity entity = findEntityByUuid(level, targetUuid, fallbackX, fallbackZ);
            return entity != null ? entity.getZ() : fallbackZ;
        }
    }

    private static Entity findEntityByUuid(Level level, UUID uuid, double centerX, double centerZ) {
        AABB search = new AABB(centerX - 64, -64, centerZ - 64, centerX + 64, 320, centerZ + 64);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, search)) {
            if (entity.getUUID().equals(uuid)) return entity;
        }
        return null;
    }
}
