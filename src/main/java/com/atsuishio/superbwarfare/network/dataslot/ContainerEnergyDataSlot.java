package com.atsuishio.superbwarfare.network.dataslot;

/**
 * Code based on @GoryMoon's Chargers
 */
public abstract class ContainerEnergyDataSlot {

    private long prevValue;

    public ContainerEnergyDataSlot() {
    }

    public static ContainerEnergyDataSlot forContainer(final ContainerEnergyData data, final int index) {
        return new ContainerEnergyDataSlot() {
            public long get() {
                return data.get(index);
            }

            public void set(long value) {
                data.set(index, value);
            }
        };
    }

    public abstract long get();

    public abstract void set(long value);

    public boolean checkAndClearUpdateFlag() {
        long tmp = this.get();
        boolean changed = tmp != this.prevValue;
        this.prevValue = tmp;
        return changed;
    }
}
