package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

class PrismTankEntity(type: EntityType<PrismTankEntity>, world: Level) : GeoVehicleEntity(type, world) {
    init {
        this.noCulling = true
    }

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.2f) * damage }

    fun hitBlock(pos: Vec3, gunData: GunData, shooter: Entity?) {
        val serverLevel = level() as? ServerLevel ?: return

        if (gunData.get(GunProp.EXPLOSION_RADIUS) > 0) {
            findNearEntity(pos, gunData, shooter)
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.END_ROD,
                pos.x,
                pos.y,
                pos.z,
                24,
                0.0,
                0.0,
                0.0,
                0.2,
                true
            )
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.LAVA,
                pos.x,
                pos.y,
                pos.z,
                8,
                0.0,
                0.0,
                0.0,
                0.4,
                true
            )
        } else {
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.END_ROD,
                pos.x,
                pos.y,
                pos.z,
                4,
                0.0,
                0.0,
                0.0,
                0.05,
                true
            )
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.LAVA,
                pos.x,
                pos.y,
                pos.z,
                2,
                0.0,
                0.0,
                0.0,
                0.15,
                true
            )
        }
    }

    fun hitEntity(pos: Vec3, gunData: GunData, shooter: Entity?) {
        val serverLevel = level() as? ServerLevel ?: return

        if (gunData.get(GunProp.EXPLOSION_RADIUS) > 0) {
            findNearEntity(pos, gunData, shooter)
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.END_ROD,
                pos.x,
                pos.y,
                pos.z,
                24,
                0.0,
                0.0,
                0.0,
                0.2,
                true
            )
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.LAVA,
                pos.x,
                pos.y,
                pos.z,
                8,
                0.0,
                0.0,
                0.0,
                0.4,
                true
            )
        } else {
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.END_ROD,
                pos.x,
                pos.y,
                pos.z,
                4,
                0.0,
                0.0,
                0.0,
                0.05,
                true
            )
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.LAVA,
                pos.x,
                pos.y,
                pos.z,
                2,
                0.0,
                0.0,
                0.0,
                0.15,
                true
            )
        }
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

    override fun getWheelMaxHealth() = 100f
    override fun getEngineMaxHealth() = 150f

    @OnlyIn(Dist.CLIENT)
    override fun firstPersonAmmoComponent(data: GunData, player: Player?): Component {
        val name = data.get(GunProp.NAME)
        if (name == null || name.isBlank()) return Component.empty()

        return Component.translatable(name, (25 + data.heat.get()).toInt().toString() + " " + "°C")
    }
}
