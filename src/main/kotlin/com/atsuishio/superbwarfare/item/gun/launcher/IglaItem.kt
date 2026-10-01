package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.item.gun.launcher.IglaItem.useSpecialFireProcedure
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

/**
 * 「针」9K38 便携防空导弹。
 *
 * 这把枪的开火链路与普通枪**完全不同**，是本次迁移里唯一需要堆代码的地方：
 *
 * - **什么时候能打**由客户端的锁定流程决定，不归常规开火链路管 —— 数据里写的是
 *   `SeekType: HoldZoom`，客户端在 `ClientEventHandler.lockWeaponSeeking` 里开镜锁定目标后
 *   **直接**发 `ShootMessage`，压根不经过 `shootClient`。所以 [useSpecialFireProcedure] 必须为真，
 *   否则按住左键会跟着锁定一起连发（老版 `IglaItem` 也是这么做的）。
 * - **打出去什么**已经完全数据驱动：`Projectile` / `Velocity` / `Damage` / `Explosion*` /
 *   `ProjectileLife` 都写在 `data/superbwarfare/sbw/guns/igla_9k38.json` 里，
 *   由 `GunItem.shootBullet` 的通用分支生成导弹并按 `MissileProjectile` 接上锁定目标，
 *   这里只需要补上通用的链路**表达不出来**的两件事：发射点/弹道，以及"服务端开火、客户端演出"的补发。
 */
@RegistryName("igla_9k38")
object IglaItem : GeoGunItemV2(Properties().rarity(Rarity.EPIC)) {

    /**
     * 关掉常规开火链路（自动连发、`shootClient`、拉栓）。
     *
     * 关掉的是**客户端主动开火**这一半，不是开火本身：锁定后的开火由客户端另发 `ShootMessage`
     * （见 `ClientEventHandler.lockWeaponSeeking` 的 `HOLD_ZOOM` 分支），照样会走到服务端的
     * `GunItem.shoot` 上。老版 `IglaItem` 与本类返回同一个值，行为不变。
     */
    override fun useSpecialFireProcedure(data: GunData) = true

    /**
     * 打出一发导弹。
     *
     * 生成导弹的部分整个交给 `super`（读 `Projectile` 等属性、接上锁定目标、走 perk 的
     * `modifyProjectile`），这里只做三件通用链路做不到的事：
     *
     * 1. **补一道"没开镜就不打"的闸**。常规入口（脚本 / 载具 / 生物用枪）不经过客户端的锁定流程，
     *    不拦的话它们能在没开镜时把导弹打出去 —— 老版 `IglaItem.shoot` 的第一行也是这个判据。
     * 2. **把发射点挪到右肩前方的筒口**。通用链路固定用眼睛位置（`Vec3(shooter.x, eyeY, z)`），
     *    而 Igla 的发射筒扛在右肩上，差这 0.25 格导弹会从脸里冒出来。
     * 3. **给弹道一点抬升**，与老版 `shoot(..., lookAngle.y + 0.3, ...)` 一致。
     */
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

    /**
     * 补发射手客户端该听到、该看到的那一份演出。
     *
     * 普通枪的第一人称开火音与开火动画都是**客户端自己开火时顺便播的**
     * （`ClientEventHandler.shootClient` → `playGunClientSounds` / `ClientGunFireEvent`）。
     * 这把枪的第一人称开火音和动画却一个都捞不到：开火完全发生在服务端，客户端只发了个
     * `ShootMessage` 出去。老版 `IglaItem` 的解法也是这个 —— 由服务端把这一份补给射手。
     *
     * ⚠ 补发 [ShootClientMessage] 会让射手客户端**回发**一条 `ShootMessage`（`handleClientShoot`
     * 本身就会发）。这不是死循环：本方法在 `super.afterShoot` 之后才执行，弹药此刻已经扣完
     * （`Magazine` 为 1），回发的那条过不了 `GunItem.shootInternal` 的 `canShoot`，只会被丢掉。
     *
     * 1P 音不用 [com.atsuishio.superbwarfare.item.gun.GunItem.resolveFire1PSounds]：那个口径
     * 的音量是 `0.5 × 音效半径倍率`，是给枪械调的音，发射器按老规矩（本枪与 Javelin 都是音量 2）
     * 要响得多。
     */
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
