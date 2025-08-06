package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class EntityEventHandler {

    @SubscribeEvent
    public static void cancelExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof VehicleEntity) {
            event.setKnockbackVelocity(Vec3.ZERO);
        }
    }
}
