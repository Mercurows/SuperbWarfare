package com.atsuishio.superbwarfare.network.dataslot;

/**
 * Code based on @GoryMoon's Chargers
 */
public class SimpleEnergyData implements ContainerEnergyData {

    private final long[] data;

    public SimpleEnergyData(int size) {
        this.data = new long[size];
    }

    @Override
    public long get(int index) {
        return this.data[index];
    }

    @Override
    public void set(int index, long value) {
        this.data[index] = value;
    }

    @Override
    public int getCount() {
        return this.data.length;
    }
}
