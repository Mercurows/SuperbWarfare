package com.atsuishio.superbwarfare.entity.vehicle.remote

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.world.saveddata.RemoteBindingData
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityMountEvent
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import java.util.*

@EventBusSubscriber(modid = Mod.MODID)
object RemoteControl {
    private const val RETURN_DATA = "SuperbWarfareRemoteReturn"
    // A region distance of 2 applies level 31 to the anchor chunk, so its entities tick.
    private const val ANCHOR_DISTANCE = 2
    private val anchorTicket = TicketType.create("superbwarfare_remote_anchor", Comparator.naturalOrder<UUID>())
    private val sessions = mutableMapOf<UUID, Session>()
    private val forwardingBodyDamage = mutableMapOf<ServerPlayer, DamageSource>()

    private class Session(
        val player: ServerPlayer,
        val vehicle: RCEntity,
        val body: RCOperatorBodyEntity,
        val carrier: Entity?,
        val seat: Int,
    ) {
        val anchorLevel: ServerLevel = player.serverLevel()
        var anchor: Vec3 = player.position()
        var chunk = carrier?.chunkPosition() ?: ChunkPos(BlockPos.containing(anchor))
        var mounting = true
        var ending = false
    }

    @JvmStatic
    fun startSession(player: ServerPlayer, vehicle: RCEntity): Boolean {
        if (player is FakePlayer || !player.isAlive || player.isSpectator ||
            player.isSleeping || player.level() !== vehicle.level() ||
            player.containerMenu !== player.inventoryMenu || sessions.containsKey(player.uuid) ||
            sessions.values.any { it.vehicle === vehicle } ||
            player.persistentData.contains(RETURN_DATA) || !vehicle.canStartRemoteControl(player)
        ) return false

        val level = player.serverLevel()
        val carrier = player.vehicle
        val seat = when (carrier) {
            is VehicleEntity -> carrier.getSeatIndex(player)
            else -> carrier?.passengers?.indexOf(player) ?: -1
        }
        if (carrier != null && (carrier is RCEntity || !isCarrierAvailable(carrier) || seat < 0)) return false

        val body = ModEntities.RC_OPERATOR_BODY.get().create(level) ?: return false
        val session = Session(player, vehicle, body, carrier, seat)
        body.copyAppearance(player)
        sessions[player.uuid] = session
        level.chunkSource.addRegionTicket(anchorTicket, session.chunk, ANCHOR_DISTANCE, player.uuid)
        saveReturn(session)

        if (!level.addFreshEntity(body)) {
            endSession(player)
            return false
        }

        if (carrier != null) {
            player.stopRiding()
            if (player.isPassenger || !boardCarrier(carrier, body, seat)) {
                endSession(player)
                return false
            }
            carrier.positionRider(body)
            if (carrier is VehicleEntity && seat == 0) {
                carrier.processInput(0)
                carrier.mouseInput(0.0, 0.0)
            }
        }

        vehicle.processInput(0)
        VehicleVecUtils.setDriverAngle(vehicle, player)
        player.isSprinting = false
        if (!player.startRiding(vehicle, true) || vehicle.firstPassenger !== player) {
            endSession(player)
            return false
        }
        player.fallDistance = 0f
        player.deltaMovement = Vec3.ZERO
        session.mounting = false
        player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.controlling", vehicle.displayName), true)
        return true
    }

    @JvmStatic
    fun isMounting(passenger: Entity, vehicle: RCEntity): Boolean =
        sessions[passenger.uuid]?.let { it.mounting && !it.ending && it.player === passenger && it.vehicle === vehicle } == true

    private fun isCarrierAvailable(carrier: Entity): Boolean =
        !carrier.isRemoved && (carrier !is VehicleEntity || !carrier.isWreck && carrier.health > 0)

    private fun boardCarrier(carrier: Entity, passenger: Entity, seat: Int): Boolean =
        if (carrier is VehicleEntity) carrier.boardSeat(passenger, seat) else passenger.startRiding(carrier, true)

    private fun isBodyOnCarrier(session: Session, carrier: Entity): Boolean = session.body.vehicle === carrier &&
        (carrier !is VehicleEntity || carrier.getNthEntity(session.seat) === session.body)

    @JvmStatic
    fun endSession(vehicle: RCEntity) {
        sessions.values.firstOrNull { it.vehicle === vehicle }?.let { endSession(it.player) }
    }

    @JvmStatic
    @JvmOverloads
    fun endSession(player: ServerPlayer, returnOnDeath: Boolean = false): Boolean {
        val session = sessions[player.uuid] ?: return false
        if (session.player !== player || session.ending) return false
        session.ending = true
        val carrier = session.carrier
        val bodyInSeat = carrier != null && isBodyOnCarrier(session, carrier)
        if (!session.body.isRemoved && (carrier == null || bodyInSeat)) updateAnchor(session)
        session.vehicle.processInput(0)
        if (player.vehicle === session.vehicle) player.stopRiding()
        if (player.vehicle === session.vehicle) {
            session.ending = false
            return false
        }

        try {
            if (bodyInSeat) session.body.stopRiding()
            if (player.isAlive || returnOnDeath) restoreReturn(player, !returnOnDeath && (session.mounting || bodyInSeat))
            else player.persistentData.remove(RETURN_DATA)
        } finally {
            sessions.remove(player.uuid)
            session.body.discard()
            session.anchorLevel.chunkSource.removeRegionTicket(anchorTicket, session.chunk, ANCHOR_DISTANCE, player.uuid)
        }
        return true
    }

