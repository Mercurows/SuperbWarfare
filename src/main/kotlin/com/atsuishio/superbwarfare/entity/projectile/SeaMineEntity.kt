package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.projectile.SeaMineItem
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.OBB
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.AbstractMinecart
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3f
import java.util.*

open class SeaMineEntity(type: EntityType<out SeaMineEntity>, level: Level) : Entity(type, level), OwnableEntity {
    var ballast by BALLAST
    var chainLength by CHAIN_LENGTH
    var released by RELEASED
    var optionalOwnerUUID by OWNER
    var anchorBlock by ANCHOR
    var anchorOffset by ANCHOR_OFFSET
    val anchor: Vec3?
        get() = anchorBlock.orElse(null)?.let {
            val offset = anchorOffset
            Vec3(it.x + offset.x.toDouble(), it.y + offset.y.toDouble(), it.z + offset.z.toDouble())
        }
    private var age = 0
    private var health = 5f
    private var detonated = false
    private var lerpSteps = 0
    private var lerpTarget = Vec3.ZERO
    private var lerpYaw = 0f
    private var lerpPitch = 0f

    override fun defineSynchedData() {
        with(entityData) {
            define(BALLAST, false)
            define(CHAIN_LENGTH, 0)
            define(RELEASED, false)
            define(OWNER, Optional.empty())
            define(ANCHOR, Optional.empty())
            define(ANCHOR_OFFSET, Vector3f())
        }
    }

    open fun configure(stack: ItemStack, owner: UUID?) {
        ballast = SeaMineItem.hasBallast(stack)
        chainLength = SeaMineItem.chainLength(stack)
        optionalOwnerUUID = Optional.ofNullable(owner)
    }

