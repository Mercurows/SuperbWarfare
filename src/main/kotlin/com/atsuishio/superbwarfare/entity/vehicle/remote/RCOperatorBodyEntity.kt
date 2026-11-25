package com.atsuishio.superbwarfare.entity.vehicle.remote

import com.mojang.authlib.GameProfile
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.IEntityAdditionalSpawnData
import net.minecraftforge.network.NetworkHooks

open class RCOperatorBodyEntity(type: EntityType<out RCOperatorBodyEntity>, level: Level) :
    LivingEntity(type, level), IEntityAdditionalSpawnData {
    open var operatorProfile: GameProfile? = null
        protected set

    private var arm = HumanoidArm.RIGHT
    private val equipment = Array(EquipmentSlot.entries.size) { ItemStack.EMPTY }

    open fun copyAppearance(player: ServerPlayer) {
        operatorProfile = player.gameProfile
        arm = player.mainArm
        moveTo(player.x, player.y, player.z, player.yRot, player.xRot)
        yBodyRot = player.yBodyRot
        yHeadRot = player.yHeadRot
        pose = player.pose
        setSharedFlag(FLAG_FALL_FLYING, player.isFallFlying)
        isSwimming = player.isSwimming
        isNoGravity = player.isNoGravity || player.abilities.flying
        setOnGround(player.onGround())
        fallDistance = player.fallDistance
        airSupply = player.airSupply
        val movement = player.deltaMovement
        val fallSpeed = if (movement.y == 0.0 && !player.onGround() && fallDistance > 0f) {
            (player.y - player.yo).coerceIn(-fallDistance.toDouble(), 0.0)
        } else movement.y
        deltaMovement = Vec3(movement.x, fallSpeed, movement.z)
        for (effect in listOf(MobEffects.JUMP, MobEffects.SLOW_FALLING, MobEffects.LEVITATION, MobEffects.WATER_BREATHING)) {
            player.getEffect(effect)?.let { addEffect(MobEffectInstance(it)) }
        }
        EquipmentSlot.entries.forEach { setItemSlot(it, player.getItemBySlot(it).copy()) }
    }

    override fun getDimensions(pose: Pose): EntityDimensions = when (pose) {
        Pose.CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f)
        Pose.FALL_FLYING, Pose.SWIMMING, Pose.SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f)
        else -> super.getDimensions(pose)
    }

    override fun getArmorSlots(): Iterable<ItemStack> =
        listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).map(::getItemBySlot)

    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = equipment[slot.ordinal]

    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
        equipment[slot.ordinal] = stack
    }

    override fun getMainArm(): HumanoidArm = arm

    override fun isPushable(): Boolean = false

    override fun getName(): Component = operatorProfile?.name?.let(Component::literal) ?: super.getName()

    override fun getScoreboardName(): String = operatorProfile?.name ?: super.getScoreboardName()

    override fun pushEntities() {}

    override fun travel(input: Vec3) {
        if (isPassenger) {
            deltaMovement = Vec3.ZERO
        } else {
            super.travel(Vec3.ZERO)
        }
    }

    override fun isInvulnerableTo(source: DamageSource): Boolean =
        RemoteControl.hasCreativeOperator(this) || super.isInvulnerableTo(source)

    override fun hurt(source: DamageSource, amount: Float): Boolean =
        !level().isClientSide && RemoteControl.hurtBody(this, source, amount)

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)

    override fun writeSpawnData(buffer: FriendlyByteBuf) {
        buffer.writeNullable(operatorProfile, FriendlyByteBuf::writeGameProfile)
        buffer.writeEnum(arm)
        buffer.writeFloat(yBodyRot)
    }

    override fun readSpawnData(buffer: FriendlyByteBuf) {
        operatorProfile = buffer.readNullable(FriendlyByteBuf::readGameProfile)
        arm = buffer.readEnum(HumanoidArm::class.java)
        yBodyRot = buffer.readFloat()
        yBodyRotO = yBodyRot
    }
}