    @JvmStatic
    fun hurtBody(body: RCOperatorBodyEntity, source: DamageSource, amount: Float): Boolean {
        val operatorId = body.operatorProfile?.id ?: return false
        val session = sessions[operatorId] ?: return false
        if (session.body !== body || session.mounting || session.ending || !amount.isFinite() || amount <= 0f) return false

        val player = session.player
        if (player.isCreative || isForwardingBodyDamage(player)) return false
        val attacker = source.entity as? ServerPlayer
        if (attacker != null && (!attacker.server.isPvpAllowed || !attacker.canHarmPlayer(player))) return false
        if (!ForgeHooks.onLivingAttack(body, source, amount)) return false
        if (!source.`is`(DamageTypeTags.IS_FALL) && !endSession(player)) return false
        forwardingBodyDamage[player] = source
        try {
            return player.hurt(source, amount)
        } finally {
            forwardingBodyDamage.remove(player)
        }
    }

    @JvmStatic
    fun isForwardingBodyDamage(entity: Entity): Boolean = entity is ServerPlayer && entity in forwardingBodyDamage

    @JvmStatic
    fun hasCreativeOperator(body: RCOperatorBodyEntity): Boolean =
        !body.level().isClientSide &&
            body.operatorProfile?.id?.let(sessions::get)?.let { it.body === body && it.player.isCreative } == true

    private fun saveReturn(session: Session) {
        session.player.persistentData.put(RETURN_DATA, CompoundTag().apply {
            putString("Dimension", session.anchorLevel.dimension().location().toString())
            putDouble("X", session.anchor.x)
            putDouble("Y", session.anchor.y)
            putDouble("Z", session.anchor.z)
            saveBodyState(this, session.body)
            session.carrier?.let {
                putUUID("Carrier", it.uuid)
                putInt("Seat", session.seat)
            }
        })
    }

    private fun saveBodyState(tag: CompoundTag, body: RCOperatorBodyEntity) {
        tag.putFloat("Yaw", body.yRot)
        tag.putFloat("Pitch", body.xRot)
        tag.putString("Pose", body.pose.name)
        tag.putBoolean("OnGround", body.onGround())
        tag.putFloat("FallDistance", body.fallDistance)
        val motion = body.deltaMovement
        tag.putDouble("MotionX", motion.x)
        tag.putDouble("MotionY", motion.y)
        tag.putDouble("MotionZ", motion.z)
    }

    private fun updateAnchor(session: Session) {
        val position = session.body.position()
        val chunk = session.carrier?.chunkPosition() ?: ChunkPos(BlockPos.containing(position))
        if (chunk != session.chunk) {
            val source = session.anchorLevel.chunkSource
            source.addRegionTicket(anchorTicket, chunk, ANCHOR_DISTANCE, session.player.uuid)
            source.removeRegionTicket(anchorTicket, session.chunk, ANCHOR_DISTANCE, session.player.uuid)
            session.chunk = chunk
        }
        if (session.anchor != position) {
            session.anchor = position
            session.player.persistentData.getCompound(RETURN_DATA).apply {
                putDouble("X", position.x)
                putDouble("Y", position.y)
                putDouble("Z", position.z)
            }
        }
        saveBodyState(session.player.persistentData.getCompound(RETURN_DATA), session.body)
    }

