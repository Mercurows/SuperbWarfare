package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.randomPos
import com.atsuishio.superbwarfare.tools.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class HappiestGhastEntity(type: EntityType<HappiestGhastEntity>, world: Level) : ArtilleryEntity(type, world) {
    open var wasOpen = false

    override fun baseTick() {
        super.baseTick()

        val doorOpen = getWeaponIndex(0) == 2

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (doorOpen && !wasOpen) {
                ctx.playAnimation("animation.door.open", AnimationPlayType.LOOP,
                    fadeInTicks = 40)
                level().playLocalSound(boundingBox.center.toBlockPos(), ModSounds.HAPPIEST_GHAST_DOOR_OPEN.get(), SoundSource.AMBIENT, 1F, 1F, false)
            } else if (!doorOpen && wasOpen) {
                ctx.stopAnimation("animation.door.open",
                    fadeOutTicks = 30)
                level().playLocalSound(boundingBox.center.toBlockPos(), ModSounds.HAPPIEST_GHAST_DOOR_CLOSE.get(), SoundSource.AMBIENT, 1F, 1F, false)
            }
            wasOpen = doorOpen
        }
    }

    fun missileLaunchEffect(serverLevel: ServerLevel, pos: Vec3, direct: Vec3) {
        for (i in 0..10) {
            val s = 0 + 0.03 * i
            val position = pos.add(direct.scale(1.2 * i))
            Mod.queueServerWork(i) {
                ParticleTool.sendParticle(
                    serverLevel,
                    ParticleTypes.CLOUD,
                    position.x,
                    position.y,
                    position.z,
                    (1 + 1.3 * i).toInt(),
                    s,
                    s,
                    s,
                    0.002 * i,
                    true
                )
            }
        }

        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(1f, 1f, 1f, 45, 0.88f, 2, 0.05f),
            direct,
            pos.add(direct.scale(3.5)),
            0.15
        )
        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(1f, 1f, 1f, 47, 0.90f, 2, 0.03f),
            direct,
            pos.add(direct.scale(3.5)),
            0.125
        )
        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(1f, 1f, 1f, 48, 0.92f, 2, 0.01f),
            direct,
            pos.add(direct.scale(3.5)),
            0.1
        )
    }

    override fun setTarget(stack: ItemStack, entity: Entity?, weaponName: String) {
        val parameters = stack.firingParameters
        radius = parameters.radius
        originPos = parameters.pos
        val randomPos = originPos.center.randomPos(radius).add(0.0, -1.0, 0.0)
        targetPos = BlockPos.containing(randomPos)
    }

    override fun resetTarget(weaponName: String) {
        if (this.isWreck) return
        val randomPos = originPos.center.randomPos(radius).add(0.0, -1.0, 0.0)
        targetPos = BlockPos.containing(randomPos)
    }

    override fun beforeShoot(living: LivingEntity?) {
        val serverLevel = level()
        if (serverLevel is ServerLevel) {
            val name = this.getGunName(0)
            if (name == "Main" || name == "AAMissile") {
                val pos = getShootPos(name, 1f)
                val direct = getShootVec(name, 1f)
                missileLaunchEffect(serverLevel, pos, direct)
            }
        }
    }

    override fun canBind() = true
}