    override fun getOwnerUUID(): UUID? = optionalOwnerUUID.orElse(null)

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)

    override fun isPickable() = !isRemoved

    override fun isPushable() = false

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = !ballast

    override fun lerpTo(
        x: Double, y: Double, z: Double, yaw: Float, pitch: Float,
        interpolationSteps: Int, interpolate: Boolean
    ) {
        lerpTarget = Vec3(x, y, z)
        lerpYaw = yaw
        lerpPitch = pitch
        lerpSteps = interpolationSteps.coerceAtLeast(1)
    }

    private fun tickLerp() {
        if (lerpSteps <= 0) return
        setPos(position().add(lerpTarget.subtract(position()).scale(1.0 / lerpSteps)))
        setRot(yRot + Mth.wrapDegrees(lerpYaw - yRot) / lerpSteps, xRot + (lerpPitch - xRot) / lerpSteps)
        lerpSteps--
    }

    override fun getBoundingBoxForCulling(): AABB = anchor?.let {
        boundingBox.minmax(AABB(it, it).inflate(0.0625))
    } ?: boundingBox

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean = distance < (chainLength + 64.0).let { it * it }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (level().isClientSide || detonated || isRemoved || amount <= 0f || isInvulnerableTo(source)) return false
        val damage = if (source.directEntity is SeaMineEntity && source.`is`(DamageTypeTags.IS_EXPLOSION)) amount * 0.01f else amount
        health -= damage
        markHurt()
        if (health <= 0f) explode()
        return true
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (!player.isShiftKeyDown || (ownerUUID != null && player.uuid != ownerUUID)) return InteractionResult.PASS
        if (!level().isClientSide && !detonated) {
            val stack = SeaMineItem.makeInstance(ballast, chainLength)
            discard()
            ItemHandlerHelper.giveItemToPlayer(player, stack)
        }
        return InteractionResult.sidedSuccess(level().isClientSide)
    }

    override fun tick() {
        super.tick()
        if (level().isClientSide) {
            tickLerp()
            return
        }
        age++
        val oldBody = boundingBox
        var fixed = anchor
        if (fixed != null) {
            val support = anchorBlock.get()
            if (!level().hasChunkAt(support)) return
            if (level().getBlockState(support).getCollisionShape(level(), support).isEmpty) {
                anchorBlock = Optional.empty()
                released = true
                fixed = null
            }
        }
        val waterPos = blockPosition()
        val fluid = level().getFluidState(waterPos)
        val water = fluid.`is`(FluidTags.WATER)
        val floating = !ballast || released || fixed != null
        if (water && floating) {
            val surface = if (!level().getFluidState(waterPos.above()).`is`(FluidTags.WATER))
                waterPos.y + fluid.getHeight(level(), waterPos) - 0.4 else Double.POSITIVE_INFINITY
            val limit = fixed?.let { it.y + chainLength } ?: Double.POSITIVE_INFINITY
            val vy = seaMineRiseVelocity(y, deltaMovement.y, minOf(surface, limit))
            deltaMovement = Vec3(
                if (fixed == null) deltaMovement.x * 0.85 else 0.0, vy,
                if (fixed == null) deltaMovement.z * 0.85 else 0.0
            )
        } else {
            deltaMovement = deltaMovement.multiply(0.9, 0.98, 0.9).add(0.0, -0.04, 0.0)
            if (deltaMovement.y < -0.5) deltaMovement = Vec3(deltaMovement.x, -0.5, deltaMovement.z)
        }
        if (fixed != null) setPos(fixed.x, y, fixed.z)
        move(MoverType.SELF, deltaMovement)
        if (fixed != null && y > fixed.y + chainLength) {
            setPos(fixed.x, fixed.y + chainLength, fixed.z)
            deltaMovement = Vec3.ZERO
        }
        if (onGround()) deltaMovement = Vec3(deltaMovement.x * 0.5, 0.0, deltaMovement.z * 0.5)
        if (water && onGround() && ballast && chainLength > 0 && !released && anchor == null) {
            val support = BlockPos.containing(x, y - 0.01, z)
            anchorOffset = Vector3f((x - support.x).toFloat(), (y - support.y).toFloat(), (z - support.z).toFloat())
            anchorBlock = Optional.of(support)
        }
        if (age >= 40) checkContact(oldBody)
        if (y < level().minBuildHeight - 32) discard()
    }

    private fun checkContact(previous: AABB) {
        val body = boundingBox.minmax(previous)
        for (other in level().getEntities(this, body.inflate(4.0)) {
            it.isAlive && !it.isSpectator && (it is LivingEntity || it is Boat || it is AbstractMinecart || it is VehicleEntity)
        }) {
            if (other.uuid == ownerUUID || (ExplosionConfig.FRIENDLY_MINES.get() && owner?.isAlliedTo(other) == true)) continue
            val touching = if (other is VehicleEntity) {
                other.getOBBs().any { OBB.isColliding(it, body) }
            } else {
                val box = other.boundingBox
                val motion = other.position().subtract(other.xo, other.yo, other.zo)
                box.intersects(body) || body.inflate(box.xsize / 2, box.ysize / 2, box.zsize / 2)
                    .clip(box.center.subtract(motion), box.center).isPresent
            }
            if (touching) {
                explode(); return
            }
        }
    }

    private fun explode() {
        if (detonated || isRemoved || level().isClientSide) return
        detonated = true // Claim the detonation before nearby explosions can call hurt again.
        CustomExplosion.Builder(this).attacker(owner)
            .damage(ExplosionConfig.SEA_MINE_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.SEA_MINE_EXPLOSION_RADIUS.get().toFloat()).keepBlock().explode()
        discard()
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putBoolean("Ballast", ballast)
        tag.putInt("ChainLength", chainLength)
        tag.putBoolean("Released", released)
        tag.putInt("Age", age)
        tag.putFloat("Health", health)
        ownerUUID?.let { tag.putUUID("Owner", it) }
        anchor?.let { tag.putDouble("AnchorX", it.x); tag.putDouble("AnchorY", it.y); tag.putDouble("AnchorZ", it.z) }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        val chains = tag.getInt("ChainLength").coerceIn(0, SeaMineItem.MAX_CHAIN_LENGTH)
        ballast = tag.getBoolean("Ballast") || chains > 0
        chainLength = chains
        released = tag.getBoolean("Released")
        optionalOwnerUUID = if (tag.hasUUID("Owner")) Optional.of(tag.getUUID("Owner")) else Optional.empty()
        anchorBlock = Optional.empty()
        if (chains > 0 && !released && tag.contains("AnchorX")) {
            val pos = Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"))
            val block = BlockPos.containing(pos.x, pos.y - 0.01, pos.z)
            anchorOffset =
                Vector3f((pos.x - block.x).toFloat(), (pos.y - block.y).toFloat(), (pos.z - block.z).toFloat())
            anchorBlock = Optional.of(block)
        }
        age = tag.getInt("Age").coerceAtLeast(0)
        health = if (tag.contains("Health")) tag.getFloat("Health").coerceAtLeast(0.01f) else 5f
    }

    fun seaMineRiseVelocity(y: Double, velocity: Double, target: Double): Double =
        if (target.isFinite()) ((target - y) * 0.25).coerceIn(-0.12, 0.12)
        else (velocity + 0.04).coerceIn(-0.12, 0.12)

    companion object {
        @JvmField
        val BALLAST: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val CHAIN_LENGTH: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val RELEASED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val OWNER: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        val ANCHOR: EntityDataAccessor<Optional<BlockPos>> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.OPTIONAL_BLOCK_POS)

        @JvmField
        val ANCHOR_OFFSET: EntityDataAccessor<Vector3f> =
            SynchedEntityData.defineId(SeaMineEntity::class.java, EntityDataSerializers.VECTOR3)
    }
}
