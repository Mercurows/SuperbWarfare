package com.atsuishio.superbwarfare.entity.vehicle


import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Math
import org.joml.Matrix4d

open class KirovEntity(type: EntityType<KirovEntity>, world: Level) : VehicleEntity(type, world) {
    var propellerO = 0f
    var propeller = 180f

    var propellerVO = 0f
    var propellerV = 180f

    var propellerLO = 0f
    var propellerL = 180f

    var propellerRO = 0f
    var propellerR = 180f

    override fun baseTick() {
        propellerO = propeller
        propellerVO = propellerV
        propellerLO = propellerL
        propellerRO = propellerR
        super.baseTick()
        if (level().isClientSide && !sympatheticDetonated) {
            //最顶上的螺旋桨
            propeller += power * 2f

            val delta = Math.abs(propeller - propellerO)
            while (propeller > 180f) {
                propeller -= 360f
                propellerO = propeller - delta
            }
            while (propeller <= -180f) {
                propeller += 360f
                propellerO = delta + propeller
            }

            //两边的升力螺旋桨
            propellerV += 1 + 2 * liftSpeed

            val deltaV = Math.abs(propellerV - propellerVO)
            while (propellerV > 180f) {
                propellerV -= 360f
                propellerVO = propellerV - deltaV
            }
            while (propellerV <= -180f) {
                propellerV += 360f
                propellerVO = deltaV + propellerV
            }

            //左推进螺旋桨

            propellerL += power * 2f - deltaRot * 3f

            val deltaL = Math.abs(propellerL - propellerLO)
            while (propellerL > 180f) {
                propellerL -= 360f
                propellerLO = propellerL - deltaL
            }
            while (propellerL <= -180f) {
                propellerL += 360f
                propellerLO = deltaL + propellerL
            }

            //右推进螺旋桨

            propellerR += power * 2f + deltaRot * 3f

            val deltaR = Math.abs(propellerR - propellerRO)
            while (propellerR > 180f) {
                propellerR -= 360f
                propellerRO = propellerR - deltaR
            }
            while (propellerR <= -180f) {
                propellerR += 360f
                propellerRO = deltaR + propellerR
            }
        }


        if (sprintInputDown) {
            if (level().isClientSide) {
                val p = Vec3(0.0, 18.68, -28.188)
                val transformV = getVehicleTransform(1f)

                val transform = Matrix4d(transformV)
                val worldPosition = VehicleVecUtils.transformPosition(
                    transform,
                    p.x,
                    p.y,
                    p.z
                )

                val worldPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)

                val l = deltaMovement.length()
                repeat(2) {
                    var i = 0.0
                    while (i < l) {
                        val pos = worldPos.add(deltaMovement.normalize().scale(-i))
                        val random = 2 * (this.random.nextFloat() - 0.5f)
                        level().addParticle(
                            CustomFlareOption(
                                0.4f,
                                0.34f,
                                0.27f,
                                600,
                                0.975f,
                                (10 + 8 * random).toInt(),
                                0.1f
                            ), pos.x + random * 2, pos.y + random * 2, pos.z + random * 2, 0.0, 0.0, 0.0
                        )
                        i += 2.0
                    }
                }
            }
            this.onHurt(0.0005f * getMaxHealth(), this.lastAttacker, false)
        }
    }
}
