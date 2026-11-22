package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.getBlockReach
import com.atsuishio.superbwarfare.tools.getEntityReach
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.CommonHooks
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import java.util.*

@EventBusSubscriber(modid = Mod.MODID)
internal object MeleeHarvest {
    private const val HIT_TOLERANCE_TICKS = 2
    private const val HIT_TIMEOUT_TICKS = 20

    private class Swing(
        val id: Int,
        val item: Item,
        val gunId: UUID,
        val slot: Int,
        val mode: Int,
        val dimension: ResourceKey<Level>,
        val actionIndex: Int,
        val chargeStrike: Boolean,
        val startedAt: Long,
        val nextStartAt: Long,
        val expiresAt: Long,
        val hitTicks: IntArray,
    ) {
        var nextHit = 0
    }

    private class Progress(val state: BlockState, val breakerId: Int) {
        var damage = 0.0
        var expiresAt = 0L
    }

    private class BreakTarget(val player: ServerPlayer, val pos: BlockPos, val stack: ItemStack)

    private val swings = WeakHashMap<ServerPlayer, Swing>()
    private val progressByLevel = mutableMapOf<ServerLevel, MutableMap<BlockPos, Progress>>()
    private val finishing = ThreadLocal<BreakTarget>()
    private var nextBreakerId = Int.MIN_VALUE

    fun startSwing(player: ServerPlayer, data: GunData, id: Int, actionIndex: Int, chargeStrike: Boolean, releaseStrike: Boolean) {
        if (!canSwing(player, data)) return
        val gunId = data.uuid ?: return
        val actions = data.meleeActions()
        if (actionIndex !in actions.indices || (chargeStrike && releaseStrike)) return
        if (releaseStrike && (data.get(GunProp.RIGHT_CLICK_ACTION) != RightClickAction.HEAVY_HOLD ||
                actionIndex != actions.indexOfFirst { it.isHeavy() }.coerceAtLeast(0))) return

        val now = player.server.tickCount.toLong()
        val previous = swings[player]
        if (previous != null && (id - previous.id <= 0 || now + HIT_TOLERANCE_TICKS < previous.nextStartAt)) return
        val startedAt = maxOf(now, previous?.nextStartAt ?: now)
        val timing = data.meleeSwingTiming(player, actionIndex, chargeStrike, releaseStrike) ?: return
        val chainWindow = data.get(GunProp.MELEE_CHAIN_WINDOW)
        val nextStart = startedAt + maxOf(1, timing.duration - chainWindow, timing.hitTicks.lastOrNull() ?: 0)
        swings[player] = Swing(id, data.item(), gunId, player.inventory.selected, data.selectedFireMode.get(),
            player.level().dimension(), actionIndex, chargeStrike, startedAt, nextStart,
            startedAt + timing.duration + HIT_TIMEOUT_TICKS, timing.hitTicks)
    }

    fun acceptHit(player: ServerPlayer, data: GunData, id: Int, actionIndex: Int, chargeStrike: Boolean, hitIndex: Int): Boolean {
        val swing = swings[player] ?: return false
        if (id != swing.id || hitIndex < swing.nextHit || actionIndex != swing.actionIndex || chargeStrike != swing.chargeStrike) return false
        val hitTick = swing.hitTicks.getOrNull(hitIndex) ?: return false
        if (data.item() !== swing.item || data.uuid != swing.gunId || player.inventory.selected != swing.slot ||
            data.selectedFireMode.get() != swing.mode || player.level().dimension() != swing.dimension || !canSwing(player, data)) return false

        val now = player.server.tickCount.toLong()
        if (now > swing.expiresAt || now + HIT_TOLERANCE_TICKS < swing.startedAt + hitTick) return false
        swing.nextHit = hitIndex + 1
        return true
    }

    private fun canSwing(player: ServerPlayer, data: GunData): Boolean {
        if (player.isCreative || player.isSpectator || !player.isAlive || player.isUsingItem ||
            player.hasEffect(ModMobEffects.SHOCK) || !data.item().canHarvest(data)) return false
        val vehicle = player.vehicle
        return vehicle !is VehicleEntity || !vehicle.banHand(player)
    }

