package com.atsuishio.superbwarfare.item.gun.data;

import net.minecraft.nbt.CompoundTag;

public final class Charge {
    private final CompoundTag data;

    Charge(GunData data) {
        this.data = data.data();
    }

    public void markStart() {
        data.putBoolean("StartCharge", true);
    }

    public boolean shouldStartCharge() {
        return data.getBoolean("StartCharge");
    }

    public void markStarted() {
        data.remove("StartCharge");
    }

    public int time() {
        return data.getInt("ChargeTime");
    }

    public void reduce() {
        setTime(time() - 1);
    }

    public void setTime(int chargeTime) {
        if (chargeTime <= 0) {
            data.remove("ChargeTime");
        } else {
            data.putInt("ChargeTime", chargeTime);
        }
    }

    public void reset() {
        setTime(0);
    }
}
