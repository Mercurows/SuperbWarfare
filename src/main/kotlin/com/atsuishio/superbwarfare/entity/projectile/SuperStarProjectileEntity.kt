package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.plus
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.entity.PartEntity
import kotlin.math.min

open class SuperStarProjectileEntity(type: EntityType<out SuperStarProjectileEntity>, world: Level) :
    FastThrowableProjectile(type, world) {

    private var currentTarget: Entity? = null

    var tickO = 0
    var tick = 0

    init {
        this.noCulling = true
    }

    override fun getDefaultItem(): Item = Items.AIR

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)

        val entity = result.entity
        if (entity === this.owner?.vehicle) return

        if (entity is PartEntity<*>) {
            this.currentTarget = entity.getParent()
        } else {
            this.currentTarget = entity
        }

        this.hitAndSlash(entity)
    }

    private fun hitAndSlash(entity: Entity) {
        val level = level() as? ServerLevel ?: return

        var hitVec = entity.position()

        if (entity is LivingEntity) {
            hitVec = entity.eyePosition
        }

        level.addFreshEntity(PrismaticBoltEntity(level).apply {
            setPos(hitVec.x, hitVec.y, hitVec.z)
        })

        // 命中伤害
        entity.forceHurt(
            ModDamageTypes.causeSuperStarHitDamage(level.registryAccess(), this, this.owner),
            damageValue
        )
        entity.invulnerableTime = 0

        // 斩切伤害
        queueServerWork(2) {
            entity.forceHurt(
                ModDamageTypes.causeSuperStarSlashDamage(level.registryAccess(), this, this.owner),
                explosionDamageValue
            )
            level.playSound(null, entity.onPos, ModSounds.KNIFE_FLESH.get(), SoundSource.PLAYERS, 2f, 1f)

            entity.invulnerableTime = 0

            val player = this.owner
            if (player is ServerPlayer) {
                level.playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
                player.sendPacket(ClientIndicatorMessage(0, 5))
            }
        }
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)

        val resultPos = result.blockPos
        val level = this.level()
        val state = level.getBlockState(resultPos)

        val event = state.block.getSoundType(state, level, resultPos, this).breakSound
        val volume = min(4f, deltaMovement.length().toFloat() / 4f + 0.5f)
        level.playSound(
            null,
            result.getLocation().x,
            result.getLocation().y,
            result.getLocation().z,
            event,
            SoundSource.AMBIENT,
            volume,
            1f
        )
        val hitVec = result.getLocation()

        this.hitBlock(hitVec, result)
        this.discard()
    }

    fun hitBlock(location: Vec3, result: BlockHitResult) {
        val level = this.level() as? ServerLevel ?: return

        val pos = result.blockPos
        val face = result.direction
        val state = level.getBlockState(pos)

        val vx = face.stepX.toDouble()
        val vy = face.stepY.toDouble()
        val vz = face.stepZ.toDouble()
        val dir = Vec3(vx, vy, vz)
        summonVectorParticle(level, state, location, dir)

        this.discard()
        level.playSound(
            null,
            BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
            ModSounds.LAND.get(),
            SoundSource.BLOCKS,
            1f,
            1f
        )
    }

    fun summonVectorParticle(serverLevel: ServerLevel, state: BlockState, pos: Vec3, dir: Vec3) {
        repeat(2) {
            val vec3 = randomVec(dir, 80.0)
            ParticleTool.sendParticle(
                serverLevel,
                ModParticleTypes.WHITE_STAR.get(),
                pos.x,
                pos.y,
                pos.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                0.2 + 0.1 * Math.random(),
                true
            )
        }

        val soundType = state.getSoundType(serverLevel, BlockPos.containing(pos.x, pos.y, pos.z), null)
        if (soundType === SoundType.METAL || soundType === SoundType.ANVIL || soundType === SoundType.CHAIN || soundType === SoundType.COPPER || soundType === SoundType.NETHERITE_BLOCK) {
            serverLevel.playSound(null, pos.x, pos.y, pos.z, ModSounds.HIT.get(), SoundSource.BLOCKS, 2f, 1f)
        }
    }

    fun onHitWater(location: Vec3, result: BlockHitResult) {
        val serverLevel = this.level() as? ServerLevel ?: return

        val pos = result.blockPos
        val face = result.direction
        val state = serverLevel.getBlockState(pos)

        val vx = face.stepX.toDouble()
        val vy = face.stepY.toDouble()
        val vz = face.stepZ.toDouble()

        val dir = Vec3(vx, vy, vz) + deltaMovement.normalize().scale(-0.1)

        if (state.block === Blocks.WATER && !isInWater) {
            val particleData = CustomCloudOption(1f, 1f, 1f, 80, 0.5f, 1f, cooldown = false, light = false)
            repeat(9) { i ->
                val vec3 = randomVec(dir, 40.0)
                ParticleTool.sendParticle(
                    serverLevel,
                    particleData,
                    location.x + 0.12 * i * dir.x,
                    location.y + 0.12 * i * dir.y,
                    location.z + 0.12 * i * dir.z,
                    0,
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    15.0,
                    true
                )
            }

            ParticleTool.spawnBulletHitWaterParticles(serverLevel, location)
            serverLevel.playSound(
                null,
                BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                ModSounds.HIT_WATER.get(),
                SoundSource.BLOCKS,
                1f,
                1f
            )
        }
    }

    fun randomVec(vec3: Vec3, spread: Double): Vec3 = vec3.normalize().add(
        this.random.triangle(0.0, 0.0172275 * spread),
        this.random.triangle(0.0, 0.0172275 * spread),
        this.random.triangle(0.0, 0.0172275 * spread)
    )

    override fun tick() {
        tickO = tick
        super.tick()
        tick++

        val level = this.level()
        if (!level.isClientSide) {
            val startVec = this.position()
            val endVec = startVec.add(this.deltaMovement)
            val fluidResult = ProjectileEntity.rayTraceBlocks(
                level,
                ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this)
            ) { _ -> false }
            this.onHitWater(fluidResult.getLocation(), fluidResult)

            val target = this.currentTarget
            if (target != null && this.boundingBox.intersects(target.boundingBox)) {
                this.hitAndSlash(target)
            }
        } else if (tickCount > 1 && tickCount % 3 == 0) {
            val vec3 = randomVec(deltaMovement, 30.0).normalize().scale(0.4 + 0.05 * Math.random())
            level.addAlwaysVisibleParticle(
                ModParticleTypes.WHITE_STAR.get(),
                true,
                xo,
                yo,
                zo,
                vec3.x,
                vec3.y,
                vec3.z
            )
        }
    }

    fun getLerpTick(tickDelta: Float) = Mth.lerp(tickDelta, tickO.toFloat(), tick.toFloat())

    override fun isFastMoving() = false

    override fun isNoGravity(): Boolean {
        return true
    }
}
