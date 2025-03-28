package com.atsuishio.superbwarfare.compat.jade;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.compat.jade.providers.C4InfoProvider;
import com.atsuishio.superbwarfare.compat.jade.providers.ContainerEntityProvider;
import com.atsuishio.superbwarfare.compat.jade.providers.VehicleHealthProvider;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SbwJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(VehicleHealthProvider.INSTANCE, VehicleEntity.class);
        registration.registerEntityComponent(C4InfoProvider.INSTANCE, C4Entity.class);
        registration.registerBlockComponent(ContainerEntityProvider.INSTANCE, ContainerBlock.class);
    }
}
