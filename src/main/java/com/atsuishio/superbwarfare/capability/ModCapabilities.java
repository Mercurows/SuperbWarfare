package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.laser.LaserCapability;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import net.neoforged.neoforge.capabilities.EntityCapability;

public class ModCapabilities {

    public static final EntityCapability<LaserCapability, Void> LASER_CAPABILITY = EntityCapability.createVoid(ModUtils.loc("laser_capability"), LaserCapability.class);
    public static final EntityCapability<PlayerVariable, Void> PLAYER_VARIABLE = EntityCapability.createVoid(ModUtils.loc("player_variable"), PlayerVariable.class);

}