    private fun restoreReturn(player: ServerPlayer, allowCarrier: Boolean = true) {
        if (!player.persistentData.contains(RETURN_DATA, Tag.TAG_COMPOUND.toInt())) return
        val tag = player.persistentData.getCompound(RETURN_DATA)
        val dimension = ResourceLocation.tryParse(tag.getString("Dimension"))
        val level = player.server.allLevels.firstOrNull { it.dimension().location() == dimension }
            ?: player.server.overworld()
        val anchor = Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"))
        val wasMounted = tag.hasUUID("Carrier")
        if (allowCarrier && wasMounted && level.dimension().location() == dimension) {
            val carrier = level.getEntity(tag.getUUID("Carrier"))
            val seat = tag.getInt("Seat")
            if (carrier != null && isCarrierAvailable(carrier) && player.level() === level) {
                player.yRot = tag.getFloat("Yaw")
                player.xRot = tag.getFloat("Pitch")
                if (player.vehicle === carrier || boardCarrier(carrier, player, seat)) {
                    player.fallDistance = 0f
                    player.persistentData.remove(RETURN_DATA)
                    return
                }
            }
        }
        val returnToBody = !wasMounted && level.dimension().location() == dimension &&
            level.worldBorder.isWithinBounds(BlockPos.containing(anchor))
        val destination = when {
            returnToBody -> anchor
            level.dimension().location() == dimension -> findReturnPosition(player, level, anchor, wasMounted)
            else -> Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.sharedSpawnPos))
        }

        player.teleportTo(level, destination.x, destination.y, destination.z, tag.getFloat("Yaw"), tag.getFloat("Pitch"))
        val onGround = returnToBody && tag.getBoolean("OnGround")
        player.fallDistance = if (returnToBody && !onGround) tag.getFloat("FallDistance") else 0f
        player.deltaMovement = if (returnToBody) {
            val y = tag.getDouble("MotionY")
            Vec3(tag.getDouble("MotionX"), if (onGround) y.coerceAtLeast(0.0) else y, tag.getDouble("MotionZ"))
        } else Vec3.ZERO
        if (returnToBody) {
            player.setOnGround(onGround)
            Pose.entries.firstOrNull { it.name == tag.getString("Pose") }?.let { player.pose = it }
        }
        if (player.deltaMovement.lengthSqr() > 0.0) player.connection.send(ClientboundSetEntityMotionPacket(player))
        player.setJumping(false)
        player.persistentData.remove(RETURN_DATA)
    }

    private fun findReturnPosition(player: ServerPlayer, level: ServerLevel, anchor: Vec3, searchSurface: Boolean): Vec3 {
        val block = BlockPos.containing(anchor)
        if (level.worldBorder.isWithinBounds(block)) level.getChunkAt(block)
        val size = player.getDimensions(Pose.STANDING)
        fun canStand(pos: Vec3): Boolean {
            val block = BlockPos.containing(pos)
            return level.hasChunkAt(block) && level.worldBorder.isWithinBounds(block) &&
                level.getFluidState(block).isEmpty &&
                level.noCollision(player, size.makeBoundingBox(pos)) &&
                !level.noCollision(player, size.makeBoundingBox(pos).move(0.0, -0.1, 0.0))
        }
        if (canStand(anchor)) return anchor
        for (candidate in BlockPos.withinManhattan(block, 2, 2, 2)) {
            val position = Vec3.atBottomCenterOf(candidate)
            if (canStand(position)) return position
        }
        if (searchSurface) {
            for (candidate in BlockPos.withinManhattan(block, 4, 0, 4)) {
                if (!level.hasChunkAt(candidate)) continue
                val surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate)
                val position = Vec3.atBottomCenterOf(surface)
                if (canStand(position)) return position
            }
        }
        return Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.sharedSpawnPos))
    }

    private fun isOperating(entity: Entity): Boolean {
        if (entity !is ServerPlayer) return false
        val session = sessions[entity.uuid] ?: return false
        return session.player === entity && !session.mounting && !session.ending && entity.vehicle === session.vehicle
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        for (session in sessions.values.toList()) {
            val player = session.player
            if (player.server !== event.server || session.mounting || session.ending) continue
            val carrier = session.carrier
            val carrierValid = carrier == null || isCarrierAvailable(carrier) && isBodyOnCarrier(session, carrier)
            if (!player.isAlive || player.vehicle !== session.vehicle || session.body.isRemoved ||
                session.body.level() !== player.level() || session.vehicle.isWreck || !carrierValid
            ) {
                endSession(player)
            } else {
                updateAnchor(session)
            }
        }
    }

    @SubscribeEvent
    fun onMount(event: EntityMountEvent) {
        if (event.level.isClientSide) return
        if (event.isDismounting) {
            val body = event.entityMounting as? RCOperatorBodyEntity ?: return
            val session = body.operatorProfile?.id?.let(sessions::get) ?: return
            if (!session.ending && session.body === body && session.carrier === event.entityBeingMounted) updateAnchor(session)
            return
        }
        val vehicle = event.entityBeingMounted as? RCEntity ?: return
        if (!isMounting(event.entityMounting, vehicle)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        endSession(player)
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        sessions.values.filter { it.player.server === event.server }.forEach { endSession(it.player) }
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        restoreReturn(player)
        RemoteBindingData.get(player.server).syncTo(player)
    }

    @SubscribeEvent
    fun onTravel(event: EntityTravelToDimensionEvent) {
        if (event.entity is RCOperatorBodyEntity || isOperating(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onAttack(event: LivingAttackEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (isOperating(player) && forwardingBodyDamage[player] !== event.source &&
            !event.source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)
        ) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onDeath(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (forwardingBodyDamage[player] === event.source && isOperating(player)) {
            endSession(player, true)
        }
    }

    @SubscribeEvent
    fun onAttackEntity(event: AttackEntityEvent) {
        if (isOperating(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onInteractBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (isOperating(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onUseItem(event: PlayerInteractEvent.RightClickItem) {
        if (isOperating(event.entity) && event.itemStack.item !is MonitorItem) event.isCanceled = true
    }

    @SubscribeEvent
    fun onInteractEntity(event: PlayerInteractEvent.EntityInteract) {
        if (isOperating(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onInteractEntitySpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        if (isOperating(event.entity)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onBreakBlock(event: BlockEvent.BreakEvent) {
        if (isOperating(event.player)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onPickup(event: EntityItemPickupEvent) {
        if (isOperating(event.entity)) event.isCanceled = true
    }
}
