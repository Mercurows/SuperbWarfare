package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/**
 * 修理工具的渲染器。除了 [GeoGunRenderer] 的常规枪械渲染，它只多管一件事：枪口那簇火焰的**逐帧切换**。
 *
 * geo 里 `flame_illuminated` 是一根空骨骼，下面挂着 `flame_0_illuminated` ~ `flame_7_illuminated`
 * 八帧形状各不相同的火焰。"这一帧该画第几张"没有任何别的信息可以推，只能在这里按 tick 挑一张 ——
 * 八帧几何是叠在同一点上的，一起画出来只会糊成一团，所以每一帧只留一张、八张里其余七张藏掉。
 *
 * 火焰的**出现与消失不归这里管**：那是动画侧的事 —— `sbw/guns/repair_tool.json` 的
 * `Animation.FireLoop`（`animation.repair_tool.fire_loop`）按 `FireLoopFadeIn` / `FireLoopFadeOut`
 * 从 idle 交叉淡入淡出（idle 里 `flame_illuminated: scale 0` 是"收起"，循环里 [1, 1, 3] 是"烧起来"）。
 * 所以这里只负责"画哪一张"，显隐是二元量、管不了"慢慢长出来"这个过程，两者分工不要混。
 *
 * 九根骨骼的 `visible` **每帧全部显式写一遍**（和 [GeoGunModel.showBulletChainBones] 同一个理由）：
 * 模型实例是所有同型号枪共用的一份，`resetPose` 之后不能指望上一帧留下了什么，也没有谁负责还原。
 *
 * 只有**自己主手的第一人称**画火焰，其余场合（GUI、掉落物、展示框、第三人称、副手）整簇藏掉 ——
 * 第三人称也藏，是因为 V2 这条渲染路径只在第一人称 `applyPose`（[GeoGunRenderer.renderModel] 里
 * `transformType.firstPerson()` 之外没有任何地方应用动画姿态），第三人称的骨骼停在 bind 姿态上，
 * 而 bind 姿态里 `flame_illuminated` 的 scale 是 1：动画里那句"没开火时收起"读不到，
 * 画出来就是一团不随开火状态变化、永远挂在枪口的火。老 GeckoLib 渲染器在第三人称是"八帧全开"，
 * 靠的是那条路径在第三人称也应用动画，V2 下这个前提不成立。
 */
class RepairToolRenderer : GeoGunRenderer() {

    override fun applyCustomAnimations(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        partialTick: Float
    ) {
        val inHand = transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
        val frame = if (inHand) (localPlayer?.tickCount ?: 0) % FLAME_FRAME_COUNT else -1

        model.getBone(FLAME_BONE)?.visible = inHand
        for (i in 0 until FLAME_FRAME_COUNT) {
            model.getBone(flameFrameBone(i))?.visible = i == frame
        }
    }

    companion object {
        /** 整簇火焰的父骨骼：动画只 K 它，逐帧切换只 K 它下面那八根子骨骼。 */
        private const val FLAME_BONE = "flame"

        /** 火焰的帧数，geo 里就是 `flame_0_illuminated` ~ `flame_7_illuminated` 这八根。 */
        private const val FLAME_FRAME_COUNT = 8

        private fun flameFrameBone(index: Int) = "flame_${index}"
    }
}
