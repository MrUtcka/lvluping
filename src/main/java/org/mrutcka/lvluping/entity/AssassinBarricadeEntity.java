package org.mrutcka.lvluping.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


public class AssassinBarricadeEntity extends Entity {

    private static final String TAG_REMOVE_AT = "lvluping_barricade_remove_at";
    private static final double WALL_HALF_WIDTH = 1.5;
    private static final double WALL_HALF_DEPTH = 0.22;
    private static final double WALL_HEIGHT = 2.0;

    private long removeAtGameTime;

    public AssassinBarricadeEntity(EntityType<? extends AssassinBarricadeEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setRemoveAtGameTime(long gameTime) {
        this.removeAtGameTime = gameTime;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        removeAtGameTime = compound.getLong(TAG_REMOVE_AT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (removeAtGameTime != 0L) {
            compound.putLong(TAG_REMOVE_AT, removeAtGameTime);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && removeAtGameTime != 0L && level().getGameTime() >= removeAtGameTime) {
            discard();
            return;
        }
        // Держим AABB в соответствии с makeBoundingBox(); иначе коллизии/снаряды используют устаревший ящик.
        setBoundingBox(makeBoundingBox());
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive() && !isRemoved();
    }

    @Override
    public boolean isPickable() {
        return false;
    }


    @Override
    public boolean canBeHitByProjectile() {
        return isAlive() && !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }


    @Override
    protected AABB makeBoundingBox() {
        double cx = getX();
        double y0 = getY();
        double cz = getZ();
        double rad = Math.toRadians(getYRot());
        double pdx = Math.cos(rad);
        double pdz = Math.sin(rad);
        double hx = WALL_HALF_WIDTH * Math.abs(pdx) + WALL_HALF_DEPTH * Math.abs(pdz);
        double hz = WALL_HALF_WIDTH * Math.abs(pdz) + WALL_HALF_DEPTH * Math.abs(pdx);
        return new AABB(cx - hx, y0, cz - hz, cx + hx, y0 + WALL_HEIGHT, cz + hz);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return makeBoundingBox();
    }

    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        return false;
    }
}
