package com.atsuishio.superbwarfare.client.renderer.scope

import com.atsuishio.superbwarfare.compat.oculus.OculusCompat
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.gun.pojo.BuiltinScopeInfo
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import java.util.regex.Pattern

/**
 * 给**枪械模型**打"镜筒窗口"的那一半模板（stencil）流水线。
 *
 * 另一半（用 `GL_EQUAL 0` 把枪身剔除在窗口之外）已经在 `GeoGunRenderer.renderModel` 里了 —— 它本来
 * 就是给配件瞄准镜用的。这里补的是"**把窗口写进模板**"，于是 `ocular` 骨骼所在的枪就能像瞄准镜
 * 配件一样开出一个圆窗：窗口之外的枪身/镜筒照常绘制，那圈几何体本身就是黑色外框
 * （仓库里没有任何 2D 遮罩贴图）。
 *
 * ## 为什么不和 `BedrockAttachmentModel` 共用
 *
 * 配件那套引擎有三处**不能**照搬到枪上：
 *
 * - `renderWithStencil` 末尾会调 `renderRemaining`，那是把**整个模型**画一遍。枪身由
 *   `GeoGunModel.renderToBuffer` 画，再来一遍就是重复绘制，还会和 `GL_EQUAL 0` 的剔除打架。
 * - `renderOcularAndDivisionInternal` 的镜片循环里有一句 `if (i >= divisions.size) break`
 *   （`BedrockAttachmentModel.kt:588`）。**没有 `division` 骨骼时它在第一轮就 break**，镜筒壁和
 *   准星一个都不画。而 20 个上线瞄准镜里恰好有一个（`steel_pipe_scope`）就是"有 ocular 无 division"
 *   的配置 —— 改那句就会动到它的外观。
 * - 其余部分（模式切换、`scope_body`、`dynamic_divison`、弹药读数）在枪侧全无意义。
 *
 * 真正值得复用的只有 [ScopeStencilRenderHelper]（本来就是共享 object）和下面那段约 30 行的屏幕空间
 * 圆盘；抄一遍的代码量很小，却把上面三个坑全绕开了。
 *
 * ## 骨骼约定（与配件瞄准镜一致）
 *
 * - `ocular` / `ocular_sight` / `ocular_scope`（可带 `_<模式号>` 后缀）—— **写窗口**，然后在圆盘
 *   **之外**被画成一圈镜筒壁。
 * - `ocular_ring` —— 镜圈，始终实心画在最前。
 * - `division`（可带 `_<模式号>` 后缀）—— 圆盘**之内**那层，也就是准星。可选：没有就只是一个干净的
 *   透光窗口。
 *
 * 模型实例与 `GeoGunModel` 共用，所以**每一步写进骨骼的 `visible` 都必须还原**（见
 * [hideOcularBones] / [hideDivisionBones]）。两者的时机**不一样**：`ocular` 只在开镜那一路藏，
 * 而 `division` 任何时候都要藏 —— 理由写在 [hideDivisionBones] 上。
 */
