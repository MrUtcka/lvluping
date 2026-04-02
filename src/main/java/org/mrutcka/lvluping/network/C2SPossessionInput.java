package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record C2SPossessionInput(float forward, float strafe, boolean jump, boolean attack, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<C2SPossessionInput> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "possess_input"));

    public static final StreamCodec<FriendlyByteBuf, C2SPossessionInput> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeFloat(msg.forward());
                buf.writeFloat(msg.strafe());
                buf.writeBoolean(msg.jump());
                buf.writeBoolean(msg.attack());
                buf.writeFloat(msg.yaw());
                buf.writeFloat(msg.pitch());
            },
            buf -> new C2SPossessionInput(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SPossessionInput msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            var pd = player.getPersistentData();
            long until = pd.getLong("lvluping_possession_until");
            if (until <= player.level().getGameTime()) return;
            if (!pd.hasUUID("lvluping_possession_mob")) return;
            UUID mobUuid = pd.getUUID("lvluping_possession_mob");

            Mob mob = null;
            AABB search = player.getBoundingBox().inflate(96, 64, 96);
            for (Mob m : player.serverLevel().getEntitiesOfClass(Mob.class, search)) {
                if (m.getUUID().equals(mobUuid)) { mob = m; break; }
            }
            if (mob == null || !mob.isAlive()) return;
            if (!mob.getPersistentData().hasUUID("lvluping_summon_owner")) return;
            if (!player.getUUID().equals(mob.getPersistentData().getUUID("lvluping_summon_owner"))) return;

            mob.setNoAi(true);
            mob.setYRot(msg.yaw());
            mob.setXRot(msg.pitch());
            mob.setYHeadRot(msg.yaw());

            float f = msg.forward();
            float s = msg.strafe();
            if (Math.abs(f) < 0.01f && Math.abs(s) < 0.01f) {
                mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                mob.getNavigation().stop();
                return;
            }
            float yawRad = (float) Math.toRadians(msg.yaw());
            Vec3 forwardVec = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vec3 strafeVec = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
            Vec3 move = forwardVec.scale(f).add(strafeVec.scale(s));
            if (move.lengthSqr() > 1.0) move = move.normalize();
            double speed = 0.35;
            Vec3 dm = new Vec3(move.x * speed, mob.getDeltaMovement().y, move.z * speed);
            if (msg.jump() && mob.onGround()) dm = new Vec3(dm.x, 0.42, dm.z);
            mob.setDeltaMovement(dm);
            mob.hurtMarked = true;

            if (msg.attack()) {
                tryAttack(player, mob);
            }
        });
    }

    private static void tryAttack(ServerPlayer owner, Mob mob) {
        var pd = mob.getPersistentData();
        int cd = pd.getInt("lvluping_possess_attack_cd");
        if (cd > 0) {
            pd.putInt("lvluping_possess_attack_cd", cd - 1);
            return;
        }
        pd.putInt("lvluping_possess_attack_cd", 6);

        var item = mob.getMainHandItem().getItem();
        boolean ranged = item == net.minecraft.world.item.Items.BOW || item == net.minecraft.world.item.Items.CROSSBOW;
        if (ranged) {
            Arrow arrow = net.minecraft.world.entity.EntityType.ARROW.create(owner.serverLevel());
            if (arrow == null) return;
            arrow.setOwner(mob);
            arrow.setPos(mob.getX(), mob.getEyeY() - 0.1, mob.getZ());
            Vec3 look = mob.getLookAngle().normalize();
            arrow.shoot(look.x, look.y, look.z, 2.2f, 2.0f);
            arrow.setBaseDamage(3.5);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            owner.serverLevel().addFreshEntity(arrow);
            return;
        }

        Vec3 look = mob.getLookAngle().normalize();
        double range = 2.6;
        AABB box = mob.getBoundingBox().inflate(range, 1.2, range);
        LivingEntity best = null;
        double bestD = 9999;
        for (LivingEntity e : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == mob) continue;
            if (!e.isAlive()) continue;
            if (e.getUUID().equals(owner.getUUID())) continue;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0).subtract(mob.position()).normalize();
            if (look.dot(to) < 0.3) continue;
            double d = mob.distanceToSqr(e);
            if (d < bestD) { bestD = d; best = e; }
        }
        if (best != null) {
            float dmg = (float) Math.max(2.0, mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            best.hurt(owner.damageSources().playerAttack(owner), dmg);
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}

