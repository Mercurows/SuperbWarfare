package com.atsuishio.superbwarfare.network.dataslot;

public interface ContainerEnergyData {

    long get(int pIndex);

    void set(int pIndex, long pValue);

    int getCount();
}
