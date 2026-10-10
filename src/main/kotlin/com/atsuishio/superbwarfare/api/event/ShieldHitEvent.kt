package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.event.ShieldRuntime
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

/**
 * 枪盾即将尝试抵挡一次命中时触发
 */
@Cancelable
@ApiStatus.AvailableSince("0.8.10")
open class ShieldHitEvent private constructor(
    val shooter: LivingEntity,
    val gunStack: ItemStack,
    val gunData: GunData,
    val shields: List<ShieldRuntime.Instance>,
    var damage: Float,
) : Event() {

    class Deflect(
        shooter: LivingEntity,
        gunStack: ItemStack,
        gunData: GunData,
        shields: List<ShieldRuntime.Instance>,
        damage: Float,
        val projectile: Projectile?,
        val victim: Entity,
        val hitVec: Vec3,
        val travel: Vec3,
    ) : ShieldHitEvent(shooter, gunStack, gunData, shields, damage)
}
