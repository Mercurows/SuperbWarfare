package com.atsuishio.superbwarfare.perk.js

import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Vex

class TargetProxy(target: Entity) {
    val armor: Double = if (target is LivingEntity) target.getAttributeValue(Attributes.ARMOR) else 0.0
    val isUndead: Boolean = target is LivingEntity && target.type.`is`(EntityTypeTags.UNDEAD)
    val isRaider: Boolean = target.type.`is`(EntityTypeTags.RAIDERS) || target is Vex
    val isMonster: Boolean = target is Monster
    val health: Float = (target as? LivingEntity)?.health ?: 0f
}
