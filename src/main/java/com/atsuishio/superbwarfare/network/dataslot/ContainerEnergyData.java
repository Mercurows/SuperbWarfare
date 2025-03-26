package com.atsuishio.superbwarfare.network.dataslot;

import net.minecraft.world.inventory.ContainerData;

public interface ContainerEnergyData extends ContainerData {

    int get(int pIndex);

    void set(int pIndex, int pValue);

    int getCount();
}
