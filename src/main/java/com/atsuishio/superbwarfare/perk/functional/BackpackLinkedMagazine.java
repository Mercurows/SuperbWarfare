package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;

public class BackpackLinkedMagazine extends Perk {

    public BackpackLinkedMagazine() {
        super("backpack_linked_magazine", Perk.Type.FUNCTIONAL);
        appendModification(GunProp.MAGAZINE, (data, value) -> 0);
        appendModification(GunProp.HEAT_PER_SHOOT, (data, value) -> value + (20 - data.perk.getLevel(this)) * 0.2);
    }
}
