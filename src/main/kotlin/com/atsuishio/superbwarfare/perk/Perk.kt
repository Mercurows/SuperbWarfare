package com.atsuishio.superbwarfare.perk

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.PropertyModifier
import com.atsuishio.superbwarfare.data.gun.DamageReduce
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.misc.PerkItem
import net.minecraft.ChatFormatting
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraftforge.registries.RegistryManager
import net.minecraftforge.registries.RegistryObject
import java.util.*

open class Perk(val descriptionId: String, val type: Type) : PropertyModifier<GunData, DefaultGunData> {
    val name: String = descriptionId.split("_")
        .filter { it.isNotEmpty() }
        .joinToString("") { word -> word.replaceFirstChar { it.uppercase(Locale.ROOT) } }

    // 默认不进行修改
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {}

    fun getItem(): RegistryObject<Item> {
        val result = ModItems.PERKS.getEntries().filter {
            val item = it.get()
            if (item is PerkItem) {
                return@filter item.perk == this
            }
            return@filter false
        }.firstOrNull()

        return result ?: throw IllegalStateException("Perk " + this.name + " not found")
    }

    /**
     * 在背包中每Tick触发
     */
    open fun tick(data: GunData, instance: PerkInstance, entity: Entity?) {}

    open fun preReload(data: GunData, instance: PerkInstance, entity: Entity?) {}

    open fun postReload(data: GunData, instance: PerkInstance, entity: Entity?) {}

    open fun onKill(data: GunData, instance: PerkInstance, target: Entity, source: DamageSource) {}

    open fun onHurtEntity(damage: Float, data: GunData, instance: PerkInstance, target: Entity, source: DamageSource) {}

    open fun onHit(attacker: LivingEntity, data: GunData, instance: PerkInstance, target: Entity) {}

    open fun getModifiedCustomRPM(rpm: Int, data: GunData, instance: PerkInstance): Int {
        return rpm
    }

    /**
     * 在切换物品时触发
     */
    open fun onChangeSlot(data: GunData, instance: PerkInstance, living: Entity?) {}

    open fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        return damage
    }

    open fun modifyProjectile(data: GunData, instance: PerkInstance, entity: Entity) {}

    /**
     * 用于处理武器伤害衰减比率
     */
    open fun getModifiedDamageReduceRate(reduce: DamageReduce?): Double {
        return reduce?.rate ?: 0.0
    }

    /**
     * 用于处理武器伤害衰减最小距离
     */
    open fun getModifiedDamageReduceMinDistance(reduce: DamageReduce?): Double {
        return reduce?.minDistance ?: 0.0
    }

    /**
     * 用于处理武器近战攻击后的逻辑（命中实体时触发）
     */
    open fun onMeleeAttack(data: GunData, instance: PerkInstance, target: Entity, source: DamageSource) {}

    /**
     * 用于处理武器按下近战键的逻辑（无论是否命中实体都会触发）
     */
    open fun onMeleeSwing(data: GunData, instance: PerkInstance, entity: Entity?) {}

    @Suppress("UnstableApiUsage")
    open fun `is`(tag: TagKey<Perk>): Boolean {
        return RegistryManager.ACTIVE.getRegistry(ModPerks.PERK_KEY).getHolder(this)
            .map { it.`is`(tag) }.orElseGet { false }
    }

    enum class Type(val typeName: String, val color: ChatFormatting) {
        AMMO("Ammo", ChatFormatting.YELLOW),
        FUNCTIONAL("Functional", ChatFormatting.GREEN),
        DAMAGE("Damage", ChatFormatting.RED);
    }
}