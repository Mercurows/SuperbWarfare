package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.projectile.DestroyableProjectile
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.ai.TowerAI
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.VectorTool.calculateAngle
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import org.joml.Math
import java.util.*

open class AutoAimableEntity(type: EntityType<*>, world: Level) : VehicleEntity(type, world), OwnableEntity {
    init {
        this.noCulling = true
    }

    open var changeTargetTimer: Int = 0

    open var targetUUID by TARGET_UUID
    open var optionalOwnerUUID by OWNER_UUID
    open var active by ACTIVE

    /** 防御塔 AI 系统实例 */
    open val towerAI: TowerAI by lazy { TowerAI(this) }

    /** AI 威胁评分配置，子类可覆盖以定制行为 */
    open val threatConfig: TowerAI.ThreatConfig
        get() = TowerAI.ThreatConfig()

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val res = super.interact(player, hand)
        if (player.isCrouching && !isWreck) {
            if (this.optionalOwnerUUID.isEmpty) {
                ownerUUID = player.getUUID()
            }

            if (this.owner === player) {
                active = !active

                if (player is ServerPlayer) {
                    player.level().playSound(
                        null,
                        player.onPos,
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        0.5f, 1f
                    )
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide())
            } else {
                return InteractionResult.PASS
            }
        }

