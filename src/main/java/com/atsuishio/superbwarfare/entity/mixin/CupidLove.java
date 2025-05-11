package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.entity.npc.Villager;

public interface CupidLove {

    static CupidLove getInstance(Villager villager) {
        return (CupidLove) villager;
    }

    void superbwarfare$setCupidLove(boolean love);

    boolean superbwarfare$getCupidLove();
}