    /** Returns true when the swing was directed at a block, even if a protection prevents damage. */
    fun hitBlock(player: ServerPlayer, data: GunData, baseDamage: Double): Boolean {
        val hit = player.pick(player.getBlockReach(), 1f, false) as? BlockHitResult ?: return false
        if (hit.type != HitResult.Type.BLOCK || entityInFrontOfBlock(player, hit)) return false
        val pos = hit.blockPos
        val level = player.serverLevel()
        if (!level.hasChunkAt(pos) || !level.worldBorder.isWithinBounds(pos) || !level.mayInteract(player, pos) ||
            player.blockActionRestricted(level, pos, player.gameMode.gameModeForPlayer)) return true

        val state = level.getBlockState(pos)
        val hardness = state.getDestroySpeed(level, pos)
        if (state.isAir || !hardness.isFinite() || hardness < 0f) return true

        val click = CommonHooks.onLeftClickBlock(player, pos, hit.direction, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
        if (click.isCanceled || click.useItem.isFalse ||
            level.getBlockState(pos) != state) return true
        if (!click.useBlock.isFalse) state.attack(level, pos, player)

        val damage = player.getDigSpeed(state, pos) * baseDamage
        if (!damage.isFinite() || damage <= 0.0 || level.getBlockState(pos) != state) return true
        data.get(GunProp.MELEE_SOUND).mine?.let { level.playSound(null, pos, it, SoundSource.BLOCKS, 1f, 1f) }

        val health = hardness * MiscConfig.HARVEST_HARDNESS_MULTIPLIER.get()
        val existing = progressByLevel[level]?.get(pos)
        if (existing != null && (existing.state != state || existing.expiresAt <= level.gameTime)) clearProgress(level, pos)

        val progress = progressByLevel.getOrPut(level) { mutableMapOf() }.getOrPut(pos.immutable()) {
            // Distinct from real entity IDs so nearby players, including the miner, see the cracks.
            Progress(state, nextBreakerId++)
        }
        progress.damage += damage
        if (progress.damage >= health) {
            clearProgress(level, pos)
            val previous = finishing.get()
            finishing.set(BreakTarget(player, pos, player.mainHandItem))
            try {
                player.gameMode.destroyBlock(pos)
            } finally {
                if (previous == null) finishing.remove() else finishing.set(previous)
            }
        } else {
            progress.expiresAt = level.gameTime + GunConfig.HARVEST_PROGRESS_TICKS.get()
            val stage = (progress.damage / health * 10).toInt().coerceIn(0, 9)
            level.destroyBlockProgress(progress.breakerId, pos, stage)
        }
        return true
    }

    private fun entityInFrontOfBlock(player: ServerPlayer, block: BlockHitResult): Boolean {
        val eye = player.eyePosition
        val reach = minOf(player.getEntityReach(), eye.distanceTo(block.location))
        val step = player.lookAngle.scale(reach)
        return ProjectileUtil.getEntityHitResult(player, eye, eye.add(step), player.boundingBox.expandTowards(step).inflate(1.0),
            { it.isPickable && it.isAttackable && !it.isSpectator }, reach * reach) != null
    }

    fun isFinishing(player: Player, pos: BlockPos, item: GunItem): Boolean {
        val target = finishing.get() ?: return false
        return target.player === player && target.pos == pos && target.stack === player.mainHandItem && target.stack.`is`(item)
    }

    private fun clearProgress(level: ServerLevel, pos: BlockPos) {
        val entries = progressByLevel[level] ?: return
        val progress = entries.remove(pos) ?: return
        level.destroyBlockProgress(progress.breakerId, pos, -1)
        if (entries.isEmpty()) progressByLevel.remove(level)
    }

    @SubscribeEvent
    fun onLevelTick(event: LevelTickEvent.Post) {
        val level = event.level as? ServerLevel ?: return
        val entries = progressByLevel[level] ?: return
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val (pos, progress) = iterator.next()
            if (level.gameTime < progress.expiresAt && level.hasChunkAt(pos) && level.getBlockState(pos) == progress.state) continue
            iterator.remove()
            level.destroyBlockProgress(progress.breakerId, pos, -1)
        }
        if (entries.isEmpty()) progressByLevel.remove(level)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level as? ServerLevel ?: return
        clearProgress(level, event.pos)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBlockPlaced(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? ServerLevel ?: return
        if (event is BlockEvent.EntityMultiPlaceEvent) {
            event.replacedBlockSnapshots.forEach { clearProgress(level, it.pos) }
        } else {
            clearProgress(level, event.pos)
        }
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        val level = event.level as? ServerLevel ?: return
        progressByLevel.remove(level)
    }
}
