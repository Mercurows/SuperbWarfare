package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class PrismaticBoltEntity extends Entity {
    public float randomAngle = (float) (((Math.random() * 2) - 1) * 45);
    public int tickO;
    public int tick;

    public PrismaticBoltEntity(EntityType<? extends PrismaticBoltEntity> type, Level world) {
        super(type, world);
    }

    public PrismaticBoltEntity(Level level) {
        super(ModEntities.PRISMATIC_BOLT.get(), level);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        tickO = tick;
        super.tick();
        tick++;
        if (this.tick > 4) {
            this.discard();
        }
    }

    public float getLerpTick(float tickDelta) {
        return Mth.lerp(tickDelta, tickO, tick);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void move(MoverType pType, Vec3 pPos) {
    }
}
