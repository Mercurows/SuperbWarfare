package com.atsuishio.superbwarfare.perk.js

import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Vex

class TargetProxy(val target: Entity) {
    fun getArmor(): Double = if (target is LivingEntity) target.getAttributeValue(Attributes.ARMOR) else 0.0

    fun isUndead(): Boolean = target is LivingEntity && target.type.`is`(EntityTypeTags.UNDEAD)

    fun isRaider(): Boolean = target.type.`is`(EntityTypeTags.RAIDERS) || target is Vex

    fun isMonster(): Boolean = target is Monster

    fun getHealth(): Float = (target as? LivingEntity)?.health ?: 0f

    fun getMaxHealth(): Float = (target as? LivingEntity)?.maxHealth ?: 0f

    // 这里高版本应该使用#aquatic标签判断
    fun isAquatic(): Boolean = target.type.`is`(EntityTypeTags.AQUATIC)
}
