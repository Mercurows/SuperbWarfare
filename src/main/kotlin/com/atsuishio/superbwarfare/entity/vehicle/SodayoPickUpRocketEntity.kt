package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
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
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f

open class SodayoPickUpRocketEntity(type: EntityType<SodayoPickUpRocketEntity>, level: Level) :
    ArtilleryEntity(type, level) {
    var body1: OBB = OBB(vec3ToVector3d(this.position()), Vector3d(1.1875, 0.5, 3.41), Quaterniond(), OBB.Part.BODY)
    var body2: OBB = OBB(vec3ToVector3d(this.position()), Vector3d(1.1875, 0.375, 0.1875), Quaterniond(), OBB.Part.BODY)
    var body3: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(1.1875, 0.094, 0.34375), Quaterniond(), OBB.Part.BODY)

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
    var turret: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.4765625, 0.3515625, 0.7578125), Quaterniond(), OBB.Part.TURRET)
    var wheelLF: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.25, 0.5, 0.5), Quaterniond(), OBB.Part.WHEEL_LEFT)
    var wheelRF: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.25, 0.5, 0.5), Quaterniond(), OBB.Part.WHEEL_RIGHT)
    var wheelLB: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.25, 0.5, 0.5), Quaterniond(), OBB.Part.WHEEL_LEFT)
    var wheelRB: OBB =
        OBB(vec3ToVector3d(this.position()), Vector3d(0.25, 0.5, 0.5), Quaterniond(), OBB.Part.WHEEL_RIGHT)

    var cooldown: Int = 0

    override fun defineSynchedData() {
        super.defineSynchedData()
        val list = IntArrayList()
        repeat(this.getContainerSize()) {
            list.add(-1)
        }
        this.entityData.define(LOADED_AMMO, list)
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        setChanged()
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.mainHandItem
        val lookingObb = getLookingObb(player, player.getEntityReach())
        val level = this.level()

        if (stack.isEmpty) {
            // 取出炮弹
            player.swing(InteractionHand.MAIN_HAND)
            if (level is ServerLevel && cooldown == 0) {
                for (i in this.barrel.indices) {
                    if (lookingObb === this.barrel[i]) {
                        if (getItems()[i].isEmpty) {
                            return super.interact(player, hand)
                        } else {
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
                            return InteractionResult.SUCCESS
                        }
                    }
                }
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
            return InteractionResult.SUCCESS
        }

        if (cooldown == 0 && !player.isShiftKeyDown && (stack.`is`(ModTags.Items.TOOLS_CROWBAR) || stack.`is`(Items.FLINT_AND_STEEL))) {
            // 发射
            if (lookingAtBarrel(player)) {
                // 精准发射
                for (i in this.barrel.indices) {
                    if (lookingObb === this.barrel[i] && getItems()[i].item is MediumRocketItem) {
                        cooldown = 10
                        shoot(player, i)
                        getItems()[i] = ItemStack.EMPTY
                        setChanged()
                    }
                }
                player.swing(InteractionHand.MAIN_HAND)
                return InteractionResult.SUCCESS
            } else {
                // 顺序发射
                for (i in 0..11) {
                    if (getItems()[i].item is MediumRocketItem) {
                        cooldown = 10
                        shoot(player, i)
                        getItems()[i] = ItemStack.EMPTY
                        setChanged()

                        player.swing(InteractionHand.MAIN_HAND)
                        return InteractionResult.SUCCESS
                    }
                }
            }
        }

        return super.interact(player, hand)
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

    override fun canBind(): Boolean {
        return true
    }

    override fun vehicleShoot(living: LivingEntity?, weaponName: String, targetPos: Vec3?) {
        if (this.isWreck) return
        // 顺序发射
        for (i in 0..11) {
            if (getItems()[i].item is MediumRocketItem && living is Player && cooldown == 0) {
                shoot(living, i)
                cooldown = 3
                getItems()[i] = ItemStack.EMPTY
                setChanged()
            }
        }
    }

    fun shoot(player: Player?, i: Int) {
        val stack = getItems()[i]
        val item = stack.item
        val level = this.level()

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
        for (entity in level.getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            ab
        ) { it !== this }) {
            entity.hurt(causeBurnDamage(entity.level().registryAccess(), player), 30 - 2 * entity.distanceTo(this))
            val force = 4 - 0.7 * entity.distanceTo(this)
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z)
        }

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
        super.baseTick()

        if (decoyInputDown) {
            horn()
        }

        if (cooldown > 0) {
            cooldown--
        }

        this.refreshDimensions()
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
            this.turret,
            this.body1,
            this.body2,
            this.body3,
            this.wheelLB,
            this.wheelRB,
            this.wheelLF,
            this.wheelRF
        )
    }

    override fun updateOBB() {
        val transform = getVehicleTransform(1f)

        val worldPositionBody1 = transformPosition(transform, 0.0, 1.0, 0.593)
        this.body1.center.set(
            Vector3f(
                worldPositionBody1.x.toFloat(),
                worldPositionBody1.y.toFloat(),
                worldPositionBody1.z.toFloat()
            )
        )
        this.body1.updateRotation(combineRotations(1f, this))

        val worldPositionBody2 = transformPosition(transform, 0.0, 1.875, 0.375)
        this.body2.center.set(
            Vector3f(
                worldPositionBody2.x.toFloat(),
                worldPositionBody2.y.toFloat(),
                worldPositionBody2.z.toFloat()
            )
        )
        this.body2.updateRotation(combineRotations(1f, this))

        val worldPositionBody3 = transformPosition(transform, 0.0, 2.15625, 0.9)
        this.body3.center.set(
            Vector3f(
                worldPositionBody3.x.toFloat(),
                worldPositionBody3.y.toFloat(),
                worldPositionBody3.z.toFloat()
            )
        )
        this.body3.updateRotation(combineRotations(1f, this))

        val worldPositionWheelLF = transformPosition(transform, 1.0, 0.5, 2.875)
        this.wheelLF.center.set(
            Vector3f(
                worldPositionWheelLF.x.toFloat(),
                worldPositionWheelLF.y.toFloat(),
                worldPositionWheelLF.z.toFloat()
            )
        )
        this.wheelLF.updateRotation(combineRotations(1f, this))

        val worldPositionWheelRF = transformPosition(transform, -1.0, 0.5, 2.875)
        this.wheelRF.center.set(
            Vector3f(
                worldPositionWheelRF.x.toFloat(),
                worldPositionWheelRF.y.toFloat(),
                worldPositionWheelRF.z.toFloat()
            )
        )
        this.wheelRF.updateRotation(combineRotations(1f, this))

        val worldPositionWheelLB = transformPosition(transform, 1.0, 0.5, -1.28)
        this.wheelLB.center.set(
            Vector3f(
                worldPositionWheelLB.x.toFloat(),
                worldPositionWheelLB.y.toFloat(),
                worldPositionWheelLB.z.toFloat()
            )
        )
        this.wheelLB.updateRotation(combineRotations(1f, this))

        val worldPositionWheelRB = transformPosition(transform, -1.0, 0.5, -1.28)
        this.wheelRB.center.set(
            Vector3f(
                worldPositionWheelRB.x.toFloat(),
                worldPositionWheelRB.y.toFloat(),
                worldPositionWheelRB.z.toFloat()
            )
        )
        this.wheelRB.updateRotation(combineRotations(1f, this))

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

        val worldPositionTurret = transformPosition(transformB, 0.0, 0.0, 0.3740625)
        this.turret.center.set(
            Vector3f(
                worldPositionTurret.x.toFloat(),
                worldPositionTurret.y.toFloat(),
                worldPositionTurret.z.toFloat()
            )
        )
        this.turret.updateRotation(combineRotationsBarrel(1f, this))
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
        val LOADED_AMMO: EntityDataAccessor<List<Int>> = SynchedEntityData.defineId(
            SodayoPickUpRocketEntity::class.java,
            ModSerializers.INT_LIST_SERIALIZER.get()
        )
    }
}
