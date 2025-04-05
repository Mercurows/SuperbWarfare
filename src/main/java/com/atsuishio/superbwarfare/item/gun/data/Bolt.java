package com.atsuishio.superbwarfare.item.gun.data;

import net.minecraft.nbt.CompoundTag;

public final class Bolt {
    private final CompoundTag data;
    private final GunData gunData;


    Bolt(GunData data) {
        this.gunData = data;
        this.data = data.data();
    }

    public boolean needed() {
        return data.getBoolean("NeedBoltAction");
    }

    public void markNeeded() {
        data.putBoolean("NeedBoltAction", true);
    }

    public void markNeedless() {
        data.remove("NeedBoltAction");
    }

    public int defaultActionTime() {
        return (int) gunData.getGunData("BoltActionTime") + gunData.item().getCustomBoltActionTime(gunData.stack());
    }

    public int actionTime() {
        return data.getInt("BoltActionTime");
    }

    public void setActionTime(int tick) {
        if (tick <= 0) {
            data.remove("BoltActionTime");
        } else {
            data.putInt("BoltActionTime", tick);
        }
    }

    public void reduceActionTime() {
        setActionTime(actionTime() - 1);
    }

}
