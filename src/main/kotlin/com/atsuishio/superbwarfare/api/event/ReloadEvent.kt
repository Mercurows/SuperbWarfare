package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("0.8.0")
open class ReloadEvent private constructor(val entity: Entity?, val data: GunData) : Event() {
    val stack: ItemStack = data.stack

    class Pre(shooter: Entity?, data: GunData) : ReloadEvent(shooter, data)

    class Post(shooter: Entity?, data: GunData) : ReloadEvent(shooter, data)
}
