package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * 下挂脚架的展开姿态：配件模型里 `bipod_l` / `bipod_r` 两条腿按卧姿架设进度绕 X 轴向后翻下去。
 *
 * ## 为什么是一段渲染代码而不是枪脚本
 *
 * 枪脚本的 `transformCustomModelPart` 拿到的 `model` 是**枪自己的**模型（`GeoGunModel`），
 * 而这两条腿长在**配件模型**里（`lower_rail_bipod.geo.json`），脚本够不着；
 * `AttachmentDefinition` 也没有 `Script` 字段。真正为"配件模型里的骨骼 + 由宿主枪的渲染路径驱动"
 * 准备的位置是 `GeoGunRenderer.renderRegisteredAttachments`（吊坠就走那条路），
 * 这里照抄它的生命周期 —— 写骨骼 → 画 → 还原 —— 只是姿态是进度的**纯函数**，没有跨帧状态。
 *
 * ## 由谁决定"哪些配件要动"
 *
 * [com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition.hasBipod]（数据里的 `"Bipod"`）。
 * 它的注释本来就写着"不限于握把，未来其他槽位的配件也可以声明"，这里正是它等的第二类用户。
 * 那个字段同时是"卧姿架设"的总开关（`Attachment.hasBipod()` → `bipodViewTime`），
 * 所以勾上它，脚架的展开姿态和卧姿视角过渡天然同步。
 *
 * ## 时钟
 *
 * 进度由调用方从 `GeoGunRenderer.scriptBipodProgress(stack)` 取，也就是
 * `ClientEventHandler.bipodViewTime`（0 → 1 的 0.35 秒过渡）。那个函数只对**本地玩家自己
 * 手里那把枪**返回非零值，所以别人手里的枪、掉落物、展示框、GUI 一律是收起状态。
 */
object BipodDeploy {

    /** 配件模型里两条腿的骨骼名，与 `scripts/guns/bipod.js` / `m_60.js` 里那份名单一致 */
    private val BONES = arrayOf("bipod_l", "bipod_r")

    /**
     * 展开时两条腿绕 X 轴转过的角度（度）。
     *
     * 模型里两条腿收起时沿 **-Z**（枪口方向）平放（`lower_rail_bipod.geo.json` 里 `bone83` /
     * `bone20` 的方块从铰链 `z≈0` 一直伸到 `z≈-11.4`），所以只有**负**角度会把它们向下翻到 -Y：
     * `Rx(-90°)` 正好把 `(0,0,-1)` 映到 `(0,-1,0)`，脚落到铰链正下方并向两侧张开，
     * 脚点从"枪口前方"走到"铰链下方" —— 就是"往后"那一下。
     *
     * 与 `bipod.js` 的 `BIPOD_DEPLOY_X_DEG = -90` 是同一个坐标系约定，数值直接沿用
     * （`m_60.js` 用 +90° 是因为它那两条腿收在枪身里、指向 **+Z**，方向相反）。
     */
    private const val DEPLOY_X_DEGREES = -90f

    private const val DEG_TO_RAD = 0.017453292f

    // 逐帧复用的一组草稿对象：本对象的方法都是"一帧内一次性算完"的，同一帧里至多顺序调用两次
    // （两只手各一次），不会重入
    private val scratch = Quaternionf()
    private val scratchAxis = Quaternionf()

    /**
     * 按 [progress]（0 = 收起，1 = 完全展开）把两条腿的姿态写进模型。
     *
     * 返回的快照**必须**交给 [revert]，理由和吊坠那边一样：配件模型的实例是**全局共享**的
     * （同一种配件装在多把枪上共用一份，见 `AttachmentModelReloadListener`），
     * 留着姿态会串到下一把枪上。
     *
     * 模型里没有这两根骨骼时返回 `null` —— 这条路径对所有 `Bipod: true` 的配件都会走到，
     * 而 `grip_vertical_bipod` 这类只有折叠外观的配件本来就没有会动的腿。
     */
    fun apply(model: BedrockAttachmentModel, progress: Float): BipodSnapshot? {
        if (progress <= 0f) return null

        val snapshot = BipodSnapshot()
        val deployX = DEPLOY_X_DEGREES * DEG_TO_RAD * progress

        for (name in BONES) {
            val index = model.baseModel.getIndex(name)
            if (index < 0) continue

            val bone = model.instance.getBone(index) ?: continue
            snapshot.entries += BipodSnapshot.Entry(index, bone)

            // 两条腿各自带 ±22.5° 的 Z 轴外张角（`bipod_l` 为负、`bipod_r` 为正），
            // 必须一并保留，否则展开后两条腿会并拢成一条
            val bindY = bone.rotationInEuler.y
            val bindZ = bone.rotationInEuler.z

            // 等价于模型加载时的 rotateZYX(z, y, x)，即 Rz * Ry * Rx —— 与 `bipod.js` 的写法逐字一致
            scratch.set(scratchAxis.rotationZ(bindZ))
                .mul(scratchAxis.rotationY(bindY))
                .mul(scratchAxis.rotationX(deployX))

            bone.rotation.set(scratch)
            bone.rotationInEuler.set(deployX, bindY, bindZ)
        }

        return if (snapshot.entries.isEmpty()) null else snapshot
    }

    /** 还原 [apply] 写进去的姿态（**必须**在画完之后调用） */
    fun revert(model: BedrockAttachmentModel, snapshot: BipodSnapshot?) {
        if (snapshot == null) return

        for (entry in snapshot.entries) {
            val bone = model.instance.getBone(entry.index) ?: continue
            bone.rotation.set(entry.rotation)
            bone.rotationInEuler.set(entry.euler)
        }
    }
}

/**
 * [BipodDeploy.apply] 写进骨骼的那两个字段的快照。
 *
 * 只存 `rotation` / `rotationInEuler`：脚架这条路径不动骨骼的平移，所以不用像
 * [com.atsuishio.superbwarfare.client.charm.CharmBoneSnapshot] 那样连 `x/y/z` 一起存；
 * `visible` 更不能碰 —— 同一个渲染窗口里渲染路径正在改它（藏手、隐藏 ocular 等）。
 */
class BipodSnapshot {

    class Entry(index: Int, bone: BoneState) {
        @JvmField
        val index: Int = index

        @JvmField
        val rotation = Quaternionf(bone.rotation)

        @JvmField
        val euler = Vector3f(bone.rotationInEuler)
    }

    /** 被写过的腿（模型里没有这两根骨骼时是空表，[BipodDeploy.apply] 会改返回 `null`） */
    @JvmField
    val entries = ArrayList<Entry>(2)
}
