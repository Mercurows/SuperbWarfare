package com.atsuishio.superbwarfare.perk.js

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Vex
import kotlin.jvm.optionals.getOrNull

class TargetProxy(val target: Entity) {
    fun hasEffect(effectId: String): Boolean {
        val living = target as? LivingEntity ?: return false
        val id = ResourceLocation.tryParse(effectId) ?: return false
        val effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).getOrNull() ?: return false
        return living.hasEffect(effect)
    }

    fun getArmor(): Double = if (target is LivingEntity) target.getAttributeValue(Attributes.ARMOR) else 0.0

    fun isUndead(): Boolean = target is LivingEntity && target.type.`is`(EntityTypeTags.UNDEAD)

    fun isRaider(): Boolean = target.type.`is`(EntityTypeTags.RAIDERS) || target is Vex

    fun isMonster(): Boolean = target is Monster

    fun getHealth(): Float = (target as? LivingEntity)?.health ?: 0f

    fun getMaxHealth(): Float = (target as? LivingEntity)?.maxHealth ?: 0f

    // 这里高版本应该使用#aquatic标签判断
    fun isAquatic(): Boolean = target.type.`is`(EntityTypeTags.AQUATIC)
}
