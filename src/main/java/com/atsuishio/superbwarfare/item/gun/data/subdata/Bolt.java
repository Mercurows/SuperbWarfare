package com.atsuishio.superbwarfare.item.gun.data.subdata;

import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.BooleanValue;
import com.atsuishio.superbwarfare.item.gun.data.value.Timer;

public final class Bolt {

    public Bolt(GunData data) {
        needed = new BooleanValue(data.data(), "NeedBoltAction");
        actionTimer = new Timer(data.data(), "BoltActionTime");
    }

    public final BooleanValue needed;
    public final Timer actionTimer;
}
