package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.api.event.ReloadEvent
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.postEvent
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.MissingMappingsEvent
import kotlin.math.max
import kotlin.math.min

@Mod.EventBusSubscriber
object GunEventHandler {
    /**
     * 拉大栓
     */
    private fun handleGunBolt(data: GunData) {
        if (data.item.useSpecialFireProcedure(data)) return

        data.bolt.actionTimer.reduce()

        // 执行拉栓期间额外行为
        data.item.boltTimeBehaviors[data.bolt.actionTimer.get()]?.accept(data)

        if (data.bolt.actionTimer.get() == 1) {
            data.bolt.needed.set(false)
        }
    }

    /**
     * 播放拉栓音效
     */
    fun playGunBoltSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.bolt

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 2f, 1f)
        }

        val shooterHeight = shooter.eyePosition.distanceTo(
            Vec3.atLowerCornerOf(
                shooter.level().clip(
                    ClipContext(
                        shooter.eyePosition, shooter.eyePosition.add(Vec3(0.0, -1.0, 0.0).scale(10.0)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter
                    )
                ).blockPos
            )
        )

        com.atsuishio.superbwarfare.Mod.queueServerWork((data.bolt.actionTimer.get() / 2.0 + 1.5 * shooterHeight).toInt()) {
            if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                val ammoType = data.selectedAmmoConsumer().playerAmmoType
                when (ammoType) {
                    Ammo.SHOTGUN -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_SHOTGUN.get(),
                        max(0.75 - 0.12 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    Ammo.SNIPER, Ammo.HEAVY -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_50CAL.get(),
                        max(1 - 0.15 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    else -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_NORMAL.get(),
                        max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                        1f
                    )
                }
            } else {
                SoundTool.playLocalSound(
                    shooter,
                    ModSounds.SHELL_CASING_NORMAL.get(),
                    max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                    1f
                )
            }
        }
    }

    /**
     * 完成换弹过程，装填弹药
     */
    private fun finishReload(shooter: Entity?, data: GunData) {
        if (data.item.isOpenBolt(data)) {
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                finishGunEmptyReload(shooter, data)
            } else {
                finishGunNormalReload(shooter, data)
            }
        } else {
            finishGunEmptyReload(shooter, data)
        }
        data.reload.setTime(0)
        data.reload.setState(ReloadState.NOT_RELOADING)

        data.reload.reloadStarter.finish()
    }

    /**
     * 初始化枪械ID和弹药数量
     */
    fun init(shooter: Entity?, data: GunData) {
        if (!data.initialized()) {
            data.initialize()
            if (shooter is Player && shooter.isCreative) {
                data.ammo.set(data.get(GunProp.MAGAZINE))
            }
        }
    }

    /**
     * 更新perk相关属性
     */
    fun tickPerk(shooter: Entity?, data: GunData) {
        for (type in Perk.Type.entries.toTypedArray()) {
            val instance = data.perk.getInstances(type)
            instance.forEach {
                it.perk.tick(data, it, shooter)
            }
        }
    }

    fun autoReload(shooter: Entity?, data: GunData, inMainHand: Boolean) {
        val autoReload = data.get(GunProp.AUTO_RELOAD) ?: return

        if (inMainHand && autoReload && !data.hasEnoughAmmoToShoot(shooter)) {
            tryStartReload(shooter, data, false)
        }
    }

    fun tryStartReload(shooter: Entity?, data: GunData, save: Boolean = true) {
        if (data.useBackpackAmmo() || data.meleeOnly()) return

        if ((shooter == null || !shooter.isSpectator)
            && !data.charging() && !data.reloading() && data.reload.time() == 0 && data.bolt.actionTimer.get() == 0
        ) {
            // 检查备弹
            if (!data.hasBackupAmmo(shooter)) return

            // Clip > Magazine > Iterative
            val reloadTypes = data.get(GunProp.RELOAD_TYPES)
            val canMagazineReload = reloadTypes.contains(ReloadType.MAGAZINE) && !reloadTypes.contains(ReloadType.CLIP)
            val canClipLoad = !data.hasEnoughAmmoToShoot(shooter) && reloadTypes.contains(ReloadType.CLIP)
            val canSingleReload = reloadTypes.contains(ReloadType.ITERATIVE)

            if (canMagazineReload || canClipLoad) {
                val magazine = data.get(GunProp.MAGAZINE)
                val extra = if (data.item.isOpenBolt(data) && data.item.hasBulletInBarrel(data)) 1 else 0
                val maxAmmo = magazine + extra

                if (data.ammo.get() < maxAmmo) {
                    data.startReload()
                }
            } else if (canSingleReload && data.ammo.get() < data.get(GunProp.MAGAZINE)) {
                data.reload.singleReloadStarter.markStart()
            } else {
                return
            }

            data.burstAmount.reset()
            if (save) {
                data.save()
            }
        }
    }

    /**
     * 减少过热值
     */
    fun handleCooldown(shooter: Entity?, data: GunData) {
        var rate = 1.0

        if (shooter != null) {
            if (shooter.wasInPowderSnow) {
                rate = data.get(GunProp.IN_SNOW_COOLDOWN_RATE)
            } else if (shooter.isInWaterOrRain) {
                rate = data.get(GunProp.IN_WATER_COOLDOWN_RATE)
            } else if (shooter.isOnFire) {
                rate = data.get(GunProp.IN_FIRE_COOLDOWN_RATE)
            } else if (shooter.isInLava) {
                rate = data.get(GunProp.IN_LAVA_COOLDOWN_RATE)
            }
        }

        data.heat.set(max(data.heat.get() - data.get(GunProp.NATURAL_COOLDOWN) * rate, 0.0))

        if (data.heat.get() < 80 && data.overHeat.get()) {
            data.overHeat.set(false)
        }
    }

    /**
     * 返还多余弹药
     */
    fun redrawExtraAmmo(shooter: Entity?, data: GunData) {
        val hasBulletInBarrel = data.item.hasBulletInBarrel(data)
        val ammoCount = data.ammo.get()
        val magazine = data.get(GunProp.MAGAZINE)

        // TODO 修改为更正确的退弹药方式？
        if ((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine)) {
            val count = ammoCount - magazine - (if (hasBulletInBarrel) 1 else 0)

            if (shooter is Player) {
                PlayerVariable.modify(shooter) {
                    if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                        val ammoType = data.selectedAmmoConsumer().playerAmmoType
                        ammoType?.add(it, count)
                    }
                }
            }

            data.ammo.set(magazine + (if (hasBulletInBarrel) 1 else 0))
        }
    }

    fun gunTick(shooter: Entity?, data: GunData, inMainHand: Boolean) {
        init(shooter, data)
        autoReload(shooter, data, inMainHand)
        tickPerk(shooter, data)
        handleCooldown(shooter, data)
        redrawExtraAmmo(shooter, data)

        data.shootAnimationTimer.set(max(data.shootAnimationTimer.get() - 1, 0))
        data.shootTimer.set(max(data.shootTimer.get() - 1, 0))

        if (inMainHand) {
            handleGunBolt(data)

            // 启动换弹
            if (data.reload.reloadStarter.start()) {
                postEvent(ReloadEvent.Pre(shooter, data))
                startReload(shooter, data)
            }

            val soundInfo = data.get(GunProp.SOUND_INFO)
            val sound1p = soundInfo.vehicleReload

            if (data.reload.time() == (if (soundInfo.vehicleReloadSoundTime != 0) Mth.clamp(
                    soundInfo.vehicleReloadSoundTime,
                    1,
                    data.get(GunProp.EMPTY_RELOAD_TIME) - 1
                ) else data.get(GunProp.EMPTY_RELOAD_TIME) - 1)
            ) {
                if (shooter is VehicleEntity) {
                    for (passenger in shooter.getPassengers()) {
                        if (passenger is ServerPlayer) {
                            SoundTool.playLocalSound(passenger, sound1p, 3f, 1f)
                        }
                    }

                    val sound = soundInfo.vehicleReload3p
                    shooter.level().playSound(shooter, shooter.onPos, sound, SoundSource.PLAYERS, 2f, 1f)
                }
            }

            // 减少换弹剩余时间
            data.reload.reduce()

            // 执行换弹期间额外行为
            data.item.reloadTimeBehaviors[data.reload.time()]?.accept(data)

            // 换弹完成
            if (data.reload.time() == 1) {
                finishReload(shooter, data)
            }

            handleGunSingleReload(shooter, data)
            handleSentinelCharge(shooter!!, data)
        }

        if (inMainHand && !data.reloading()) {
            if (data.currentAvailableShots(shooter) <= data.item.hideBulletChainBelowShots()) {
                data.hideBulletChain.set(true)
            }
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                data.item.whenNoAmmo(data)
            }
        }

        data.item.tick(shooter, data, inMainHand)

        data.save()
    }

    private fun startReload(shooter: Entity?, data: GunData) {
        val reload = data.reload

        if (data.item.isOpenBolt(data)) {
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                reload.setTime(data.get(GunProp.EMPTY_RELOAD_TIME) + 1)
                reload.setState(ReloadState.EMPTY_RELOADING)
                playGunEmptyReloadSounds(shooter, data)
            } else {
                reload.setTime(data.get(GunProp.NORMAL_RELOAD_TIME) + 1)
                reload.setState(ReloadState.NORMAL_RELOADING)
                playGunNormalReloadSounds(shooter, data)
            }
        } else {
            reload.setTime(data.get(GunProp.EMPTY_RELOAD_TIME) + 2)
            reload.setState(ReloadState.EMPTY_RELOADING)
            playGunEmptyReloadSounds(shooter, data)
        }
    }

    fun finishGunNormalReload(shooter: Entity?, data: GunData) {
        data.reloadAmmo(shooter, data.item().hasBulletInBarrel(data))
        postEvent(ReloadEvent.Post(shooter, data))
    }

    fun finishGunEmptyReload(shooter: Entity?, data: GunData) {
        data.reloadAmmo(shooter)
        postEvent(ReloadEvent.Post(shooter, data))
    }

    fun playGunEmptyReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadEmpty

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 8f, 1f)
        }
    }

    fun playGunNormalReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadNormal

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 8f, 1f)
        }
    }

    /**
     * 单发装填类的武器换弹流程
     */
    private fun handleGunSingleReload(shooter: Entity?, data: GunData) {
        val stack = data.stack()
        val reload = data.reload

        // 换弹流程计时器
        reload.prepareTimer.reduce()
        reload.prepareLoadTimer.reduce()
        reload.iterativeLoadTimer.reduce()
        reload.finishTimer.reduce()

        // 一阶段
        if (reload.singleReloadStarter.start()) {
            postEvent(ReloadEvent.Pre(shooter, data))

            if (data.get(GunProp.PREPARE_LOAD_TIME) != 0 && (!data.hasEnoughAmmoToShoot(shooter) || stack.`is`(
                    ModItems.SECONDARY_CATACLYSM.get()))) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(shooter, data)
                val prepareLoadTime = data.get(GunProp.PREPARE_LOAD_TIME)
                reload.prepareLoadTimer.set(prepareLoadTime + 1)
            } else if (data.get(GunProp.PREPARE_EMPTY_TIME) != 0 && !data.hasEnoughAmmoToShoot(shooter)) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(shooter, data)
                val prepareEmptyTime = data.get(GunProp.PREPARE_EMPTY_TIME)
                reload.prepareTimer.set(prepareEmptyTime + 1)
            } else {
                playGunPrepareReloadSounds(shooter, data)
                val prepareTime = data.get(GunProp.PREPARE_TIME)
                reload.prepareTimer.set(prepareTime + 1)
            }

            data.forceStop.set(false)
            data.stopped.set(false)
            reload.setStage(1)
            reload.setState(ReloadState.NORMAL_RELOADING)
        }

        if (reload.prepareLoadTimer.get() == data.get(GunProp.PREPARE_AMMO_LOAD_TIME)) {
            prepareLoad(shooter, data)
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((reload.prepareTimer.get() == 1 || reload.prepareLoadTimer.get() == 1)) {
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= data.get(GunProp.MAGAZINE)) {
                reload.stage3Starter.markStart()
            } else {
                reload.setStage(2)
            }
        }

        // 强制停止换弹，进入三阶段
        if (data.forceStop.get() && reload.stage() == 2 && reload.iterativeLoadTimer.get() > 0) {
            data.stopped.set(true)
        }

        // 二阶段
        if ((reload.prepareTimer.get() == 0 || reload.iterativeLoadTimer.get() == 0)
            && reload.stage() == 2 && reload.iterativeLoadTimer.get() == 0 && !data.stopped.get() && data.ammo.get() < data.get(
                GunProp.MAGAZINE
            )
        ) {
            playGunLoopReloadSounds(shooter, data)
            val iterativeTime = data.get(GunProp.ITERATIVE_TIME)
            reload.iterativeLoadTimer.set(iterativeTime)

            // 动画播放nbt
            data.loadIndex.set(if (data.loadIndex.get() == 1) 0 else 1)
        }

        // 装填
        if (data.get(GunProp.ITERATIVE_AMMO_LOAD_TIME) == reload.iterativeLoadTimer.get()) {
            iterativeLoad(shooter, data)
        }

        // 二阶段打断
        if (reload.iterativeLoadTimer.get() == 1) {
            // 装满或备弹耗尽结束
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= data.get(GunProp.MAGAZINE)) {
                reload.setStage(3)
            }

            // 强制结束
            if (data.stopped.get()) {
                reload.setStage(3)
                data.stopped.set(false)
                data.forceStop.set(false)
            }
        }

        // 三阶段
        if ((reload.iterativeLoadTimer.get() == 1 && reload.stage() == 3) || reload.stage3Starter.shouldStart()) {
            reload.setStage(3)
            reload.stage3Starter.finish()

            val finishTime = data.get(GunProp.FINISH_TIME)
            reload.finishTimer.set(finishTime + 2)

            playGunEndReloadSounds(shooter, data)
        }

        if (stack.item === ModItems.MARLIN.get() && reload.finishTimer.get() == 10) {
            data.isEmpty.set(false)
            data.closeStrike.set(false)
        }

        // 三阶段结束
        if (reload.finishTimer.get() == 1) {
            reload.setStage(0)
            if (data.get(GunProp.BOLT_ACTION_TIME) > 0) {
                data.bolt.needed.set(false)
            }
            reload.setState(ReloadState.NOT_RELOADING)
            reload.singleReloadStarter.finish()

            postEvent(ReloadEvent.Post(shooter, data))
        }
    }

    fun prepareLoad(shooter: Entity?, data: GunData) {
        val required = min(data.get(GunProp.MAGAZINE) - data.ammo.get(), 1)
        val available = min(required, data.countBackupAmmo(shooter))
        data.ammo.add(available)

        if (!InventoryTool.hasCreativeAmmoBox(shooter)) {
            data.consumeBackupAmmo(shooter, available)
        }
    }

    fun iterativeLoad(shooter: Entity?, data: GunData) {
        val required = min(
            data.get(GunProp.MAGAZINE) - data.ammo.get(),
            data.get(GunProp.ITERATIVE_LOAD_AMOUNT)
        )
        val available = min(required, data.countBackupAmmo(shooter))
        data.ammo.add(available)

        if (!InventoryTool.hasCreativeAmmoBox(shooter)) {
            data.consumeBackupAmmo(shooter, available)
        }
    }

    fun playGunPrepareReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadPrepare

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 10f, 1f)
        }
    }

    fun playGunEmptyPrepareSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadPrepareEmpty

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 10f, 1f)
        }

        val shooterHeight = shooter.eyePosition.distanceTo(
            Vec3.atLowerCornerOf(
                shooter.level().clip(
                    ClipContext(
                        shooter.eyePosition, shooter.eyePosition.add(Vec3(0.0, -1.0, 0.0).scale(10.0)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter
                    )
                ).blockPos
            )
        )

        com.atsuishio.superbwarfare.Mod.queueServerWork((data.get(GunProp.PREPARE_EMPTY_TIME) / 2.0 + 3 + 1.5 * shooterHeight).toInt()) {
            if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                val ammoType = data.selectedAmmoConsumer().playerAmmoType
                when (ammoType) {
                    Ammo.SHOTGUN -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_SHOTGUN.get(),
                        max(0.75 - 0.12 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    Ammo.SNIPER, Ammo.HEAVY -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_50CAL.get(),
                        max(1 - 0.15 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    else -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_NORMAL.get(),
                        max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                        1f
                    )
                }
            } else {
                SoundTool.playLocalSound(
                    shooter,
                    ModSounds.SHELL_CASING_NORMAL.get(),
                    max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                    1f
                )
            }
        }
    }

    fun playGunPrepareLoadReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadPrepareLoad

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 10f, 1f)
        }

        val shooterHeight = shooter.eyePosition.distanceTo(
            Vec3.atLowerCornerOf(
                shooter.level().clip(
                    ClipContext(
                        shooter.eyePosition, shooter.eyePosition.add(Vec3(0.0, -1.0, 0.0).scale(10.0)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter
                    )
                ).blockPos
            )
        )

        com.atsuishio.superbwarfare.Mod.queueServerWork((8 + 1.5 * shooterHeight).toInt()) {
            if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                val ammoType = data.selectedAmmoConsumer().playerAmmoType
                when (ammoType) {
                    Ammo.SHOTGUN -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_SHOTGUN.get(),
                        max(0.75 - 0.12 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    Ammo.SNIPER, Ammo.HEAVY -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_50CAL.get(),
                        max(1 - 0.15 * shooterHeight, 0.0).toFloat(),
                        1f
                    )

                    else -> SoundTool.playLocalSound(
                        shooter,
                        ModSounds.SHELL_CASING_NORMAL.get(),
                        max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                        1f
                    )
                }
            } else {
                SoundTool.playLocalSound(
                    shooter,
                    ModSounds.SHELL_CASING_NORMAL.get(),
                    max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                    1f
                )
            }
        }
    }

    fun playGunLoopReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadLoop

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 10f, 1f)
        }
    }

    fun playGunEndReloadSounds(shooter: Entity?, data: GunData) {
        if (shooter !is ServerPlayer) return

        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.reloadEnd

        if (sound != null) {
            SoundTool.playLocalSound(shooter, sound, 10f, 1f)
        }

        val shooterHeight = shooter.eyePosition.distanceTo(
            Vec3.atLowerCornerOf(
                shooter.level().clip(
                    ClipContext(
                        shooter.eyePosition, shooter.eyePosition.add(Vec3(0.0, -1.0, 0.0).scale(10.0)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter
                    )
                ).blockPos
            )
        )

        // TODO 为什么要特判这个
        if (data.stack.`is`(ModItems.MARLIN.get())) {
            com.atsuishio.superbwarfare.Mod.queueServerWork((5 + 1.5 * shooterHeight).toInt()) {
                SoundTool.playLocalSound(
                    shooter,
                    ModSounds.SHELL_CASING_NORMAL.get(),
                    max(1.5 - 0.2 * shooterHeight, 0.0).toFloat(),
                    1f
                )
            }
        }
    }

    /**
     * 哨兵充能
     */
    private fun handleSentinelCharge(entity: Entity, data: GunData) {
        // 启动充能
        if (data.charge.starter.start()) {
            data.charge.timer.set(127)

            if (entity is ServerPlayer) {
                SoundTool.playLocalSound(entity, ModSounds.SENTINEL_CHARGE.get(), 2f, 1f)
            }
        }

        data.charge.timer.reduce()
        if (data.charge.timer.get() != 17) return

        val cap = entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
        if (cap.resolve().isEmpty) return
        val itemHandler = cap.resolve().get()

        for (i in 0..<itemHandler.slots) {
            val cell = itemHandler.getStackInSlot(i)
            if (!cell.`is`(ModItems.CELL.get())) continue

            val stackCap = data.stack().getCapability(ForgeCapabilities.ENERGY)
            if (!stackCap.isPresent) continue

            val stackStorage = stackCap.resolve().get()

            val stackMaxEnergy = stackStorage.maxEnergyStored
            val stackEnergy = stackStorage.energyStored

            val cellCap = cell.getCapability(ForgeCapabilities.ENERGY)
            if (!cellCap.isPresent) continue

            val cellStorage = cellCap.resolve().get()
            val cellEnergy = cellStorage.energyStored

            val stackEnergyNeed = min(cellEnergy, stackMaxEnergy - stackEnergy)

            if (cellEnergy > 0) {
                stackStorage.receiveEnergy(stackEnergyNeed, false)
            }
            cellStorage.extractEnergy(stackEnergyNeed, false)
        }
    }

    @SubscribeEvent
    fun onMissingMappings(event: MissingMappingsEvent) {
        for (mapping in event.getAllMappings(Registries.ITEM)) {
            if (com.atsuishio.superbwarfare.Mod.MODID == mapping.getKey().namespace) {
                val item = mapping.getKey().path
                when (item) {
                    "abekiri" -> mapping.remap(ModItems.HOMEMADE_SHOTGUN.get())
                    "m2hb_blueprint" -> mapping.remap(ModItems.M_2_HB_BLUEPRINT.get())
                    "rocket_70" -> mapping.remap(ModItems.SMALL_ROCKET.get())
                    "us_helmet_pastg" -> mapping.remap(ModItems.US_HELMET_PASGT.get())
                    "agm" -> mapping.remap(ModItems.LARGE_ANTI_GROUND_MISSILE.get())
                    "wire_guide_missile" -> mapping.remap(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get())
                    "aurelia_sceptre" -> mapping.remap(ModItems.SUPER_STAR_SHOOTER.get())
                }
            }
        }
    }
}