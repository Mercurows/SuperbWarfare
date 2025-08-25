package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Tiers;

public class NetheriteHammer extends Hammer {

    public NetheriteHammer() {
        super(Tiers.NETHERITE, 13, -3.2f, new CustomDamageProperty(false).fireResistant());
    }

}
