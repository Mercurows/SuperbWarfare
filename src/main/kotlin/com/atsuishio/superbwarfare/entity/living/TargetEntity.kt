package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.client.animation.entity.TargetAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.SoundTool
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent

open class TargetEntity(type: EntityType<TargetEntity>, level: Level) : LivingEntity(type, level) {
    open val animationInstance: TargetAnimationInstance? =
        if (this.level().isClientSide) TargetAnimationInstance(this) else null

    open var downTime by DOWN_TIME

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DOWN_TIME, 0)
    }

    override fun getArmorSlots(): Iterable<ItemStack> {
        return NonNullList.withSize(1, ItemStack.EMPTY)
    }

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {}

    override fun causeFallDamage(l: Float, d: Float, source: DamageSource) = false

    override fun shouldRenderAtSqrDistance(pDistance: Double) = true

    init {
        this.noCulling = true
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        // 不处理/kill伤害
        var amount = amount
        if (source.`is`(DamageTypes.GENERIC_KILL)) {
            this.remove(RemovalReason.KILLED)
            return super.hurt(source, amount)
        }

        amount = DAMAGE_MODIFIER.compute(source, amount)
        if (amount <= 0 || downTime > 0) {
            return false
        }

        if (!this.level().isClientSide()) {
            this.level().playSound(
                null,
                BlockPos.containing(this.x, this.y, this.z),
                ModSounds.HIT.get(),
                SoundSource.BLOCKS,
                1f,
                1f
            )
        } else {
            this.level().playLocalSound(
                this.x,
                this.y,
                this.z,
                ModSounds.HIT.get(),
                SoundSource.BLOCKS,
                1f,
                1f,
                false
            )
        }
        return super.hurt(source, amount)
    }

    override fun isPickable() = downTime == 0

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (!player.mainHandItem.isEmpty && !player.mainHandItem.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            return InteractionResult.PASS
        }

        if (player.isShiftKeyDown) {
            if (!this.level().isClientSide()) {
                this.discard()
            }

            if (!player.abilities.instabuild) {
                player.addItem(ItemStack(ModItems.TARGET_DEPLOYER.get()))
            }
        } else {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3((player.x), this.y, (player.z)))
            this.xRot = 0f
            this.xRotO = this.xRot
            downTime = 0
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()
        if (downTime > 0) {
            downTime -= 1
        }
    }

    override fun getDeltaMovement() = Vec3(0.0, 0.0, 0.0)

    override fun isPushable() = false

    override fun getMainArm() = HumanoidArm.RIGHT

    override fun doPush(entityIn: Entity) {}

    override fun pushEntities() {}

    override fun setNoGravity(ignored: Boolean) {
        super.setNoGravity(true)
    }

    override fun aiStep() {
        super.aiStep()
        this.updateSwingTime()
        this.isNoGravity = true
    }

    override fun tickDeath() {
        ++this.deathTime
        if (this.deathTime >= 100) {
            this.spawnAtLocation(ItemStack(ModItems.TARGET_DEPLOYER.get()))
            this.remove(RemovalReason.KILLED)
        }
    }

    override fun getPickResult(): ItemStack? {
        return ItemStack(ModItems.TARGET_DEPLOYER.get())
    }

    @EventBusSubscriber
    companion object {
        @JvmField
        val DOWN_TIME: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TargetEntity::class.java, EntityDataSerializers.INT)
        private val DAMAGE_MODIFIER = createDefaultModifier()
            .immuneTo(DamageTypes.LIGHTNING_BOLT)
            .immuneTo(DamageTypes.FALLING_ANVIL)
            .immuneTo(DamageTypes.MAGIC)

        @SubscribeEvent
        fun onTargetDown(event: LivingDeathEvent) {
            val entity = event.entity
            // 不处理/kill伤害
            if (event.source.`is`(DamageTypes.GENERIC_KILL)) return
            val sourceEntity = event.source.entity

            if (entity is TargetEntity) {
                event.setCanceled(true)
                entity.health = entity.maxHealth

                if (sourceEntity is Player) {
                    sourceEntity.displayClientMessage(
                        Component.translatable(
                            "tips.superbwarfare.target.down",
                            format1D((entity.position()).distanceTo((sourceEntity.position())), "m")
                        ), true
                    )
                    SoundTool.playLocalSound(sourceEntity, ModSounds.TARGET_DOWN.get(), 1f, 1f)
                    entity.downTime = 40
                }
            }
        }

        fun createAttributes(): AttributeSupplier.Builder {
            return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.FLYING_SPEED, 0.0)
        }
    }
}
