package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.ModUtils;
import net.neoforged.neoforge.capabilities.EntityCapability;

public class ModCapabilities {

    public static final EntityCapability<LaserCapability.ILaserCapability, Void> LASER_CAPABILITY = EntityCapability.createVoid(ModUtils.loc("laser_capability"), LaserCapability.ILaserCapability.class);

}
