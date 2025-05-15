package com.atsuishio.superbwarfare.data.gun.subdata;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.Starter;
import com.atsuishio.superbwarfare.data.gun.value.Timer;

public final class Charge {
    public final Timer timer;
    public final Starter starter;

    public Charge(GunData data) {
        this.timer = new Timer(data.data(), "Charge");
        this.starter = new Starter(data.data(), "Charge");
    }

    public int time() {
        return timer.get();
    }
}
