package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.entity.monster.Monster;

public class MonsterHunter extends Perk {

    public MonsterHunter() {
        super("monster_hunter", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, value, target, source) -> target instanceof Monster ? value * 1.1 + 0.1 * data.perk.getLevel(this) : value);
        appendModification(GunProp.EXPLOSION_DAMAGE, (data, value, target, source) -> target instanceof Monster ? value * 1.1 + 0.1 * data.perk.getLevel(this) : value);
    }
}
