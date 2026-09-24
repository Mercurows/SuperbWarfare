package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.client.gun.GunActionLock
import com.atsuishio.superbwarfare.client.renderer.special.OBBRenderer.renderOBB
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.RenderType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Quaterniond

/**
 * 近战判定体可视化（`MeleeHitbox` × `MeleeSweep` 采样体的线框）。
 *
 * **触发方式**：原版 `F3 + B`（实体 hitbox 显示）打开、`DisplayConfig.MELEE_DEBUG_LOG`
 * 打开，且主手是一把能近战的枪。沿用 F3+B 而不是新加键位，是因为它本来就是
 * "显示碰撞/判定体"的调试开关，不需要往 `ModKeyMappings` 里塞一个只有开发时才有用的键。
 *
 * 颜色约定：
 * - **绿色** = 精确形状（`Box`）
 * - **黄色** = 近似形状（`Cone` 的外接盒、`Capsule` 的外接盒）
 * - **红色** = 当前正在挥击的那一段（`GunActionLock.State.meleeActionIndex`）
 */
@EventBusSubscriber(value = [Dist.CLIENT])
object MeleeDebugRenderer {

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        if (!DisplayConfig.MELEE_DEBUG_LOG.get()) return
        if (!mc.entityRenderDispatcher.shouldRenderHitBoxes()) return

        val player = localPlayer ?: return
        if (clientLevel == null) return

        val stack = player.mainHandItem
        if (stack.item !is GunItem || !GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)
        if (!data.hasMeleeAttack()) return

        val state = GunActionLock.of(data)
        val context = MeleeQuery.contextOf(player, data.resolveMeleeAction(state.meleeActionIndex))

        val poseStack = event.poseStack
        val camera = event.camera
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(RenderType.lines())

        poseStack.pushPose()

        // 相机相对变换：判定体坐标是世界坐标
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)

        val active = state.meleeTicks > 0
        for (actionIndex in data.meleeActions().indices) {
            val isActive = active && actionIndex == state.meleeActionIndex
            val action = data.resolveMeleeAction(actionIndex)

            for (box in MeleeQuery.debugBoxes(context, action)) {
                val rotation = Quaterniond().rotateY(Math.toRadians(box.yaw.toDouble()))
                val r: Float
                val g: Float
                val b: Float
                when {
                    isActive -> {
                        r = 1f; g = 0.25f; b = 0.2f
                    }

                    box.exact -> {
                        r = 0.25f; g = 1f; b = 0.35f
                    }

                    else -> {
                        r = 1f; g = 0.85f; b = 0.2f
                    }
                }

                renderOBB(
                    poseStack, buffer,
                    box.center.x, box.center.y, box.center.z,
                    rotation,
                    box.radiusX, box.radiusY, box.radiusZ,
                    r, g, b, 1f,
                )
            }
        }

        poseStack.popPose()
        bufferSource.endBatch()
    }
}
