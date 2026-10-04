package com.atsuishio.superbwarfare.client.renderer.ammo

import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.renderer.ammo.RangeReadout.MAX_RANGE
import com.atsuishio.superbwarfare.client.renderer.ammo.RangeReadout.RAY_LENGTH
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.roundToInt

/**
 * 配件上 `%range%` 文字锚点的读数来源：**玩家准星正对着的东西有多远**，单位是格。
 *
 * ## 算法
 *
 * 与 [com.atsuishio.superbwarfare.client.overlay.SpyglassRangeOverlay] **逐字一致**，那边是这份
 * 读数的唯一权威 —— 同一位玩家在望远镜里看到的数，和枪上小屏里的数必须是同一个，所以两处都按同一
 * 顺序取：
 *
 * 1. 眼睛位置沿视线射线 [RAY_LENGTH] 格做**方块**碰撞（`OUTLINE`，忽略液体），得到方块距离；
 * 2. 这一 tick 准星锁到的实体（[OverlayTraceHandler.maxRangeEntity]，由客户端 tick 更新）
 *    优先。它是载具时**没有读数** —— 望远镜在那种情况下也不显示距离；
 * 3. 实体距离取 `player.distanceTo(entity)`（脚对脚），方块距离超过 [MAX_RANGE] 则没有读数。
 *
 * 望远镜把"没有读数"画成 `---M`，这里同样返回 [AmmoTextEntry.NO_RANGE]，由
 * [AmmoTextEntry.resolve] 换成 `---`，模板自己在后面补单位即可写出同样的 `---M`。
 *
 * ## 为什么不是一个"每帧都测"的东西
 *
 * 每次读数是**一条 512 格的射线**。调用方（`GeoGunRenderer.attachmentReadout`）只在
 * "本地玩家自己第一人称手里那把枪"、且这件配件的文字模板里真的写了 `%range%` 时才调它 ——
 * 别人手里的枪、掉落物、展示框、改装界面一律不测，既省下射线，也避免第三人称里
 * 别人的枪上挂着一个跟着**自己**视线乱跳的数字。
 */
@OnlyIn(Dist.CLIENT)
object RangeReadout {

    /** 超过这个距离就没有读数，与望远镜一致 */
    private const val MAX_RANGE = 500.0

    /** 射线长度，比 [MAX_RANGE] 多留一截，好把 `MAX_RANGE` 与 `RAY_LENGTH` 之间的命中区分出来 */
    private const val RAY_LENGTH = 512.0

    /**
     * 玩家当前看着的东西有多少格远；没有有效读数时返回 [AmmoTextEntry.NO_RANGE]。
     *
     * 取整到整格：这块小屏只有几个像素高，望远镜那种一位小数在这里只会挤成一团。
     */
    @JvmStatic
    fun measure(player: Player?): Int {
        if (player == null) return AmmoTextEntry.NO_RANGE

        val result = player.level().clip(
            ClipContext(
                player.eyePosition, player.eyePosition.add(player.getViewVector(1f).scale(RAY_LENGTH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
            )
        )
        val blockRange = player.getEyePosition(1f).distanceTo(result.location)

        val lookingEntity = OverlayTraceHandler.maxRangeEntity
        // 载具不给读数：望远镜在这种情况下也是一句都不显示
        if (lookingEntity is VehicleEntity) return AmmoTextEntry.NO_RANGE

        if (lookingEntity != null) return player.distanceTo(lookingEntity).roundToInt()

        if (blockRange > MAX_RANGE) return AmmoTextEntry.NO_RANGE
        return blockRange.roundToInt()
    }
}
