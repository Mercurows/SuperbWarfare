package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.calculateAngle
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getSubmergedHeight
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeBurnDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSerializers
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.projectile.MediumRocketItem
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.OBB.Companion.getLookingObb
import com.atsuishio.superbwarfare.tools.OBB.Companion.vec3ToVector3d
import com.atsuishio.superbwarfare.tools.OBB.Companion.vector3dToVec3
import com.atsuishio.superbwarfare.tools.ParticleTool.spawnMediumCannonMuzzleParticles
import com.atsuishio.superbwarfare.tools.VectorTool.combineRotations
import com.atsuishio.superbwarfare.tools.VectorTool.combineRotationsBarrel
import com.atsuishio.superbwarfare.tools.VectorTool.combineRotationsTurret
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Math
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

open class Type63Entity(type: EntityType<Type63Entity>, level: Level) : VehicleEntity(type, level) {
    var barrel: Array<OBB> = arrayOf(
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        ),
        OBB(
            vec3ToVector3d(this.position()),
            Vector3d(0.09375, 0.09375, 0.0625),
            Quaterniond(),
            OBB.Part.INTERACTIVE
        )
    )
    var pitchController: OBB = OBB(
        vec3ToVector3d(this.position()),
        Vector3d(0.15625, 0.21875, 0.21875),
        Quaterniond(),
        OBB.Part.INTERACTIVE
    )
    var yawController: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.125, 0.125, 0.125), Quaterniond(), OBB.Part.INTERACTIVE)
    var hoe1: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.125, 0.125, 0.875), Quaterniond(), OBB.Part.INTERACTIVE)
    var hoe2: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.125, 0.125, 0.875), Quaterniond(), OBB.Part.INTERACTIVE)
    var wheel1: OBB = OBB(
        vec3ToVector3d(this.position()),
        Vector3d(0.125, 0.390625, 0.390625),
        Quaterniond(),
        OBB.Part.WHEEL_LEFT
    )
    var wheel2: OBB = OBB(
        vec3ToVector3d(this.position()),
        Vector3d(0.125, 0.390625, 0.390625),
        Quaterniond(),
        OBB.Part.WHEEL_RIGHT
    )
    var body1: OBB = OBB(
        vec3ToVector3d(this.position()),
        Vector3d(0.4765625, 0.3515625, 0.7578125),
        Quaterniond(),
        OBB.Part.BODY
    )
    var body2: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.771875, 0.109375, 0.296875), Quaterniond(), OBB.Part.BODY)

    var interactionTick: Double = 0.0
    var cooldown: Int = 0

    override fun canBeCollidedWith(): Boolean {
        return true
    }

    override fun playerTouch(pPlayer: Player) {
        if (this.position().distanceTo(pPlayer.position()) > 1.4
            || pPlayer === this.getFirstPassenger()
            || pPlayer.position().y > position().y
            || !pPlayer.isShiftKeyDown
        ) return
        if (!this.level().isClientSide && pPlayer.y < this.y + this.bbHeight
            && pPlayer.y + pPlayer.bbHeight > this.y
        ) {
            val entitySize = (pPlayer.bbWidth * pPlayer.bbHeight).toDouble()
            val thisSize = (this.bbWidth * this.bbHeight).toDouble()
            val f = Math.min(entitySize / thisSize, 2.0)
            this.deltaMovement = this.deltaMovement.add(
                Vec3(
                    pPlayer.position().vectorTo(this.position()).toVector3f()
                ).scale(0.5 * f * pPlayer.deltaMovement.length())
            )
            this.yRot = pPlayer.getYHeadRot()
        }
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        val list = IntArrayList()
        repeat(this.getContainerSize()) {
            list.add(-1)
        }

        this.entityData.define(TARGET_PITCH, 0f)
        this.entityData.define(TARGET_YAW, 0f)
        this.entityData.define(BODY_YAW, 0f)
        this.entityData.define(SHOOT_PITCH, 0f)
        this.entityData.define(SHOOT_YAW, 0f)
        this.entityData.define(LOADED_AMMO, list)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("Pitch", this.entityData.get(TARGET_PITCH))
        compound.putFloat("Yaw", this.entityData.get(TARGET_YAW))
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        this.entityData.set(TARGET_PITCH, compound.getFloat("Pitch"))
        this.entityData.set(TARGET_YAW, compound.getFloat("Yaw"))
        setChanged()
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val result = super.interact(player, hand)
        if (result != InteractionResult.PASS) return result

        val stack = player.mainHandItem
        val lookingObb = getLookingObb(player, player.getEntityReach())
        val level = this.level()

        if (stack.isEmpty) {
            if (player.isShiftKeyDown) {
                if (lookingObb === hoe1) {
                    if (level is ServerLevel) {
                        entityData.set(BODY_YAW, entityData.get(BODY_YAW) + 0.2f * interactionTick.toFloat())
                        interactionTick++
                        if (cooldown == 0) {
                            cooldown = 6
                            val vec3 = vector3dToVec3(hoe1.center)
                            level.playSound(
                                null,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                ModSounds.WHEEL_VEHICLE_STEP.get(),
                                SoundSource.PLAYERS,
                                0.5f,
                                random.nextFloat() * 0.05f + 0.975f
                            )
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND)
                }

                if (lookingObb === hoe2) {
                    if (level is ServerLevel) {
                        entityData.set(
                            BODY_YAW,
                            entityData.get(BODY_YAW) - 0.2f * interactionTick.toFloat()
                        )
                        interactionTick++
                        if (cooldown == 0) {
                            cooldown = 6
                            val vec3 = vector3dToVec3(hoe1.center)
                            level.playSound(
                                null,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                ModSounds.WHEEL_VEHICLE_STEP.get(),
                                SoundSource.PLAYERS,
                                0.5f,
                                random.nextFloat() * 0.05f + 0.975f
                            )
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND)
                }
            } else {
                // 取出炮弹
                player.swing(InteractionHand.MAIN_HAND)

                if (level is ServerLevel && cooldown == 0) {
                    for (i in this.barrel.indices) {
                        if (lookingObb === this.barrel[i] && !getItems()[i].isEmpty) {
                            player.addItem(getItems()[i].copyWithCount(1))
                            val vec3 = vector3dToVec3(this.barrel[i].center)
                            level.playSound(
                                null,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                ModSounds.TYPE_63_RELOAD.get(),
                                SoundSource.PLAYERS,
                                1f,
                                random.nextFloat() * 0.1f + 0.9f
                            )
                            cooldown = 5
                            getItems()[i] = ItemStack.EMPTY
                            setChanged()
                        }
                    }
                }
            }

            if (lookingObb === yawController) {
                interactEvent(vector3dToVec3(yawController.center))
                entityData.set(
                    TARGET_YAW,
                    Mth.clamp(
                        entityData.get(TARGET_YAW) + (if (player.isShiftKeyDown) -0.02f else 0.02f) * interactionTick.toFloat(),
                        -turretMaxYaw,
                        -turretMinYaw
                    )
                )
                player.swing(InteractionHand.MAIN_HAND)
            }

            if (lookingObb === pitchController) {
                interactEvent(vector3dToVec3(pitchController.center))
                entityData.set(
                    TARGET_PITCH,
                    Mth.clamp(
                        entityData.get(TARGET_PITCH) + (if (player.isShiftKeyDown) 0.02f else -0.02f) * interactionTick.toFloat(),
                        -turretMaxPitch,
                        -turretMinPitch
                    )
                )
                player.swing(InteractionHand.MAIN_HAND)
            }
        }

        if (stack.item is MediumRocketItem) {
            for (i in this.barrel.indices) {
                if (lookingObb === this.barrel[i] && getItems()[i].isEmpty && level is ServerLevel && cooldown == 0) {
                    this.setItem(i, stack.copyWithCount(1))
                    if (!player.isCreative) {
                        stack.shrink(1)
                    }
                    val vec3 = vector3dToVec3(this.barrel[i].center)
                    level.playSound(
                        null,
                        vec3.x,
                        vec3.y,
                        vec3.z,
                        ModSounds.TYPE_63_RELOAD.get(),
                        SoundSource.PLAYERS,
                        1f,
                        random.nextFloat() * 0.1f + 0.9f
                    )
                    cooldown = 5
                    setChanged()
                }
                player.swing(InteractionHand.MAIN_HAND)
            }
        }

        if (cooldown == 0 && (stack.`is`(ModTags.Items.TOOLS_CROWBAR) || stack.`is`(Items.FLINT_AND_STEEL))) {
            // 发射
            if (lookingAtBarrel(player)) {
                // 精准发射
                for (i in this.barrel.indices) {
                    if (lookingObb === this.barrel[i] && getItems()[i].item is MediumRocketItem) {
                        shoot(player, i)
                        getItems()[i] = ItemStack.EMPTY
                        setChanged()
                    }
                }

                player.swing(InteractionHand.MAIN_HAND)
            } else {
                // 顺序发射
                for (i in 0..11) {
                    if (getItems()[i].item is MediumRocketItem) {
                        shoot(player, i)
                        getItems()[i] = ItemStack.EMPTY
                        setChanged()

                        player.swing(InteractionHand.MAIN_HAND)
                        return InteractionResult.SUCCESS
                    }
                }
            }
        }

        return InteractionResult.FAIL
    }

    fun lookingAtBarrel(player: Player): Boolean {
        val lookingObb = getLookingObb(player, player.getEntityReach())

        for (i in 0..11) {
            if (lookingObb === barrel[i]) {
                return true
            }
        }

        return false
    }

    fun interactEvent(vec3: Vec3) {
        val level = this.level()
        if (level is ServerLevel) {
            interactionTick++
            if (cooldown <= 0) {
                cooldown = 6
                level.playSound(
                    null,
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    ModSounds.HAND_WHEEL_ROT.get(),
                    SoundSource.PLAYERS,
                    1f,
                    random.nextFloat() * 0.05f + 0.975f
                )
            }
        }
    }

    fun shoot(player: Player?, i: Int) {
        val stack = getItems()[i]
        val item = stack.item

        if (item !is MediumRocketItem) {
            return
        }

        val gunData = getGunData(item.type.toString()) ?: return

        val shootVelocity = getProjectileVelocity(gunData)
        val shootSpread = getProjectileSpread(gunData)
        val shootGravity = getProjectileGravity(gunData)

        val obb = this.barrel[i]
        val shootPos = vector3dToVec3(obb.center)

        val entityToSpawn = MediumRocketEntity(
            ModEntities.MEDIUM_ROCKET.get(),
            shootPos.x,
            shootPos.y,
            shootPos.z,
            level(),
            gunData.get(GunProp.DAMAGE).toFloat(),
            gunData.get(GunProp.EXPLOSION_RADIUS).toFloat(),
            gunData.get(GunProp.EXPLOSION_DAMAGE).toFloat(),
            0f,
            0,
            item.type,
            gunData.get(GunProp.SPREAD_AMOUNT),
            gunData.get(GunProp.SPREAD_ANGLE)
        )
        entityToSpawn.durability(gunData.get(GunProp.AP_DURABILITY))
        entityToSpawn.setCustomGravity(shootGravity)
        entityToSpawn.owner = player

        val barrelVector = getBarrelVector(1f)
        entityToSpawn.shoot(barrelVector.x, barrelVector.y, barrelVector.z, shootVelocity, shootSpread)
        level().addFreshEntity(entityToSpawn)

        val sound = gunData.get(GunProp.SOUND_INFO).fire3P
        if (sound != null) {
            level().playSound(
                null,
                shootPos.x,
                shootPos.y,
                shootPos.z,
                sound,
                SoundSource.PLAYERS,
                gunData.get(GunProp.SOUND_RADIUS).toFloat(),
                random.nextFloat() * 0.1f + 0.95f
            )
        }

        val ab = AABB(boundingBox.center, boundingBox.center).inflate(0.75)
            .move(barrelVector.scale(-2.0)).expandTowards(barrelVector.scale(-5.0))

        // 尾焰
        for (entity in level().getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            ab
        ) { it !== this }) {
            entity.hurt(causeBurnDamage(entity.level().registryAccess(), player), 30 - 2 * entity.distanceTo(this))
            val force = 4 - 0.7 * entity.distanceTo(this)
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z)
        }

        cooldown = 10
        val level = this.level()
        if (level is ServerLevel) {
            spawnMediumCannonMuzzleParticles(
                barrelVector.scale(-1.0),
                shootPos.add(barrelVector.scale(-0.5)),
                level,
                this
            )
            spawnMediumCannonMuzzleParticles(
                barrelVector.scale(-1.0),
                shootPos.add(barrelVector.scale(-1.5)),
                level,
                this
            )
            spawnMediumCannonMuzzleParticles(barrelVector, shootPos.add(barrelVector.scale(1.5)), level, this)
        }

        gunData.shakePlayers(this)
    }

    override fun baseTick() {
        turretYRotO = this.turretYRot
        turretXRotO = this.turretXRot
        leftWheelRotO = this.leftWheelRot
        rightWheelRotO = this.rightWheelRot

        super.baseTick()

        val fluidFloat = 0.052 * getSubmergedHeight(this)
        this.setDeltaMovement(this.deltaMovement.add(0.0, fluidFloat, 0.0))

        if (this.onGround()) {
            val f0 = 0.35f + 0.5f * abs(90 - calculateAngle(this.deltaMovement, this.getViewVector(1f)).toFloat()) / 90
            this.setDeltaMovement(
                this.deltaMovement
                    .add(this.getViewVector(1f).normalize().scale(0.05 * deltaMovement.dot(getViewVector(1f))))
            )
            this.setDeltaMovement(this.deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble()))
        } else {
            this.setDeltaMovement(this.deltaMovement.multiply(0.99, 0.99, 0.99))
        }

        if (this.isInWater) {
            val f1 = (0.7f - (0.04f * Math.min(
                getSubmergedHeight(this),
                this.bbHeight.toDouble()
            )) + 0.08f * abs(
                90 - calculateAngle(this.deltaMovement, this.getViewVector(1f)).toFloat()
            ) / 90).toFloat()
            this.deltaMovement = this.deltaMovement.add(
                this.getViewVector(1f).normalize().scale(0.04 * deltaMovement.dot(getViewVector(1f)))
            )
            this.deltaMovement = this.deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble())
        }

        if (cooldown > 0) {
            cooldown--
        }

        interactionTick *= 0.94

        if (level() is ServerLevel) {
            entityData.set(SHOOT_PITCH, getXRotFromVector(getBarrelVector(1f)).toFloat())
            entityData.set(SHOOT_YAW, -getYRotFromVector(getBarrelVector(1f)).toFloat())
        }

        entityData.set(BODY_YAW, entityData.get(BODY_YAW) * 0.8f)
        yRot += entityData.get(BODY_YAW)

        this.refreshDimensions()
    }

    override fun travel() {
        val diffY = entityData.get(TARGET_YAW) - turretYRot
        this.turretYRot = Mth.clamp(this.turretYRot + 0.1f * diffY, -turretMaxYaw, -turretMinYaw)

        val diffX = entityData.get(TARGET_PITCH) - turretXRot
        this.turretXRot = Mth.clamp(this.turretXRot + 0.1f * diffX, -turretMaxPitch, -turretMinPitch)

        val s0 = deltaMovement.dot(this.getViewVector(1f))

        this.leftWheelRot = (this.leftWheelRot - 1.167 * s0).toFloat()
        this.rightWheelRot = (this.rightWheelRot - 1.167 * s0).toFloat()
    }

    fun getShootPos(pPartialTicks: Float): Vec3 {
        val transform = getBarrelTransform(pPartialTicks)
        val rootPosition = transformPosition(transform, 0.0, 0.000625, -0.44625)
        return Vec3(rootPosition.x, rootPosition.y, rootPosition.z)
    }

    override var maxStackSize: Int = 1

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return false
    }

    override fun getOBBs(): MutableList<OBB> {
        return mutableListOf(
            this.barrel[0],
            this.barrel[1],
            this.barrel[2],
            this.barrel[3],
            this.barrel[4],
            this.barrel[5],
            this.barrel[6],
            this.barrel[7],
            this.barrel[8],
            this.barrel[9],
            this.barrel[10],
            this.barrel[11],
            this.hoe1,
            this.hoe2,
            this.yawController,
            this.pitchController,
            this.wheel1,
            this.wheel2,
            this.body1,
            this.body2
        )
    }

    override fun updateOBB() {
        val transform = getVehicleTransform(1f)

        // 驻锄位置
        val worldPosition = transformPosition(transform, 0.875, 0.1875, -1.625)
        this.hoe1.center.set(Vector3f(worldPosition.x.toFloat(), worldPosition.y.toFloat(), worldPosition.z.toFloat()))
        this.hoe1.updateRotation(combineRotations(1f, this))

        val worldPosition2 = transformPosition(transform, -0.875, 0.1875, -1.625)
        this.hoe2.center.set(
            Vector3f(
                worldPosition2.x.toFloat(),
                worldPosition2.y.toFloat(),
                worldPosition2.z.toFloat()
            )
        )
        this.hoe2.updateRotation(combineRotations(1f, this))

        val worldPositionW = transformPosition(transform, 0.90625, 0.390625, 0.1071875)
        this.wheel1.center.set(
            Vector3f(
                worldPositionW.x.toFloat(),
                worldPositionW.y.toFloat(),
                worldPositionW.z.toFloat()
            )
        )
        this.wheel1.updateRotation(combineRotations(1f, this))

        val worldPositionW2 = transformPosition(transform, -0.90625, 0.390625, 0.1071875)
        this.wheel2.center.set(
            Vector3f(
                worldPositionW2.x.toFloat(),
                worldPositionW2.y.toFloat(),
                worldPositionW2.z.toFloat()
            )
        )
        this.wheel2.updateRotation(combineRotations(1f, this))

        val worldPositionBody2 = transformPosition(transform, 0.0, 0.42546875, -0.090625)
        this.body2.center.set(
            Vector3f(
                worldPositionBody2.x.toFloat(),
                worldPositionBody2.y.toFloat(),
                worldPositionBody2.z.toFloat()
            )
        )
        this.body2.updateRotation(combineRotationsBarrel(1f, this))

        val transformT = getTurretTransform(1f)

        val worldPositionYaw = transformPosition(transformT, 0.62625, 0.0396875, -0.5)
        this.yawController.center.set(
            Vector3f(
                worldPositionYaw.x.toFloat(),
                worldPositionYaw.y.toFloat(),
                worldPositionYaw.z.toFloat()
            )
        )
        this.yawController.updateRotation(combineRotationsTurret(1f, this))

        val worldPositionPitch = transformPosition(transformT, 0.7825, 0.5771875, -0.024375)
        this.pitchController.center.set(
            Vector3f(
                worldPositionPitch.x.toFloat(),
                worldPositionPitch.y.toFloat(),
                worldPositionPitch.z.toFloat()
            )
        )
        this.pitchController.updateRotation(combineRotationsTurret(1f, this))

        val transformB = getBarrelTransform(1f)

        val i = 0.24375

        setBarrelOBB(0, -0.3659375, 0.244375)
        setBarrelOBB(1, -0.3659375 + i, 0.244375)
        setBarrelOBB(2, -0.3659375 + 2 * i, 0.244375)
        setBarrelOBB(3, -0.3659375 + 3 * i, 0.244375)
        setBarrelOBB(4, -0.3659375, 0.244375 - i)
        setBarrelOBB(5, -0.3659375 + i, 0.244375 - i)
        setBarrelOBB(6, -0.3659375 + 2 * i, 0.244375 - i)
        setBarrelOBB(7, -0.3659375 + 3 * i, 0.244375 - i)
        setBarrelOBB(8, -0.3659375, 0.244375 - 2 * i)
        setBarrelOBB(9, -0.3659375 + i, 0.244375 - 2 * i)
        setBarrelOBB(10, -0.3659375 + 2 * i, 0.244375 - 2 * i)
        setBarrelOBB(11, -0.3659375 + 3 * i, 0.244375 - 2 * i)

        val worldPositionBody1 = transformPosition(transformB, 0.0, 0.0, 0.3740625)
        this.body1.center.set(
            Vector3f(
                worldPositionBody1.x.toFloat(),
                worldPositionBody1.y.toFloat(),
                worldPositionBody1.z.toFloat()
            )
        )
        this.body1.updateRotation(combineRotationsBarrel(1f, this))
    }

    private fun setBarrelOBB(index: Int, x: Double, y: Double) {
        val vec = transformPosition(getBarrelTransform(1f), x, y, -0.44625)
        this.barrel[index].center.set(Vector3f(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat()))
        this.barrel[index].updateRotation(combineRotationsBarrel(1f, this))
    }

    override fun setChanged() {
        super.setChanged()
        val list = arrayListOf<Int>()
        for (item in this.getItems()) {
            val i = item.item
            if (i is MediumRocketItem) {
                list.add(i.type.ordinal)
            } else {
                list.add(-1)
            }
        }
        this.entityData.set(LOADED_AMMO, list)
    }

    companion object {
        @JvmField
        val TARGET_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Type63Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val TARGET_YAW: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Type63Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val BODY_YAW: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Type63Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SHOOT_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Type63Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SHOOT_YAW: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Type63Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LOADED_AMMO: EntityDataAccessor<List<Int>> = SynchedEntityData.defineId(
            Type63Entity::class.java,
            ModSerializers.INT_LIST_SERIALIZER.get()
        )
    }
}
