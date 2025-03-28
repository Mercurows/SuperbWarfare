package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity.LASER_LENGTH;

public class LaserTowerModel extends GeoModel<LaserTowerEntity> {

    @Override
    public ResourceLocation getAnimationResource(LaserTowerEntity entity) {
        return Mod.loc("animations/laser_tower.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(LaserTowerEntity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 24 || player.isScoping()) {
            return Mod.loc("geo/laser_tower.geo.json");
        } else if (distance < 48) {
            return Mod.loc("geo/laser_tower.lod1.geo.json");
        } else {
            return Mod.loc("geo/laser_tower.lod2.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(LaserTowerEntity entity) {
        return Mod.loc("textures/entity/laser_tower.png");
    }

    @Override
    public void setCustomAnimations(LaserTowerEntity animatable, long instanceId, AnimationState<LaserTowerEntity> animationState) {
        GeoBone laser = getAnimationProcessor().getBone("laser");
        laser.setScaleZ(10 * animatable.getEntityData().get(LASER_LENGTH));
    }
}
