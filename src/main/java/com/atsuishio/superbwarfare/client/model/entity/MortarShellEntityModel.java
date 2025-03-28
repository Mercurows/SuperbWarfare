package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MortarShellEntityModel extends GeoModel<MortarShellEntity> {

    @Override
    public ResourceLocation getAnimationResource(MortarShellEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(MortarShellEntity entity) {
        return Mod.loc("geo/mortar_shell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MortarShellEntity entity) {
        return Mod.loc("textures/entity/mortar.png");
    }
}
