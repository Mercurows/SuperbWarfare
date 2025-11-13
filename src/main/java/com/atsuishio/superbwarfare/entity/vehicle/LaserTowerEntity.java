package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LaserTowerEntity extends AutoAimableEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LaserTowerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.LASER_TOWER.get(), world);
    }

    public LaserTowerEntity(EntityType<LaserTowerEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
