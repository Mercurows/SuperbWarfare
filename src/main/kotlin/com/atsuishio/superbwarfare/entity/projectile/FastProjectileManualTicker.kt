package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

/**
 * 服务端手动 tick 兜底：让 fast projectile 在未加载区块中也能继续飞行。
 *
 * 背景：`isAlwaysTicking()` 只能防止弹体在区块卸载时被移除（仍留在 level 中），
 * 但 vanilla 仍可能把它从 `entityTickList` 中 stopTicking 掉而冻结（且 always-ticking
 * 实体不会再被 `updateChunkStatus` 重新加入 tick 列表）。所以这里在每 tick 末尾检测：
 * 若弹体的 `tickCount` 没有推进（原版系统已停止 tick 它），就手动调用一次 `tick()`，
 * 让它继续按弹道/制导飞行，同时完全不加载沿途区块。
 *
 * 判定依据：`tickCount` 仅由 `ServerLevel.tickNonPassenger` 递增；若某 tick 结束时
 * `tickCount` 与上次相同，说明原版系统这一 tick 没有 tick 该弹体 → 手动补一次。
 * 这样既不会与原版 double-tick，也能保证在未加载区块中持续飞行。
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
object FastProjectileManualTicker {

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (!ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) return
        if (FastThrowableProjectile.manualTickRegistered().isEmpty()) return

        for (projectile in FastThrowableProjectile.manualTickRegistered()) {
            if (projectile.tickCount < 1) continue
            // 已移除或客户端实体：清理
            if (projectile.isRemoved || projectile.level().isClientSide) {
                FastThrowableProjectile.unregisterForManualTickInternal(projectile)
                continue
            }

            val current = projectile.tickCount
            val last = FastThrowableProjectile.lastManualTickCount(projectile.id) ?: current

            if (current == last) {
                // 原版系统这一 tick 没有 tick 该弹体（冻结）→ 手动补一次
                try {
                    projectile.tick()
                } catch (_: Exception) {
                    // 手动 tick 出错时静默跳过，避免拖垮服务器
                }
            }

            FastThrowableProjectile.setLastManualTickCount(projectile.id, projectile.tickCount)
        }
    }
}
