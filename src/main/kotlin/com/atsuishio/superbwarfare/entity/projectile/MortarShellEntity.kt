package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.SoundTool
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.max
import kotlin.math.sqrt

open class MortarShellEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    enum class Type {
        NORMAL, WP, SMOKE
    }

    private var type: Type? = Type.NORMAL
    // PJM: свист подлёта уже проигран
    private var incomingPlayed = false
    // PJM: точка вылета — чтобы far-петля свиста не играла у ствола и мешалась с выстрелом
    private var launchPos: Vec3? = null
    private var smokeCount: Int = 12
    private var potion: Potion? = Potions.WATER.value()
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set

    init {
        this.damageValue = 60f
        this.explosionDamageValue = 100f
        this.explosionRadiusValue = 8f
        this.gravityValue = 0.13f
    }

    constructor(type: EntityType<out MortarShellEntity>, level: Level) : super(type, level)

    constructor(
        type: EntityType<out MortarShellEntity>,
        x: Double,
        y: Double,
        z: Double,
        level: Level,
        gravity: Float
    ) : super(type, x, y, z, level) {
        this.gravityValue = gravity
    }

    constructor(
        entity: LivingEntity?,
        level: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float
    ) : super(ModEntities.MORTAR_SHELL.get(), entity, level) {
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
        this.gravityValue = 0.13f
    }

    override fun setRGB(rgb: FloatArray) {
        this.red = rgb[0] / 255f
        this.green = rgb[1] / 255f
        this.blue = rgb[2] / 255f
    }

    fun setEffectsFromItem(stack: ItemStack) {
        if (stack.`is`(ModItems.POTION_MORTAR_SHELL.get())) {
            val potionContents =
                stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
            this.potion = potionContents.potion().orElseGet { Potions.WATER }.value()

            for (instance in potionContents.allEffects) {
                this.effectsValue.add(MobEffectInstance(instance))
            }
        } else {
            this.potion = Potions.WATER.value()
            this.effectsValue.clear()
        }
    }

    open fun setType(type: Type?) {
        this.type = type
        if (type == Type.SMOKE) {
            this.damageValue /= 10f
            this.explosionDamageValue /= 10f
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.MORTAR_SHELL.get()
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        if (this.potion != Potions.WATER.value() && this.potion != null) {
            compound.putString(
                "Potion",
                Objects.requireNonNullElse(
                    BuiltInRegistries.POTION.getKey(this.potion!!),
                    "empty"
                ).toString()
            )
        }

        compound.putFloat("RColor", this.red)
        compound.putFloat("GColor", this.green)
        compound.putFloat("BColor", this.blue)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        if (compound.contains("Potion", 8)) {
            val tagName = compound.getString("Potion")
            this.potion = BuiltInRegistries.POTION.get(ResourceLocation.parse(tagName))
        }

        if (compound.contains("RColor")) {
            this.red = compound.getFloat("RColor")
        }
        if (compound.contains("GColor")) {
            this.green = compound.getFloat("GColor")
        }
        if (compound.contains("BColor")) {
            this.blue = compound.getFloat("BColor")
        }
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (this.tickCount <= 1) return
        if (this.owner == null) return

        if (!this.level().isClientSide()) {
            when (type) {
                Type.WP -> this.causeWPEffect(result.getLocation(), this.owner!!)
                Type.SMOKE -> this.releaseSmoke()
                else -> {}
            }

            this.causeExplode(result.getLocation())
            this.createAreaCloud(this.level(), result.getLocation())
        }
        this.discard()
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (!this.level().isClientSide() && this.owner != null) {
            when (type) {
                Type.WP -> causeWPEffect(result.getLocation(), this.owner!!)
                Type.SMOKE -> releaseSmoke()
                else -> {}
            }
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount > 1) {
                causeExplode(result.getLocation())
                this.createAreaCloud(this.level(), position())
            }
        }
        this.discard()
    }

    open fun causeWPEffect(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadiusValue.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            entities.asSequence()
                .filter { it is LivingEntity && !(it is Player && it.isCreative) }
                .forEach {
                    val dis = pos.distanceTo(it.position())
                    if (!checkNoClip(it, pos)) return@forEach
                    (it as LivingEntity).addEffect(
                        MobEffectInstance(
                            ModMobEffects.PHOSPHORUS_FIRE,
                            (300 - 30 * dis).toInt(),
                            max(explosionRadiusValue - dis, 0.0).toInt()
                        ), this.owner
                    )
                }
        }
    }

    open fun releaseSmoke() {
        val level = this.level()
        if (level is ServerLevel) {
            val vec3 = Vec3(1.0, 0.05, 0.0)
            for (i in 0..<this.smokeCount) {
                val decoy = SmokeDecoyEntity(ModEntities.SMOKE_DECOY.get(), level, true)
                    .setColor(this.red, this.green, this.blue)
                decoy.setPos(this.x, this.y + bbHeight, this.z)
                decoy.decoyShoot(this, vec3.yRot(i * (360f / this.smokeCount) * Mth.DEG_TO_RAD), 2f, 5f)
                level.addFreshEntity(decoy)
            }
        }
    }

    override fun tick() {
        val level = this.level()
        // PJM: запоминаем точку вылета на первом тике (работает и на клиенте, где играет FlySound)
        if (launchPos == null) launchPos = position()
        if (tickCount > this.getLife()) {
            if (level is ServerLevel) {
                this.createAreaCloud(level, position())
            }
        }

        super.tick()
        if (deltaMovement.lengthSqr() > 25) {
            mediumTrail()
        }

        // PJM: одноразовый свист подлёта. Считаем РЕАЛЬНОЕ время до земли с учётом ускорения
        // (toGround = v·t + ½·g·t²), а не линейно toGround/v — линейная оценка сразу за апогеем,
        // пока v мал, огромна, поэтому порог срабатывал лишь у земли и вой (~4с, кульминация в
        // конце) звучал уже после попадания. Ведём старт так, чтобы вой закончился к удару.
        // Источник ставим на землю у точки падения, чтобы наземные игроки слышали его без затухания сверху.
        if (level is ServerLevel && !incomingPlayed && deltaMovement.y < 0) {
            val groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ)
            val toGround = this.y - groundY
            if (toGround > 0) {
                val v = -deltaMovement.y
                val g = getCustomGravity().toDouble()
                val ticksToImpact = (-v + sqrt(v * v + 2 * g * toGround)) / g
                if (ticksToImpact < INCOMING_LEAD_TICKS) {
                    incomingPlayed = true
                    // radius играет роль громкости: слышно до ~radius*16 блоков, а SoundClientMessage
                    // ещё и задерживает звук на distance/17 тиков — дальние слышат тот же свист тише и
                    // позже (это и есть «далёкий» incoming). 9.4 → ~150 блоков.
                    // Источник — ПРОГНОЗИРУЕМАЯ точка падения (трение в воздухе = 1, горизонтальная
                    // скорость постоянна): в момент старта воя мина ещё за сотни блоков от цели,
                    // и свист под её текущей позицией у цели было не слышно вовсе.
                    val impactX = this.x + deltaMovement.x * ticksToImpact
                    val impactZ = this.z + deltaMovement.z * ticksToImpact
                    val impactY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING,
                        Mth.floor(impactX), Mth.floor(impactZ)
                    ).toDouble()
                    SoundTool.playDistantSound(
                        level, ModSounds.MORTAR_INCOMING.get(),
                        Vec3(impactX, impactY, impactZ),
                        INCOMING_RADIUS, random.nextFloat() * 0.1f + 0.95f, null
                    )
                }
            }
        }

        if (type == Type.WP) {
            val hitResult = level().clip(
                ClipContext(
                    position(),
                    position().add(deltaMovement.scale(8.0)),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.ANY,
                    this
                )
            )

            if (hitResult.type == HitResult.Type.BLOCK) {
                releaseWp(owner)
            }
        }
    }

    private fun releaseWp(shooter: Entity?) {
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumExplosionParticles(level, position())
            repeat(31) {
                val whitePhosphorusProjectileEntity = WhitePhosphorusProjectileEntity(shooter, level)

                whitePhosphorusProjectileEntity.setPos(position().x, position().y, position().z)
                whitePhosphorusProjectileEntity.shoot(
                    deltaMovement.x,
                    deltaMovement.y,
                    deltaMovement.z,
                    (random.nextFloat() * 0.05f + 0.1f * deltaMovement.length()).toFloat(),
                    35f
                )
                level.addFreshEntity(whitePhosphorusProjectileEntity)
            }
            discard()
        }
    }

    open fun createAreaCloud(level: Level, pos: Vec3) {
        if (this.potion === Potions.WATER.value() && this.getEffects().isEmpty()) return

        val cloud = AreaEffectCloud(level, pos.x, pos.y, pos.z)
        if (this.getEffects().isNotEmpty()) {
            this.getEffects().forEach { cloud.addEffect(it) }
        }

        cloud.duration = this.explosionDamageValue.toInt()
        cloud.radius = this.explosionRadiusValue
        val owner = this.owner
        if (owner is LivingEntity) {
            cloud.owner = owner
        }
        level.addFreshEntity(cloud)
    }

    override fun getSound(): SoundEvent {
        // PJM: сквадовская петля свиста мины вместо генерического shell_fly
        return ModSounds.MORTAR_INCOMING_LOOP.get()
    }

    override fun getVolume(): Float {
        // PJM: far-петля свиста играет только на НИСХОДЯЩЕЙ ветке (фаза подлёта) и вдали от точки
        // вылета. Вся восходящая ветка у ствола молчит — иначе петля мешается со звуком выстрела рядом.
        val lp = launchPos ?: return 0f
        if (deltaMovement.y >= 0 || this.distanceToSqr(lp) < FAR_LOOP_MIN_DIST_SQ) return 0f
        return 1.2f
    }

    override fun forceLoadChunk(): Boolean {
        return true
    }

    companion object {
        // PJM: за сколько тиков до попадания стартует свист подлёта. Клип ~4с = ~80 тиков; берём
        // чуть меньше, чтобы кульминация совпала с ударом. Крутить, если вой звучит рано/поздно.
        private const val INCOMING_LEAD_TICKS = 75

        // PJM: радиус слышимости свиста подлёта (~radius*16 = ~150 блоков)
        private const val INCOMING_RADIUS = 9.4f

        // PJM: в этом радиусе от точки вылета far-петля молчит (не мешать выстрелу). Квадрат 50 бл.
        private const val FAR_LOOP_MIN_DIST_SQ = 50.0 * 50.0
    }
}
