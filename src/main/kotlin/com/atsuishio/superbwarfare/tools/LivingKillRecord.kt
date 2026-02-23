package com.atsuishio.superbwarfare.tools

import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

class LivingKillRecord(
    var attacker: LivingEntity,
    var target: Entity,
    var stack: ItemStack,
    var headshot: Boolean,
    var damageType: ResourceKey<DamageType>
) {
    var tick: Int = 0
    var freeze: Boolean = false
    var fastRemove: Boolean = false
}