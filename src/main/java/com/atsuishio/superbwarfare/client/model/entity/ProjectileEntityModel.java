package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ProjectileEntityModel extends GeoModel<ProjectileEntity> {

    @Override
    public ResourceLocation getAnimationResource(ProjectileEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(ProjectileEntity entity) {
        return Mod.loc("geo/projectile_entity.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ProjectileEntity entity) {
        return Mod.loc("textures/entity/empty.png");
    }

    @Override
    public void setCustomAnimations(ProjectileEntity animatable, long instanceId, AnimationState animationState) {
        CoreGeoBone bone = getAnimationProcessor().getBone("bone");
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            bone.setHidden(animatable.position().distanceTo(player.position()) < 2 || animatable.tickCount < 1);
        }
    }
}
