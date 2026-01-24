package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

object DoubleJumpMessage : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()

        val level = player.level()
        val x = player.x
        val y = player.y
        val z = player.z

        level.playSound(null, BlockPos.containing(x, y, z), ModSounds.DOUBLE_JUMP.get(), SoundSource.BLOCKS, 1f, 1f)

        val vehicle = player.getRootVehicle()
        if (vehicle !== player) {
            vehicle.deltaMovement = Vec3(vehicle.lookAngle.x, 0.8, vehicle.lookAngle.z)
        }
    }
}