@OnlyIn(Dist.CLIENT)
class BuiltinGunScopeRenderer(
    private val baseModel: TreeBedrockModel,
    private val instance: TreeModelInstance
) {

    /** 参与写窗口的骨骼，按名字里的模式号分组。枪只认第 0 组（没有模式切换）。 */
    private val ocularIndices: List<Int>

    /** `ocular_ring`；geo 里没有就是 -1。 */
    private val ringIndex: Int

    /** `division`；geo 里没有就是 -1 —— 此时窗口里什么都不画，直接透出放大的世界。 */
    private val divisionIndex: Int

    /**
     * 走模板那一路时要在枪身那一遍里藏掉的骨骼（`ocular` / `ocular_ring`）。
     *
     * `division` **不在**这里 —— 它的藏法完全不同：它不是"画过一遍就别再画"，而是
     * **任何时候都不该被当成普通几何体画出来**，见 [hideDivisionBones]。
     */
    private val ocularBoneIndices: IntArray

    init {
        val oculars = mutableListOf<Int>()
        for (bone in baseModel.bones()) {
            if (OCULAR_PATTERN.matcher(bone.name()).matches()) {
                oculars += bone.index()
            }
        }
        ocularIndices = oculars

        ringIndex = baseModel.getIndex(OCULAR_RING_NODE)
        divisionIndex = baseModel.getIndex(DIVISION_NODE)

        ocularBoneIndices = buildList {
            addAll(ocularIndices)
            if (ringIndex >= 0) add(ringIndex)
        }.toIntArray()
    }

    /**
     * geo 里有没有 `ocular`。为假时这条路径整个关闭 —— 绝大多数枪都是这样，连一次 `RenderSystem`
     * 调用都不会多。
     */
    val available: Boolean get() = ocularIndices.isNotEmpty()

    /**
     * 藏掉 `ocular` / `ocular_ring`，返回它们原来的 `visible`，交给 [restoreOcularBones] 还原。
     *
     * 只包住**走模板那一路**时的枪身那一遍：这两根骨骼 [renderWithStencil] 已经画过了，不藏就会在
     * `GL_EQUAL 0` 的剔除区间里被**再画一遍**（配件侧由 `renderRemaining` 的隐藏逻辑承担同一职责）。
     * 腰射时它们就是枪上真实存在的镜筒本体，**不藏**。
     */
    fun hideOcularBones(): BooleanArray {
        val saved = BooleanArray(ocularBoneIndices.size)
        for (i in ocularBoneIndices.indices) {
            val bone = instance.getBone(ocularBoneIndices[i]) ?: continue
            saved[i] = bone.visible
            bone.visible = false
        }
        return saved
    }

    /** 还原 [hideOcularBones] 记录的可见性。geo 里没有 `division` 时传 null 即可。 */
    fun restoreOcularBones(saved: BooleanArray) {
        for (i in ocularBoneIndices.indices) {
            if (i >= saved.size) break
            instance.getBone(ocularBoneIndices[i])?.visible = saved[i]
        }
    }

    /**
     * 藏掉准星板 `division`。geo 里没有这根骨骼时返回 `null`。
     *
     * ⚠ **不论瞄不瞄准都要调**，这是维修记录：`division` 是一块悬在枪口前约六个方块、200×200 的
     * 透明大板，`renderToBuffer` 会把它当**普通几何体**画出来 —— 一旦只在开镜那一路藏它，腰射时
     * 就会有一块十字准星飘在枪前方半空中。它唯一该出现的地方是 [renderWithStencil] 里那一段，
     * 由后者自己临时打开。
     *
     * 返回原值而不是 `BooleanArray`：`division` 只有一根骨骼，包一层数组没有意义。
     */
    fun hideDivisionBones(): Boolean? {
        if (divisionIndex < 0) return null
        val bone = instance.getBone(divisionIndex) ?: return null
        val original = bone.visible
        bone.visible = false
        return original
    }

    /** 还原 [hideDivisionBones]。 */
    fun restoreDivisionBones(saved: Boolean?) {
        if (saved == null || divisionIndex < 0) return
        instance.getBone(divisionIndex)?.visible = saved
    }

    /**
     * 画窗口、镜圈、镜筒壁与准星。
     *
     * **调用方的 `poseStack` 必须与画枪身时是同一个**（即不要额外 push 任何变换）—— 窗口、准星和
     * 被剔除的枪身三者就是靠共用同一份变换才咬合在一起的，Z 轴压缩量再怎么变都不会错位。
     *
     * 收尾会把模板测试关掉；调用方若要接着做枪身剔除，需自己重新 `enableItemEntityStencilTest()`
     * （`GeoGunRenderer` 正是这么接的）。
     */
    fun renderWithStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        info: BuiltinScopeInfo
    ) {
        if (!available) return

        ScopeStencilRenderHelper.enableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)

        // 镜圈：永远实心，画在写模板之前，所以它不吃窗口的亏。
        if (ringIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderBoneImmediate(ringIndex, poseStack, bufferSource, quadType, triangleType, light)
        }

        writeWindowStencil(poseStack, bufferSource, quadType, triangleType, light)
        carveDisc(poseStack, info)
        drawWallsAndReticle(poseStack, bufferSource, quadType, triangleType, light)

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
        ScopeStencilRenderHelper.disableItemEntityStencilTest()
    }

    /**
     * 把镜筒口那片轮廓写进模板。
     *
     * 由内向外画（`reversed()`）并配 `GL_GREATER i + 1`：**只有还没被写过的像素**才被写上 —— 也就是
     * 每一根 `ocular` 只写自己比前面几根多出来的那部分。单根时结果就是"轮廓=1"，多根时是同心圈。
     * 颜色与深度都关掉，这一遍纯粹是往模板里塞值。
     */
    private fun writeWindowStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)
        RenderSystem.stencilMask(0xFF)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)

        for (i in ocularIndices.indices.reversed()) {
            RenderSystem.stencilFunc(GL11.GL_GREATER, i + 1, 0xFF)
            renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)
        }

        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
    }

    private fun carveDisc(poseStack: PoseStack, info: BuiltinScopeInfo) {
        val builder: BufferBuilder = Tesselator.getInstance().builder
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT)
        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)

        val progress = ClientEventHandler.aimingProgress(ClientEventHandler.zoomTime)
        val rad = 80f * info.viewRadiusModifier * progress

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        for (i in ocularIndices.indices) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            val center = getBoneCenter(poseStack, ocularIndices[i])
            val centerX = center.x() * 16f * 90f
            val centerY = center.y() * 16f * 90f

            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)
            builder.vertex(centerX.toDouble(), centerY.toDouble(), DISC_Z.toDouble())
                .color(255, 255, 255, 255).endVertex()
            for (j in 0..90) {
                val angle = j * ((Math.PI * 2.0) / 90.0)
                val sin = Mth.sin(angle.toFloat())
                val cos = Mth.cos(angle.toFloat())
                builder.vertex((centerX + cos * rad).toDouble(), (centerY + sin * rad).toDouble(), DISC_Z.toDouble())
                    .color(255, 255, 255, 255)
                    .endVertex()
            }
            BufferUploader.drawWithShader(builder.end())
        }

        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
    }

    /**
     * 圆盘**之外**画镜筒壁，圆盘**之内**画准星。
     *
     * 圆盘已经把窗口内部翻成了 `~(i + 1)`，所以 `EQUAL i + 1` 剩下的正好是"轮廓减去圆盘"那一圈
     * （镜筒壁，永远画），`EQUAL ~(i + 1)` 则正好是窗口本身（准星）。
     *
     * 准星额外等一个 [DIVISION_MIN_ZOOM]：它是贴在镜筒前方一块透明板上的准星纹理，镜筒还没抬到位时
     * 就已经悬在枪身前面了，等开镜推到四成再出现才不会穿帮（与配件瞄准镜同一个门槛）。
     */
    private fun drawWallsAndReticle(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        val divisionDue = divisionIndex >= 0 && ClientEventHandler.zoomTime >= DIVISION_MIN_ZOOM

        for (i in ocularIndices.indices) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)

            if (divisionDue) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, (i + 1).inv() and 0xFF, 0xFF)
                renderBoneImmediate(divisionIndex, poseStack, bufferSource, quadType, triangleType, light)
            }
        }
    }

    /**
     * 单画一根骨骼。
     *
     * 与 `BedrockAttachmentModel.renderBoneImmediate` 同一套：**先把 `visible` 打开再画**。原因和那边
     * 一样 —— 这些骨骼在常规那一遍里是被 [hideOcularBones] / [hideDivisionBones] 藏起来的，而
     * `renderSingleBone` 仍然会检查骨骼自身的可见性标志；画完立刻还原，免得污染同一份共享实例的后续渲染。
     */
    private fun renderBoneImmediate(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        if (boneIndex < 0) return
        val bone = instance.getBone(boneIndex) ?: return
        val originalVisible = bone.visible
        bone.visible = true

        instance.renderSingleBone(
            poseStack,
            boneIndex,
            bufferSource,
            quadType,
            triangleType,
            light,
            OverlayTexture.NO_OVERLAY,
            1f,
            1f,
            1f,
            1f,
            true
        )
        flush(bufferSource, quadType, triangleType)

        bone.visible = originalVisible
    }

    private fun flush(
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType
    ) {
        if (!OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(quadType)
            bufferSource.endBatch(triangleType)
        }
    }

    /** 骨骼在**当前 poseStack**下的原点位置（方块）。圆盘的圆心就是这么来的。 */
    private fun getBoneCenter(poseStack: PoseStack, boneIndex: Int): Vector3f {
        val matrix = Matrix4f(poseStack.last().pose()).mul(instance.getGlobalTransform(boneIndex))
        return matrix.getTranslation(Vector3f())
    }

    companion object {
        /**
         * 准星（以及跟着它一起藏在 `division` 子树里的东西）开始绘制的开镜进度。
         *
         * 取值与 `BedrockAttachmentModel.DIVISION_MIN_ZOOM` 一致，这样同一把枪不管用内置瞄具还是
         * 配件瞄准镜，准星出现的时机都一样。是个 `Double` 以匹配 [ClientEventHandler.zoomTime]，
         * 免得比较的是被拓宽后的 `Float`。
         */
        private const val DIVISION_MIN_ZOOM = 0.4

        /** 圆盘所贴的深度。只是个"够靠前"的常量，不参与任何几何计算。 */
        private const val DISC_Z = -90.0

        private const val OCULAR_RING_NODE = "ocular_ring"
        private const val DIVISION_NODE = "division"
        private const val OCULAR_NODE = "ocular"
        private const val OCULAR_SIGHT_NODE = "ocular_sight"
        private const val OCULAR_SCOPE_NODE = "ocular_scope"

        private val OCULAR_PATTERN = Pattern.compile(
            "^($OCULAR_NODE|$OCULAR_SIGHT_NODE|$OCULAR_SCOPE_NODE)(_\\d+)?$"
        )
    }
}
