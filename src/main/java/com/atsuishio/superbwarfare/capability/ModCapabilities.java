package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities {

    public static final Capability<LaserCapability.ILaserCapability> LASER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<PlayerVariable> PLAYER_VARIABLE = CapabilityManager.get(new CapabilityToken<>() {
    });
}
