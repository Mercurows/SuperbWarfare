@file:JvmName("CameraTool")

package com.atsuishio.superbwarfare.tools

import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4d
import org.joml.Vector4d

@OnlyIn(Dist.CLIENT)
fun Vector4d.maxZoom(transform: Matrix4d): Vec3 = getMaxZoom(transform, this)

@OnlyIn(Dist.CLIENT)
fun getMaxZoom(transform: Matrix4d, maxCameraPos: Vector4d): Vec3 {
    val vehiclePos = transform.transform(Vector4d(0.0, 0.0, 0.0, 1.0))
    val maxCameraPosVec3 = Vec3(maxCameraPos.x, maxCameraPos.y, maxCameraPos.z)

    val player = localPlayer ?: return maxCameraPosVec3

    val vehiclePosVec3 = Vec3(vehiclePos.x, vehiclePos.y, vehiclePos.z)
    val toVec = vehiclePosVec3.vectorTo(maxCameraPosVec3)

    val hitResult = player.level().clip(
        ClipContext(
            vehiclePosVec3,
            vehiclePosVec3.add(toVec).add(toVec.normalize().scale(1.0)),
            ClipContext.Block.VISUAL,
            ClipContext.Fluid.NONE,
            player
        )
    )
    return if (hitResult.type == HitResult.Type.BLOCK) {
        hitResult.getLocation().add(toVec.normalize().scale(-1.0))
    } else {
        maxCameraPosVec3
    }
}
