package com.atsuishio.superbwarfare.network.dataslot;

/**
 * Code based on @GoryMoon's Chargers
 */
public class SimpleEnergyData implements ContainerEnergyData {

    private final int[] data;

    public SimpleEnergyData(int size) {
        this.data = new int[size];
    }

    @Override
    public int get(int index) {
        return this.data[index];
    }

    @Override
    public void set(int index, int value) {
        this.data[index] = value;
    }

    @Override
    public int getCount() {
        return this.data.length;
    }
}
