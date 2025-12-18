package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void defineSynchedData() {
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
    public void move(MoverType pType, Vec3 pPos) {
    }
}
