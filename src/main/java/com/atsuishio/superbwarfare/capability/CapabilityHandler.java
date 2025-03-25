package com.atsuishio.superbwarfare.capability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CapabilityHandler {

    private static final CapabilityHandler INSTANCE = new CapabilityHandler();

    public static void register(IEventBus bus) {
        bus.addListener(INSTANCE::registerCapabilities);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            event.registerEntity(ModCapabilities.LASER_CAPABILITY, entityType,
                    (entity, ctx) -> {
                        if (entity instanceof Player) {
                            return new LaserCapability.LaserCapabilityImpl();
                        }
                        return null;
                    });
        }
    }

}
