package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerLaserLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerPowerLayer;
import com.atsuishio.superbwarfare.client.model.entity.LaserTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LaserTowerRenderer extends VehicleRenderer<LaserTowerEntity> {

    public LaserTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LaserTowerModel());
        this.addRenderLayer(new LaserTowerPowerLayer(this));
        this.addRenderLayer(new LaserTowerLaserLayer(this));
    }
}
