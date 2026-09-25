package com.atsuishio.superbwarfare.entity.goal

import com.atsuishio.superbwarfare.data.gun.FireMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData
import com.atsuishio.superbwarfare.tools.MillisTimer
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import java.util.*
import kotlin.math.min

/**
 * 生物持枪射击的 goal。
 *
 * - 瞄准时间 / 射程 / 散布等 AI 参数来自生成时抽中的配置（`MobGunData.selection`）；
 * - 枪械状态一律取**当前手持物品**对应的 [GunData]（`MobGunData.gunData()`），
 *   不再像以前那样另外缓存一份，避免生物换枪后拿旧参数打新枪；
 * - 注册时占用 MOVE/LOOK 控制位，与原版近战 / 远程 goal 互斥，不会被"举着枪贴脸砍"。
 */
class GunShootGoal<T : Mob>(
    private val mob: T,
    private val mobGun: MobGunData,
) : Goal() {

    private var aimTime = 0
    private val shootTimer = MillisTimer()

    init {
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK))
    }

    private fun gunData(): GunData? {
        // 配置没能还原出来时（数据包删了对应条目）不该继续开火
        if (mobGun.selection == null) return null
        return mobGun.gunData()
    }

    /** 备弹池或弹匣里还有能打出去的弹药 */
    private fun hasAmmo(data: GunData): Boolean {
        return data.countBackupAmmo(mob) > 0 || data.hasEnoughAmmoToShoot(mob)
    }

    override fun canUse(): Boolean {
        if (mob.target == null) return false

        val data = gunData() ?: return false
        return hasAmmo(data)
    }

    override fun canContinueToUse(): Boolean {
        val data = gunData() ?: return false
        if (!hasAmmo(data)) return false

        // 没有目标但还在赶路时也保持 goal 运行（与原实现一致）
        return mob.target != null || !mob.navigation.isDone
    }

    override fun start() {
        super.start()
        mob.isAggressive = true
    }

    override fun stop() {
        super.stop()
        mob.isAggressive = false
        mob.stopUsingItem()
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun tick() {
        val target = mob.target ?: return
        val data = gunData() ?: return

        val distance = mob.distanceToSqr(target.x, target.y, target.z)

        if (mob.sensing.hasLineOfSight(target)) {
            aimTime = min(mobGun.aimTime, aimTime + 1)
        } else if (mobGun.clearAimTimeWhenLostSight) {
            aimTime = 0
        } else {
            aimTime = (aimTime - 1).coerceAtLeast(0)
        }

        mob.lookAt(target, 30f, 30f)

        if (distance > mobGun.shootDistance) {
            mob.navigation.moveTo(target, 1.0)
        } else {
            mob.navigation.stop()
        }

        data.tick(mob, true)

        if (data.shouldStartReloading(mob)) {
            data.startReload()
        }

        if (data.shouldStartBolt()) {
            data.startBolt()
        }

        if (data.canShoot(mob) && aimTime >= mobGun.aimTime) {
            // 实际射速 = 基础 RPM * 全局射速倍率（perk / 数据包都可能改这个倍率）
            val rps = data.get(GunProp.RPM) * data.get(GunProp.RPM_MULTIPLIER) / 60.0

            // cooldown in ms
            var cooldown = Math.round(1000 / rps)

            val fireMode = data.selectedFireModeInfo().mode
            // 半自动或连发开火时，添加额外的开火冷却时间
            if (fireMode == FireMode.SEMI || fireMode == FireMode.BURST && data.burstAmount.get() == 0) {
                cooldown += mobGun.semiFireInterval
            }

            if (!shootTimer.started()) {
                shootTimer.start()
                // 首发瞬间发射
                shootTimer.progress = cooldown + 1
            }

            if (shootTimer.progress >= cooldown) {
                var newProgress = shootTimer.progress

                // 低帧率下的开火次数补偿
                do {
                    data.shoot(mob, mobGun.spread, mobGun.zoom, target.uuid)
                    newProgress -= cooldown
                } while (newProgress - cooldown > 0)

                shootTimer.progress = newProgress
            }
        } else {
            shootTimer.stop()
        }
    }
}
