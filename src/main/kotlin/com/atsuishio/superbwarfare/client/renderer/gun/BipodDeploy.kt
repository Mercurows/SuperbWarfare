package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.renderer.gun.BipodDeploy.apply
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * 下挂脚架的展开姿态：配件模型里 `bipod_l` / `bipod_r` 两条腿按卧姿架设进度绕 X 轴向后翻下去
 */
object BipodDeploy {

    // 配件模型里两条腿的骨骼名
    private val BONES = arrayOf("bipod_l", "bipod_r")

    // 展开时两条腿绕 X 轴转过的角度（度）
    private const val DEPLOY_X_DEGREES = -90f
    private const val DEG_TO_RAD = 0.017453292f

    // 逐帧复用的一组草稿对象
    private val scratch = Quaternionf()
    private val scratchAxis = Quaternionf()

    /**
     * 按 [progress]（0 = 收起，1 = 完全展开）把两条腿的姿态写进模型
     */
    @JvmStatic
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
    @JvmStatic
    fun revert(model: BedrockAttachmentModel, snapshot: BipodSnapshot?) {
        if (snapshot == null) return

        for (entry in snapshot.entries) {
            val bone = model.instance.getBone(entry.index) ?: continue
            bone.rotation.set(entry.rotation)
            bone.rotationInEuler.set(entry.euler)
        }
    }
}

class BipodSnapshot {
    class Entry(@JvmField val index: Int, bone: BoneState) {
        @JvmField
        val rotation = Quaternionf(bone.rotation)

        @JvmField
        val euler = Vector3f(bone.rotationInEuler)
    }

    @JvmField
    val entries = ArrayList<Entry>(2)
}
