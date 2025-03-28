package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class MelonBombEntity extends FastThrowableProjectile {

    public MelonBombEntity(EntityType<? extends MelonBombEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public MelonBombEntity(LivingEntity entity, Level level) {
        super(ModEntities.MELON_BOMB.get(), entity, level);
    }

    // TODO AEP
//    @Override
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.MELON;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        ProjectileTool.causeCustomExplode(this, VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get(), VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue(), 1.5f);
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 600) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get(), VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue(), 1.5f);
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05F;
    }
}
