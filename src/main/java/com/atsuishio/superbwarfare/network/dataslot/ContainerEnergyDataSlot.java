package com.atsuishio.superbwarfare.network.dataslot;

/**
 * Code based on @GoryMoon's Chargers
 */
public abstract class ContainerEnergyDataSlot {

    private int prevValue;

    public ContainerEnergyDataSlot() {
    }

    public static ContainerEnergyDataSlot forContainer(final ContainerEnergyData data, final int index) {
        return new ContainerEnergyDataSlot() {
            public int get() {
                return data.get(index);
            }

            @Override
            public void set(int value) {
                data.set(index, value);
            }

        };
    }

    public abstract int get();

    public abstract void set(int value);

    public boolean checkAndClearUpdateFlag() {
        int tmp = this.get();
        boolean changed = tmp != this.prevValue;
        this.prevValue = tmp;
        return changed;
    }
}
