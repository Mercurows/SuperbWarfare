package com.atsuishio.superbwarfare.entity

import net.minecraft.core.NonNullList
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob.createMobAttributes
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkHooks
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class SteelCoilEntity(type: EntityType<SteelCoilEntity>, level: Level) : LivingEntity(type, level), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getArmorSlots(): Iterable<ItemStack?> {
        return NonNullList.withSize(1, ItemStack.EMPTY)
    }

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack {
        return ItemStack.EMPTY
    }

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {
    }

    override fun causeFallDamage(l: Float, d: Float, source: DamageSource): Boolean {
        return false
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener?> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
        }
    }
}