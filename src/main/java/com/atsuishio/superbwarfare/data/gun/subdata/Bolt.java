package com.atsuishio.superbwarfare.data.gun.subdata;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.BooleanValue;
import com.atsuishio.superbwarfare.data.gun.value.Timer;

public final class Bolt {

    public Bolt(GunData data) {
        needed = new BooleanValue(data.data(), "NeedBoltAction");
        actionTimer = new Timer(data.data(), "BoltActionTime");
    }

    public final BooleanValue needed;
    public final Timer actionTimer;
}
