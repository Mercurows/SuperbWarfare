package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.toVec3
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData.Companion.enabledTDM
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DiodeBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.*

open class EDDEntity : HangingEntity, OwnableEntity {
    // 0 - Left Top; 1 - Left Bottom; 2 - Right Bottom; 3 - Right Top
    var corner: Int

    @JvmOverloads
    constructor(
        type: EntityType<out EDDEntity> = ModEntities.EDD.get(),
        level: Level,
        corner: Int = 0
    ) : super(type, level) {
        this.corner = corner
    }

    @JvmOverloads
    constructor(
        type: EntityType<out EDDEntity> = ModEntities.EDD.get(),
        owner: LivingEntity?,
        level: Level,
        corner: Int = 0
    ) : super(type, level) {
        this.corner = corner
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
    }

    @JvmOverloads
    constructor(
        type: EntityType<out EDDEntity> = ModEntities.EDD.get(),
        owner: LivingEntity?,
        level: Level,
        pos: BlockPos,
        direction: Direction,
        corner: Int = 0
    ) : super(type, level, pos) {
        this.corner = corner
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
        this.setDirection(direction)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (source.directEntity is EDDEntity) return false
        return super.hurt(source, amount)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(OWNER_UUID, Optional.empty())
            .define(LAST_ATTACKER_UUID, "undefined")
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putInt("Corner", this.corner)
        tag.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID))
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID!!)
        }
        tag.putByte("Facing", this.direction.get3DDataValue().toByte())
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        if (tag.contains("LastAttacker")) {
            this.entityData.set(LAST_ATTACKER_UUID, tag.getString("LastAttacker"))
        }

        if (tag.contains("Corner")) {
            this.corner = tag.getInt("Corner")
        }

        var uuid: UUID?
        if (tag.hasUUID("Owner")) {
            uuid = tag.getUUID("Owner")
        } else {
            val s = tag.getString("Owner")

            try {
                uuid = if (this.server == null) {
                    UUID.fromString(s)
                } else {
                    OldUsersConverter.convertMobOwnerIfNecessary(this.server!!, s)
                }
            } catch (exception: Exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception)
                uuid = null
            }
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid)
            } catch (_: Throwable) {
            }
        }

        this.setDirection(Direction.from3DDataValue(tag.getByte("Facing").toInt()))
    }

    override fun setDirection(direction: Direction) {
        this.direction = direction
        if (direction.axis.isHorizontal) {
            this.xRot = 0.0f
            this.yRot = (this.direction.get2DDataValue() * 90).toFloat()
        } else {
            this.xRot = (-90 * direction.axisDirection.step).toFloat()
            this.yRot = 0.0f
        }

        this.xRotO = this.xRot
        this.yRotO = this.yRot
        this.recalculateBoundingBox()
    }

    override fun calculateBoundingBox(
        pos: BlockPos,
        direction: Direction
    ): AABB {
        val d0 = 0.46875
        val centerX = this.pos.x.toDouble() + 0.5 - direction.stepX.toDouble() * d0
        val centerY = this.pos.y.toDouble() + 0.5 - direction.stepY.toDouble() * d0
        val centerZ = this.pos.z.toDouble() + 0.5 - direction.stepZ.toDouble() * d0

        val halfWidth = 8 / 32.0
        val halfHeight = 8 / 32.0

        val cornerOffset = calculateCornerOffset(direction, this.corner, halfWidth, halfHeight)

        val finalX = centerX + cornerOffset.x
        val finalY = centerY + cornerOffset.y
        val finalZ = centerZ + cornerOffset.z

        this.setPosRaw(finalX, finalY, finalZ)

        var dx = 8 / 32.0
        var dy = 8 / 32.0
        var dz = 8 / 32.0
        when (direction.axis) {
            Direction.Axis.X -> dx = 1.0 / 32.0
            Direction.Axis.Y -> dy = 1.0 / 32.0
            Direction.Axis.Z -> dz = 1.0 / 32.0
        }

        return AABB(
            finalX - dx, finalY - dy, finalZ - dz,
            finalX + dx, finalY + dy, finalZ + dz
        )
    }

    private fun calculateCornerOffset(
        direction: Direction,
        corner: Int,
        width: Double,
        height: Double
    ): Vec3 {
        if (corner !in 0..3) return Vec3.ZERO

        val left = (corner == 0 || corner == 1)   // 左
        val top = (corner == 0 || corner == 3)   // 上

        val signY = if (top) 1.0 else -1.0

        return when (direction) {
            Direction.NORTH -> {
                val signX = if (left) 1.0 else -1.0
                Vec3(signX * width, signY * height, 0.0)
            }

            Direction.SOUTH -> {
                val signX = if (left) -1.0 else 1.0
                Vec3(signX * width, signY * height, 0.0)
            }

            Direction.WEST -> {
                val signZ = if (left) -1.0 else 1.0
                Vec3(0.0, signY * height, signZ * width)
            }

            Direction.EAST -> {
                val signZ = if (left) 1.0 else -1.0
                Vec3(0.0, signY * height, signZ * width)
            }

            else -> Vec3.ZERO
        }
    }

    override fun dropItem(pBrokenEntity: Entity?) {

    }

    override fun playPlacementSound() {
        this.playSound(SoundEvents.ITEM_FRAME_PLACE, 1f, 1f)
    }

    override fun survives(): Boolean {
        if (!this.level().noCollision(this)) {
            return false
        } else {
            val blockstate = this.level().getBlockState(this.pos.relative(this.direction.opposite))
            return if (blockstate.isSolid || this.direction.axis.isHorizontal && DiodeBlock.isDiode(blockstate))
                this.level().getEntities(this, this.boundingBox, HANGING_ENTITY).isEmpty() else false
        }
    }

    override fun getAddEntityPacket(entity: ServerEntity): Packet<ClientGamePacketListener> {
        val data = this.corner * 10 + this.direction.get3DDataValue()
        return ClientboundAddEntityPacket(this, data, this.getPos())
    }

    override fun recreateFromPacket(packet: ClientboundAddEntityPacket) {
        super.recreateFromPacket(packet)
        this.corner = packet.data / 10
        this.setDirection(Direction.from3DDataValue(packet.data % 10))
    }

    fun setOwnerUUID(pUuid: UUID?) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid))
    }

    override fun getOwnerUUID(): UUID? {
        return this.entityData.get(OWNER_UUID).orElse(null)
    }

    fun isOwnedBy(pEntity: LivingEntity?): Boolean {
        return pEntity === this.owner
    }

    fun isFacingLeft(): Boolean {
        return this.corner == 0 || this.corner == 1
    }

    fun getFacingDirection(): Direction {
        return when (this.direction) {
            Direction.NORTH -> if (this.isFacingLeft()) Direction.EAST else Direction.WEST
            Direction.SOUTH -> if (this.isFacingLeft()) Direction.WEST else Direction.EAST
            Direction.EAST -> if (this.isFacingLeft()) Direction.SOUTH else Direction.NORTH
            else -> if (this.isFacingLeft()) Direction.NORTH else Direction.SOUTH
        }
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    private fun triggerExplode(pos: Vec3) {
        CustomExplosion.Builder(this)
            .position(pos)
            .attacker(this.owner)
            .damage(ExplosionConfig.EDD_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.EDD_EXPLOSION_RADIUS.get().toFloat())
            .keepBlock()
            .withParticleType(ParticleTool.ParticleType.MINI)
            .explode()

        this.discard()
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (this.isOwnedBy(player) && player.isShiftKeyDown) {
            if (!this.level().isClientSide()) {
                this.discard()
            }

            if (!player.abilities.instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, ItemStack(ModItems.EDD.get()))
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()

        val facing = this.getFacingDirection()

        val aabb = this.boundingBox
            .expandTowards(this.lookAngle.normalize().scale(0.5))
            .expandTowards(facing.step().toVec3().scale(ExplosionConfig.EDD_TRACE_RANGE.get().toDouble()))
        val entity = this.level().getEntitiesOfClass(
            Entity::class.java,
            aabb
        ) { true }.asSequence().filter {
            it !is EDDEntity && it !is TargetEntity
                    && !it.type.`is`(ModTags.EntityTypes.DECOY)
                    && it != this.owner
                    && !(it is Player && (it.isCreative || it.isSpectator))
                    && if (ExplosionConfig.FRIENDLY_MINES.get()) {
                if (owner == null) true else owner != it && !owner!!.isAlliedTo(it)
            } else {
                (owner != null && owner != it && !owner!!.isAlliedTo(it)) || it.team == null || enabledTDM(it)
            }
        }.toList().firstOrNull {
            this.level().clip(
                ClipContext(
                    this.position(),
                    it.position(),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                )
            ).type != HitResult.Type.BLOCK
        }

        if (entity != null) {
            this.triggerExplode(entity.position())
            ParticleTool.spawnMiniExplosionParticles(this.level(), entity.position())
            this.discard()
        }
    }

    companion object {
        @JvmField
        val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(EDDEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(EDDEntity::class.java, EntityDataSerializers.STRING)
    }
}