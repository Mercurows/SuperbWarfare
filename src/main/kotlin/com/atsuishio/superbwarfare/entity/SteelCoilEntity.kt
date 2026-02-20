package com.atsuishio.superbwarfare.entity

import net.minecraft.core.NonNullList
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Mob.createMobAttributes
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkHooks
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class SteelCoilEntity(type: EntityType<SteelCoilEntity>, level: Level) : PathfinderMob(type, level), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    var wheelRot = 0f
    var wheelRotO = 0f

    override fun aiStep() {
        super.aiStep()
        this.updateSwingTime()
    }

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

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(1, object : MeleeAttackGoal(this, 1.4, false) {
            override fun getAttackReachSqr(entity: LivingEntity): Double {
                return (this.mob.bbWidth * this.mob.bbWidth + entity.bbWidth).toDouble()
            }
        })
        this.targetSelector.addGoal(2, HurtByTargetGoal(this).setAlertOthers())
//        this.goalSelector.addGoal(3, RandomLookAroundGoal(this))
        this.goalSelector.addGoal(3, FloatGoal(this))
//        this.goalSelector.addGoal(5, RandomStrollGoal(this, 0.8))
        this.targetSelector.addGoal(4, NearestAttackableTargetGoal(this, Player::class.java, false, false))
    }

    override fun getMobType(): MobType = MobType.UNDEFINED

    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
        }
    }

    override fun baseTick() {
        wheelRotO = wheelRot
        super.baseTick()
        val speed = deltaMovement.dot(forward).toFloat()
        val c = 4f * Mth.PI
        val t = c / speed
        val rpt = 360f / t
        wheelRot += 2 * rpt
    }

    fun getRotaion(ticks: Float): Float {
        return Mth.lerp(ticks, wheelRotO, wheelRot)
    }
}