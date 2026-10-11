package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage
import com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle
import com.atsuishio.superbwarfare.tools.playLocalSound
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.item.Rarity
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d

@RegistryName("igla_9k38")
object IglaItem : GeoGunItemV2(Properties().rarity(Rarity.EPIC)) {

    override fun useSpecialFireProcedure(data: GunData) = true
    override fun shootBullet(parameters: ShootParameters): Boolean {
        val shooter = parameters.shooter ?: return false
        val data = parameters.data

        if (!parameters.zoom || !data.hasEnoughAmmoToShoot(shooter)) return false

        // 老版是 `Vector3d(0, -0.2, 0.15)` 依次按俯仰、偏航旋转：
        // 偏航要先 +90 再取负，得到的才是"右肩前"这个方位（直接按 -yaw 转会跑到正前方）。
        val yRot = (shooter.yRot + 360f + 90f) % 360f
        val muzzle = Vector3d(0.0, -0.2, 0.15)
            .rotateZ((-shooter.xRot * Mth.DEG_TO_RAD).toDouble())
            .rotateY((-yRot * Mth.DEG_TO_RAD).toDouble())

        // `shootDirection` 是 `doShoot` 已经算好的方向（含散布），所以只叠加 Y 分量，
        // 不能整根换掉 —— 换掉就等于把 `Spread` 吞了。
        val direction = parameters.shootDirection

        val launched = super.shootBullet(
            parameters.copy(
                shootPosition = Vec3(
                    shooter.x + muzzle.x,
                    shooter.eyeY + muzzle.y,
                    shooter.z + muzzle.z
                ),
                shootDirection = Vec3(direction.x, direction.y + 0.3, direction.z)
            )
        )
        if (!launched) return false

        // 发射筒的尾烟（放在这里而不是 `afterShoot`，是为了让"打出去了"这个瞬间只有一处判据）
        val look = shooter.lookAngle
        sendParticle(
            parameters.level, ParticleTypes.CLOUD,
            shooter.x + 1.8 * look.x,
            shooter.y + shooter.bbHeight - 0.1 + 1.8 * look.y,
            shooter.z + 1.8 * look.z,
            30, 0.4, 0.4, 0.4, 0.005, true
        )

        return true
    }

    override fun afterShoot(parameters: ShootParameters) {
        super.afterShoot(parameters)

        val shooter = parameters.shooter
        if (shooter !is ServerPlayer) return

        parameters.data.get(GunProp.SOUND_INFO).fire1P?.let {
            shooter.playLocalSound(it, SoundSource.PLAYERS, 2f, 1f)
        }

        sendPacketTo(shooter, ShootClientMessage)
    }
}
