package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LaserTowerEntity extends AutoAimableEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
