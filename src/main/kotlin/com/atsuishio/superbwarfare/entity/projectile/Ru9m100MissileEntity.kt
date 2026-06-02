package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class Ru9m100MissileEntity(type: EntityType<out Ru9m100MissileEntity>, level: Level) :
    Ru9m336MissileEntity(type, level) {

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_AIR_MISSILE.get()
    }
}
