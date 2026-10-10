package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object DeployedWeaponHandler {

    const val PIVOT_HEIGHT = 1.0

    const val STAND_BEHIND = 1.0

    const val PITCH_LIMIT = 60f

    const val MAX_DRIFT = 1.0

    @JvmStatic
    fun isDeployed(player: Player): Boolean = pivotOf(player) != null

    @JvmStatic
    fun pivotOf(player: Player): Vec3? = PlayerVariable.getOrDefault(player).deployPivot

    @JvmStatic
    fun deploy(player: Player): Boolean {
        if (player.level().isClientSide) return false

        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return false
        val data = GunData.from(stack)
        if (!item.deployable(data)) return false

        if (player.isSpectator || player.isPassenger || !player.isAlive) return false
        if (!player.onGround()) return false

        val level = player.level()
        val feet = player.position()

        if (!level.getBlockState(BlockPos.containing(feet.x, feet.y - 0.1, feet.z)).canOcclude()) return false

        val pivot = Vec3(feet.x, feet.y + PIVOT_HEIGHT, feet.z)

        val forward = horizontalForward(player)
        val behindX = pivot.x - forward.x * STAND_BEHIND
        val behindZ = pivot.z - forward.z * STAND_BEHIND
        if (!level.getBlockState(BlockPos.containing(behindX, feet.y - 0.1, behindZ)).canOcclude()) return false

        player.teleportTo(behindX, feet.y, behindZ)
        player.deltaMovement = Vec3.ZERO

        val uuid = data.uuid?.toString()
        PlayerVariable.modify(player) {
            it.deployPivot = pivot
            it.deployGunUuid = uuid
        }
        return true
    }

    @JvmStatic
    fun undeploy(player: Player) {
        if (player.level().isClientSide) return

        PlayerVariable.modify(player) {
            it.deployPivot = null
            it.deployGunUuid = null
        }
    }

    @JvmStatic
    fun applyOrbit(player: Player) {
        val pivot = pivotOf(player) ?: return

        val forward = horizontalForward(player)
        val x = pivot.x - forward.x * STAND_BEHIND
        val z = pivot.z - forward.z * STAND_BEHIND

        player.setPos(x, player.y, z)

        player.xo = x
        player.zo = z
        player.yo = player.y
    }

    fun serverTick(player: Player) {
        if (player.level().isClientSide) return

        val cap = PlayerVariable.getOrDefault(player)
        val pivot = cap.deployPivot ?: return

        val stack = player.mainHandItem
        val item = stack.item as? GunItem
        val data = if (item == null) null else GunData.from(stack)
        val stillHolding = item != null && data != null && item.deployable(data) &&
                cap.deployGunUuid != null && cap.deployGunUuid == data.uuid?.toString()

        if (!stillHolding || !player.isAlive || player.isPassenger || player.isSpectator) {
            undeploy(player)
            return
        }

        val dx = player.x - pivot.x
        val dz = player.z - pivot.z
        val radius = sqrt(dx * dx + dz * dz)
        val dy = player.y - (pivot.y - PIVOT_HEIGHT)

        if (abs(radius - STAND_BEHIND) > MAX_DRIFT ||
            abs(dy) > MAX_DRIFT ||
            player.level().getBlockState(player.blockPosition()).canOcclude()
        ) {
            undeploy(player)
            return
        }

        player.deltaMovement = Vec3.ZERO
        player.fallDistance = 0f
        val clamped = Mth.clamp(player.xRot, -PITCH_LIMIT, PITCH_LIMIT)
        if (clamped != player.xRot) {
            player.xRot = clamped
            player.xRotO = clamped
        }
    }

    fun clientTick(player: Player) {
        if (!isDeployed(player)) return

        applyOrbit(player)
        player.deltaMovement = Vec3.ZERO

        if (player.isSprinting) {
            player.setSprinting(false)
        }

        val clamped = Mth.clamp(player.xRot, -PITCH_LIMIT, PITCH_LIMIT)
        if (clamped != player.xRot) {
            player.xRot = clamped
            player.xRotO = clamped
        }
    }

    @JvmStatic
    fun horizontalForward(player: Player): Vec3 =
        Vec3.directionFromRotation(0f, player.yRot)
}
