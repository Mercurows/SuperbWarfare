package com.atsuishio.superbwarfare.item.gun.data;

public final class Charge {
    public final Timer timer;
    public final Starter starter;

    Charge(GunData data) {
        this.timer = new Timer(data.data(), "Charge");
        this.starter = new Starter(data.data(), "Charge");
    }

    public int time() {
        return timer.get();
    }
}