        targetUUID = ""
        return res
    }

    override fun onCrowbarInteract(
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (!player.isShiftKeyDown || this.isWreck) return null
        if (this.owner != null || player != this.owner) return null
        if (player.level().isClientSide) return null

        val container = ContainerBlockItem.createInstance(this)
        if (!player.addItem(container)) {
            player.drop(container, false)
        }
        this.remove(RemovalReason.DISCARDED)
        this.discard()
        return InteractionResult.SUCCESS
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        with(entityData) {
            define(TARGET_UUID, "")
            define(OWNER_UUID, Optional.empty())
            define(ACTIVE, false)
        }
    }

    open fun setOwnerUUID(pUuid: UUID?) {
        optionalOwnerUUID = Optional.ofNullable(pUuid)
    }

    override fun getOwnerUUID(): UUID? {
        return optionalOwnerUUID.orElse(null)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        compound.putBoolean("Active", active)
        if (optionalOwnerUUID.isPresent) {
            compound.putUUID("Owner", optionalOwnerUUID.get())
        }
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        active = compound.getBoolean("Active")

        var uuid: UUID?
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner")
        } else {
            val s = compound.getString("Owner")

            try {
                val server = this.server
                uuid = if (server == null) {
                    UUID.fromString(s)
                } else {
                    OldUsersConverter.convertMobOwnerIfNecessary(server, s)
                }
            } catch (exception: Exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception)
                uuid = null
            }
        }

        if (uuid != null) {
            try {
                ownerUUID = uuid
            } catch (_: Throwable) {
            }
        }
    }

    override fun baseTick() {
        super.baseTick()
        autoAim()

        val pTeam = owner?.team

        if (pTeam != null && pTeam is PlayerTeam && level() is ServerLevel) {
            level().scoreboard.addPlayerToTeam(this.getStringUUID(), pTeam)
        }
    }

    open fun autoAim() {
        if (isWreck) return
        if (this.getFirstPassenger() != null || !active) {
            return
        }

        val weaponName = "Main"
        val data = getGunData(weaponName) ?: return

        val seekInfo = data().compute().seekInfo ?: return

        val maxSeekRange = seekInfo.maxSeekRange
        val minSeekRange = seekInfo.minSeekRange
        val changeTargetTime = seekInfo.changeTargetTime
        val seekIterative = Math.max(1, seekInfo.seekIterative)
        val minTargetSize = seekInfo.minTargetSize

        if (this.energy < seekInfo.seekEnergyCost) return

        val projectileInfo = data.get(GunProp.PROJECTILE)
        val projectileType = projectileInfo.itemId
        val projectileTypeStr = projectileType.trim().lowercase()
        val rpm = Math.ceil(20f / (vehicleWeaponRpm(weaponName).toFloat() / 60)).toInt()

        if (projectileTypeStr == "ray" && chargeProgress < 1 && energy > data.get(GunProp.AMMO_COST_PER_SHOOT)) {
            val chargeSpeed = 1f / rpm
            chargeProgress = Mth.clamp(chargeProgress + chargeSpeed, 0f, 1f)
        }

        val barrelRootPos = getShootPos(weaponName, 1f)

        // ---- 使用 TowerAI 系统进行目标管理与获取 ----

        // 1. 验证当前目标是否仍有效（含视线检查）
        val targetValid = towerAI.tracker.validateCurrentTarget(barrelRootPos)

        // 2. 尝试获取新目标（含优先切换逻辑）
        if (!targetValid || targetUUID.isEmpty()) {
            towerAI.tracker.acquireTarget(
                barrelRootPos,
                turretMinPitch.toDouble(),
                turretMaxPitch.toDouble(),
                minSeekRange,
                maxSeekRange,
                minTargetSize,
                seekIterative,
            )
        } else {
            // 即使当前目标有效，也定期扫描是否有更高优先级目标
            if (tickCount % seekIterative == 0) {
                towerAI.tracker.acquireTarget(
                    barrelRootPos,
                    turretMinPitch.toDouble(),
                    turretMaxPitch.toDouble(),
                    minSeekRange,
                    maxSeekRange,
                    minTargetSize,
                    seekIterative,
                )
            }
        }

        // 3. 获取当前锁定的目标
        val target = EntityFindUtil.findEntity(level(), targetUUID)
        if (target == null || !target.isAlive) {
            targetUUID = ""
            return
        }

        // 烟雾中丢失目标
        if (!SeekTool.NOT_IN_SMOKE.test(target)) {
            targetUUID = ""
            return
        }

        // 目标骑到了其他载具上则跟踪该载具
        val targetVehicle = target.vehicle
        if (targetVehicle != null) {
            targetUUID = targetVehicle.stringUUID
        }

        // 4. 计算瞄准向量并执行攻击
        val targetPos = target.boundingBox.center
        val targetVel = target.deltaMovement

        val targetVec = if (projectileTypeStr == "ray") {
            barrelRootPos.vectorTo(targetPos).normalize()
        } else {
            calculateFiringSolution(
                barrelRootPos,
                targetPos,
                targetVel.scale(1.1 + random.nextFloat() * 0.2f),
                getProjectileVelocity(weaponName).toDouble(),
                getProjectileGravity(weaponName).toDouble()
            )
        }

        if (laserScale == 0f) {
            turretAutoAimFromVector(targetVec)
            if (calculateAngle(getShootVec(weaponName, 1f), targetVec) < 1) {
                if (checkNoClip(target, barrelRootPos) && !data.overHeat.get()) {
                    if (level() is ServerLevel) {
                        if (projectileTypeStr == "ray" && chargeProgress == 1f) {
                            rayShoot(owner, target, data)
                            changeTargetTimer = 0
                        } else if (getAmmoCount(weaponName) > 0 && tickCount % rpm == 0) {
                            vehicleShoot(owner, "Main", targetPos)
                            changeTargetTimer = 0
                        }
                    }
                } else {
                    // 目标被遮挡，立即清除并让下个 tick 重新索敌
                    towerAI.tracker.clearTarget()
                }
            }
        }

        // 5. 超时切换（保底机制，处理持续无法瞄准的边缘情况）
        if (changeTargetTimer > changeTargetTime) {
            towerAI.tracker.clearTarget()
        }
    }

    @Deprecated(
        message = "Use TowerAI.TeamResolver.isHostile() instead",
        replaceWith = ReplaceWith("TowerAI.TeamResolver.isHostile(this, entity)")
    )
    open fun basicEnemyFilter(entity: Entity): Boolean {
        return TowerAI.TeamResolver.isHostile(this, entity)
    }

    @Deprecated(
        message = "Use TowerAI.TeamResolver.isHostileProjectile() instead",
        replaceWith = ReplaceWith("TowerAI.TeamResolver.isHostileProjectile(this, projectile)")
    )
    open fun basicEnemyProjectileFilter(projectile: Projectile): Boolean {
        return TowerAI.TeamResolver.isHostileProjectile(this, projectile)
    }


    /**
     * 判断具有威胁的弹射物。
     *
     * @deprecated 使用 [TowerAI.TargetValidator.isValidProjectileTarget] 替代（含轨迹威胁分析）
     */
    @Deprecated(
        message = "Use TowerAI.TargetValidator.isValidProjectileTarget() instead",
        replaceWith = ReplaceWith("TowerAI.TargetValidator.isValidProjectileTarget(this, target as Projectile, pos)")
    )
    open fun isThreateningEntity(target: Entity, size: Double, pos: Vec3): Boolean {
        if (target is SmallCannonShellEntity) return false

        if (!target.onGround() && target is Projectile && (target.bbWidth >= size || target.bbHeight >= size)) {
            return checkNoClip(target, pos) && basicEnemyProjectileFilter(target)
        }

        return false
    }

    // 判断载具和目标之间有无障碍物
    open fun checkNoClip(target: Entity, pos: Vec3): Boolean {
        return this.level().clip(
            ClipContext(
                pos, target.boundingBox.center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this
            )
        ).type != HitResult.Type.BLOCK
    }

    open fun rayShoot(living: LivingEntity?, target: Entity, gunData: GunData) {
        val serverLevel = level() as ServerLevel
        ParticleTool.sendParticle(
            serverLevel,
            ParticleTypes.END_ROD,
            target.x, target.eyeY, target.z,
            12,
            0.0, 0.0, 0.0,
            0.05, true
        )
        ParticleTool.sendParticle(
            serverLevel,
            ParticleTypes.LAVA,
            target.x, target.eyeY, target.z,
            4,
            0.0, 0.0, 0.0,
            0.15, true
        )

        val pos = target.boundingBox.center

        laserLength = getShootPos("Main", 1f).distanceTo(pos).toFloat()

        target.forceHurt(
            ModDamageTypes.causeLaserStaticDamage(this.level().registryAccess(), this, living),
            gunData.get(GunProp.DAMAGE).toFloat()
        )
        target.invulnerableTime = 0

        if (gunData.get(GunProp.EXPLOSION_RADIUS) > 0) {
            findNearEntity(pos, gunData, living)
        }

        if (Math.random() < 0.25 && target is LivingEntity) {
            target.remainingFireTicks = 40
        }

        if (target is Projectile && target !is DestroyableProjectile) {
            causeAirExplode(pos)
            target.discard()
        }

        if (!target.isAlive) {
            targetUUID = ""
        }

        laserScale = gunData.get(GunProp.SHOOT_ANIMATION_TIME).toFloat()
        chargeProgress = 0f
        playShootSound3p(living, "Main")

        this.consumeEnergy(gunData.get(GunProp.AMMO_COST_PER_SHOOT))
    }

    fun findNearEntity(vec: Vec3, gunData: GunData, shooter: Entity?) {
        val serverLevel = level() as? ServerLevel ?: return

        val aoeDamage = gunData.get(GunProp.EXPLOSION_DAMAGE)
        val range = gunData.get(GunProp.EXPLOSION_RADIUS)

        val entities = SeekTool.Builder(this)
            .withinRange(vec, range)
            .notItsVehicle()
            .baseFilter()
            .smokeFilter()
            .noVehicle()
            .differentTeam()
            .notFriendly()
            .build()

        for (e in entities) {
            val dis = vec.distanceTo(e.eyePosition)
            var i = 0f
            while (i < dis) {
                val toVec = vec.vectorTo(e.eyePosition).normalize()
                val pos = vec.add(toVec.scale(i.toDouble()))
                ParticleTool.sendParticle(
                    serverLevel,
                    ParticleTypes.END_ROD,
                    pos.x,
                    pos.y,
                    pos.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    true
                )
                i += 0.2f
            }

            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.LAVA,
                e.x,
                e.eyeY,
                e.z,
                4,
                0.0,
                0.0,
                0.0,
                0.15,
                true
            )
            e.forceHurt(
                ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, shooter),
                (aoeDamage - Mth.clamp(dis / range, 0.0, 0.75) * aoeDamage).toFloat()
            )

            if (shooter is ServerPlayer) {
                val holder = Holder.direct(ModSounds.INDICATION.get())
                shooter.connection.send(
                    ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        shooter.x,
                        shooter.y,
                        shooter.z,
                        1f,
                        1f,
                        shooter.level().random.nextLong()
                    )
                )
                shooter.sendPacket(ClientIndicatorMessage(0, 5))
            }
        }
    }

    // TODO 自定义溅射类型（散射or爆炸）
    private fun causeLaserExplode(vec3: Vec3, gunData: GunData, living: Entity?) {
        val radius = gunData.get(GunProp.EXPLOSION_RADIUS).toFloat()

        createCustomExplosion()
            .damage(gunData.get(GunProp.EXPLOSION_DAMAGE).toFloat())
            .radius(radius)
            .attacker(living)
            .position(vec3)
            .explode()
    }

    private fun causeAirExplode(vec3: Vec3) {
        createCustomExplosion()
            .damage(5f)
            .radius(1f)
            .keepBlock()
            .attacker(owner)
            .position(vec3)
            .explode()
    }

    companion object {
        @JvmField
        val ACTIVE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(AutoAimableEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(AutoAimableEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(AutoAimableEntity::class.java, EntityDataSerializers.STRING)

        @JvmStatic
        fun canAim(pos: Vec3, target: Entity, minAngle: Double, maxAngle: Double): Boolean {
            val targetPos = target.boundingBox.center
            val toVec = pos.vectorTo(targetPos).normalize()
            val targetAngle = getXRotFromVector(toVec)
            return minAngle < targetAngle && targetAngle < maxAngle
        }
    }
}
