package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class SmallGrenadeEntity : GunGrenadeEntity {
    constructor(type: EntityType<out SmallGrenadeEntity>, level: Level) : super(type, level)

    constructor(owner: Entity?, level: Level) : super(ModEntities.SMALL_GRENADE.get(), level) {
        this.owner = owner
    }

    override fun trail() {
        smallTrail()
    }
}
