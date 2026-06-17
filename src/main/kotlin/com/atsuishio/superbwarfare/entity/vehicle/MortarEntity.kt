package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.item.projectile.MortarShellItem
import com.atsuishio.superbwarfare.item.projectile.MortarShellItem.Companion.createShell
import com.atsuishio.superbwarfare.network.message.receive.VehicleShootClientMessage
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.ParticleTool.spawnMediumCannonMuzzleParticles
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.TrajectoryCalculator.calculateLaunchVector
import com.atsuishio.superbwarfare.tools.randomPos
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.items.ItemHandlerHelper

open class MortarEntity(type: EntityType<MortarEntity>, level: Level) : ArtilleryEntity(type, level) {
    private var shooter: LivingEntity? = null
    var intelligent by INTELLIGENT

    constructor(level: Level, yRot: Float) : this(ModEntities.MORTAR.get(), level) {
        this.yRot = yRot
        this.entityData.set(TARGET_YAW, yRot)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(INTELLIGENT, false)
        this.entityData.define(TARGET_PITCH, -70f)
        this.entityData.define(TARGET_YAW, this.yRot)
        this.entityData.define(FIRE_TIME, 0)
        this.entityData.define(NEED_RESET_TARGET, true)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("TargetPitch", this.entityData.get(TARGET_PITCH))
        compound.putFloat("TargetYaw", this.entityData.get(TARGET_YAW))
        compound.putBoolean("Intelligent", this.entityData.get(INTELLIGENT))
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetPitch")) {
            this.entityData.set(TARGET_PITCH, compound.getFloat("TargetPitch"))
        }
        if (compound.contains("TargetYaw")) {
            this.entityData.set(TARGET_YAW, compound.getFloat("TargetYaw"))
        }
        if (compound.contains("Intelligent")) {
            this.entityData.set(INTELLIGENT, compound.getBoolean("Intelligent"))
        }
    }

    override fun vehicleShoot(living: LivingEntity?, weaponName: String, targetPos: Vec3?) {
        if (this.getItems()[0].item !is MortarShellItem) return
        val gunData = getGunData(weaponName) ?: return
        if (entityData.get(FIRE_TIME) != 0) return
        val soundInfo = gunData.get(GunProp.SOUND_INFO)

        this.shooter = living
        this.entityData.set(FIRE_TIME, 25)

        val level = this.level()
        if (!level.isClientSide()) {
            this.level().playSound(
                null,
                this.x,
                this.y,
                this.z,
                soundInfo.vehicleReload,
                SoundSource.PLAYERS,
                1f,
                1f
            )
        }

        if (level is ServerLevel) {
            if (soundInfo.fire3P != null) {
                SoundTool.playDistantSound(
                    level,
                    soundInfo.fire3P!!,
                    position(),
                    (0.25f * gunData.get(GunProp.SOUND_RADIUS)).toFloat(),
                    random.nextFloat() * 0.1f + 1,
                    null
                )
            }
            if (soundInfo.fire3PFar != null) {
                SoundTool.playDistantSound(
                    level,
                    soundInfo.fire3PFar!!,
                    position(),
                    gunData.get(GunProp.SOUND_RADIUS).toFloat(),
                    random.nextFloat() * 0.1f + 1,
                    null
                )
            }
            if (shooter != null) {
                sendPacketToAll(VehicleShootClientMessage(shooter!!.uuid, this.getUUID(), 0, weaponName))
            }
        }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val result = super.interact(player, hand)
        if (result != InteractionResult.PASS) return result

        val stack = player.mainHandItem
        val mainHandItem = stack.item

        if (mainHandItem is MonitorItem && !this.entityData.get(INTELLIGENT)) {
            entityData.set(INTELLIGENT, true)
            if (player is ServerPlayer) {
                player.level()
                    .playSound(null, player.onPos, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1f)
            }
            if (!player.isCreative) {
                stack.shrink(1)
            }
            return InteractionResult.SUCCESS
        }

        if (mainHandItem is MortarShellItem && !player.isShiftKeyDown && this.entityData.get(FIRE_TIME) == 0 && this.getItems()[0].isEmpty) {
            this.getItems()[0] = stack.copyWithCount(1)
            if (!player.isCreative) {
                stack.shrink(1)
            }
            vehicleShoot(player, "Main", targetPos.center)
            entityData.set(NEED_RESET_TARGET, false)
            return InteractionResult.SUCCESS
        }

        if (player.isShiftKeyDown) {
            entityData.set(TARGET_YAW, player.yRot)
        }

        return InteractionResult.FAIL
    }

    override fun onCrowbarInteract(
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (!player.isShiftKeyDown) {
            if (this.getItems()[0].item is MortarShellItem && this.entityData.get(FIRE_TIME) == 0 && level() is ServerLevel) {
                vehicleShoot(player, "Main", targetPos.center)
            }
            return InteractionResult.SUCCESS
        } else {
            if (this.passengers.isNotEmpty()) return null
            if (this.isWreck) {
                return InteractionResult.PASS
            } else {
                for (item in this.getRetrieveItems()) {
                    ItemHandlerHelper.giveItemToPlayer(player, item)
                }
                this.remove(RemovalReason.DISCARDED)
                this.discard()
                return InteractionResult.SUCCESS
            }
        }
    }

    override fun getRetrieveItems(): MutableList<ItemStack> {
        val list = arrayListOf<ItemStack>()

        list.add(ItemStack(ModItems.MORTAR_DEPLOYER.get()))
        if (entityData.get(INTELLIGENT)) {
            list.add(ItemStack(ModItems.MONITOR.get()))
        }

        if (getItems()[0] != ItemStack.EMPTY) {
            list.add(getItems()[0])
        }

        return list
    }

    override fun getEyeHeight(pPose: Pose, pSize: EntityDimensions): Float {
        return 0.2f
    }

    override fun baseTick() {
        super.baseTick()
        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1)
        }

        if (entityData.get(FIRE_TIME) == 5 && this.getItems()[0].item is MortarShellItem) {
            val level = this.level()
            val gunData = getGunData("Main")
            if (level is ServerLevel && gunData != null) {
                val entityToSpawn = createShell(
                    shooter,
                    level,
                    this.getItems()[0],
                    getProjectileGravity("Main"),
                    gunData.get(GunProp.DAMAGE).toFloat(),
                    gunData.get(GunProp.EXPLOSION_DAMAGE).toFloat(),
                    gunData.get(GunProp.EXPLOSION_RADIUS).toFloat()
                )
                entityToSpawn.setPos(this.x, this.eyeY, this.z)
                entityToSpawn.shoot(
                    this.lookAngle.x,
                    this.lookAngle.y,
                    this.lookAngle.z,
                    getProjectileVelocity("Main"),
                    getProjectileSpread("Main")
                )
                entityToSpawn.setLife(gunData.get(GunProp.PROJECTILE_LIFE))
                level.addFreshEntity(entityToSpawn)

                spawnMediumCannonMuzzleParticles(
                    lookAngle,
                    Vec3(this.x, this.eyeY, this.z).add(lookAngle.scale(1.5)),
                    level,
                    this
                )

                this.clearContent()

                if (this.entityData.get(INTELLIGENT) && entityData.get(NEED_RESET_TARGET)) {
                    this.resetTarget("Main")
                }

                entityData.set(NEED_RESET_TARGET, true)

                gunData.shakePlayers(this)
            }
        }

        var f = 0.98f
        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f
        }

        this.deltaMovement = this.deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (this.onGround()) {
            this.deltaMovement = this.deltaMovement.multiply(1.0, -0.9, 1.0)
        }
    }

    override fun setTarget(stack: ItemStack, entity: Entity?, weaponName: String) {
        val parameters = stack.firingParameters
        var canAim = true

        targetPos = parameters.pos
        depressed = !parameters.isDepressed
        radius = parameters.radius
        val randomPos = targetPos.center.randomPos(radius).add(0.0, -1.0, 0.0)
        val flatTrajectory = calculateLaunchVector(
            eyePosition,
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            depressed
        )
        val highTrajectory = calculateLaunchVector(
            eyePosition,
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            !depressed
        )

        var component: Component = Component.literal("")
        val location: Component = Component.translatable("tips.superbwarfare.mortar.position", this.displayName)
            .append(Component.literal(" X:${format0D(x)} Y:${format0D(y)} Z:${format0D(z)} "))
        var angle = xRot

        if (flatTrajectory == null || highTrajectory == null) {
            canAim = false
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range")
        } else {
            angle = -getXRotFromVector(flatTrajectory).toFloat()
            val angle2 = -getXRotFromVector(highTrajectory).toFloat()
            if (angle < -turretMaxPitch || angle > -turretMinPitch) {
                if (angle2 > -turretMaxPitch && angle2 < -turretMinPitch) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn2")
                    canAim = false
                } else {
                    component = Component.translatable("tips.superbwarfare.mortar.warn", this.displayName)
                    if (entity is Player) {
                        entity.displayClientMessage(
                            location.copy().append(component).withStyle(ChatFormatting.RED),
                            false
                        )
                    }
                    return
                }
            }

            if (angle < -turretMaxPitch) {
                component = Component.translatable("tips.superbwarfare.ballistics.warn")
                canAim = false
            }
        }

        if (canAim) {
            this.look(randomPos)
            entityData.set(TARGET_PITCH, angle)
        } else if (entity is Player) {
            entity.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false)
        }
    }

    override fun resetTarget(weaponName: String) {
        val randomPos = targetPos.center.randomPos(radius).add(0.0, -1.0, 0.0)
        val launchVector = calculateLaunchVector(
            eyePosition,
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            depressed
        )
        this.look(randomPos)

        if (launchVector == null) {
            return
        }
        val angle = -getXRotFromVector(launchVector).toFloat()
        if (angle > -turretMaxPitch && angle < -turretMinPitch) {
            entityData.set(TARGET_PITCH, angle)
        }
    }

    fun look(pTarget: Vec3) {
        val vec3 = EntityAnchorArgument.Anchor.EYES.apply(this)
        val d0 = (pTarget.x - vec3.x) * 0.2
        val d2 = (pTarget.z - vec3.z) * 0.2
        entityData.set(TARGET_YAW, Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875).toFloat() - 90f))
    }

    override fun travel() {
        val diffY = Mth.wrapDegrees(entityData.get(TARGET_YAW) - this.yRot)
        val diffX = Mth.wrapDegrees(entityData.get(TARGET_PITCH) - this.xRot)

        this.yRot += (0.5f * diffY).coerceIn(-20f, 20f)
        this.xRot = Mth.clamp(this.xRot + Mth.clamp(0.5f * diffX, -20f, 20f), -turretMaxPitch, -turretMinPitch)
    }

    override fun destroy() {
        val level = this.level()
        if (level is ServerLevel) {
            val x = this.x
            val y = this.y
            val z = this.z
            level.explode(null, x, y, z, 0f, Level.ExplosionInteraction.NONE)
            val mortar = ItemEntity(level, x, (y + 1), z, ItemStack(ModItems.MORTAR_DEPLOYER.get()))
            mortar.setPickUpDelay(10)
            level.addFreshEntity(mortar)
            if (entityData.get(INTELLIGENT)) {
                val monitor = ItemEntity(level, x, (y + 1), z, ItemStack(ModItems.MONITOR.get()))
                monitor.setPickUpDelay(10)
                level.addFreshEntity(monitor)
            }
        }
        super.destroy()
        discard()
    }

    override var maxStackSize = 1

    override fun setChanged() {
        super.setChanged()
        if (!entityData.get(INTELLIGENT)) {
            vehicleShoot(null, "Main", targetPos.center)
        }
    }

    override fun getPickResult(): ItemStack? {
        return ItemStack(ModItems.MORTAR_DEPLOYER.get())
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return super.canPlaceItem(slot, stack) && this.entityData.get(FIRE_TIME) == 0 && stack.item is MortarShellItem
    }

    override fun canBind(): Boolean {
        return this.entityData.get(INTELLIGENT)
    }

    companion object {
        @JvmField
        val FIRE_TIME: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(MortarEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val TARGET_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(MortarEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val TARGET_YAW: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(MortarEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val INTELLIGENT: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(MortarEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val NEED_RESET_TARGET: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(MortarEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
