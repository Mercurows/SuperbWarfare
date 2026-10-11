package com.atsuishio.superbwarfare.client.charm

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots.Bones
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.LocalCubeBounds
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * 吊坠模型里与摆动有关的那部分结构。
 *
 * ## 模型约定
 *
 * 配件模型里三个**分组**（都是 [Bones] 里的常量名）：
 *
 * | 分组 | 作用 | 摆不摆 |
 * |---|---|---|
 * | `fixed` | 固定件（挂环、卡扣） | 不摆，正常渲染 |
 * | `string` | 连接绳 | 摆（绕[摆点][pivot]刚性旋转） |
 * | `charm` | 挂件本体 | 摆（同上） |
 */
class CharmRig(
    private val stringIndex: Int,
    private val charmIndex: Int,
    /** `charm` 是 `string` 的后代（两组互为父子）时为真 */
    private val charmFollowsString: Boolean,
    /** `string` 是 `charm` 的后代时为真 */
    private val stringFollowsCharm: Boolean,
    private val stringParentGlobalInverse: Matrix4f,
    private val stringBindGlobal: Matrix4f,
    private val charmParentGlobalInverse: Matrix4f,
    private val charmBindGlobal: Matrix4f,
    /** 摆点（模型局部空间，方块） */
    val pivot: Vector3f,
    /** 摆长（方块） */
    val length: Float,
    /** 绑定姿态下挂件相对摆点的方向（模型局部空间，单位向量） */
    val restDir: Vector3f,
) {

    // 逐帧复用的一组草稿对象：这个类的所有方法都是"一帧内一次性算完"的，
    // 而 (手, 配件) 那层的状态是分开的，所以同一帧里最多顺序调用两次，不会重入。
    private val scratchW = Matrix4f()
    private val scratchLocal = Matrix4f()
    private val scratchPivot = Matrix4f()
    private val scratchTranslation = Vector3f()
    private val scratchRotation = Quaternionf()

    /**
     * 把摆角写进两根骨骼，并把原值存进 [snapshot]
     */
    fun apply(instance: TreeModelInstance, swing: Quaternionf, snapshot: CharmSnapshot) {
        // W = T(P) · R · T(-P)：模型局部空间里"绕摆点转"
        val w = scratchW.translation(pivot.x, pivot.y, pivot.z)
            .rotate(swing)
            .translate(-pivot.x, -pivot.y, -pivot.z)

        // ⚠ 两组**互为父子**时只能驱动祖先那一根：子骨骼会跟着父骨骼一起转，
        // 再给它套一次同样的 `W` 就等于转了两次（子骨骼的局部矩阵是相对**已旋转**的父级解出来的）。
        if (!charmFollowsString) {
            writeBone(instance, stringIndex, stringParentGlobalInverse, stringBindGlobal, w, snapshot.string)
        }
        if (!stringFollowsCharm) {
            writeBone(instance, charmIndex, charmParentGlobalInverse, charmBindGlobal, w, snapshot.charm)
        }
    }

    /** 还原 [apply] 写进去的那两根骨骼 */
    fun restore(instance: TreeModelInstance, snapshot: CharmSnapshot) {
        snapshot.string.restore(instance.getBone(stringIndex))
        snapshot.charm.restore(instance.getBone(charmIndex))
    }

    private fun writeBone(
        instance: TreeModelInstance,
        index: Int,
        parentGlobalInverse: Matrix4f,
        bindGlobal: Matrix4f,
        w: Matrix4f,
        snapshot: CharmBoneSnapshot,
    ) {
        val bone = instance.getBone(index) ?: return
        snapshot.save(bone)

        val local = scratchLocal.set(parentGlobalInverse).mul(w).mul(bindGlobal)
        val rotation = local.getUnnormalizedRotation(scratchRotation)

        val definition = bone.definition()
        val pivotMatrix = scratchPivot
        if (definition.rotateAroundPivot()) {
            val px = definition.pivotX()
            val py = definition.pivotY()
            val pz = definition.pivotZ()
            pivotMatrix.translation(px, py, pz).rotate(rotation).translate(-px, -py, -pz)
        } else {
            pivotMatrix.rotation(rotation)
        }

        val translation = local.mul(pivotMatrix.invert()).getTranslation(scratchTranslation)

        bone.rotation.set(rotation)
        rotation.getEulerAnglesZYX(bone.rotationInEuler)
        bone.x = translation.x * 16f
        bone.y = translation.y * 16f
        bone.z = translation.z * 16f
    }

    companion object {
        /** `string` / `charm` 两组都不存在时返回 `null`（这个模型不是吊坠） */
        fun resolve(model: BedrockAttachmentModel): CharmRig? {
            val base = model.baseModel
            val instance = model.instance

            val stringIndex = base.getIndex(Bones.CHARM_STRING)
            val charmIndex = base.getIndex(Bones.CHARM_CHARM)
            if (stringIndex < 0 || charmIndex < 0) return null

            // 绑定姿态：全部包围盒与父子关系都在这个姿态下取，之后不受任何动画影响
            instance.resetPose()

            // 父级绑定全局变换的**逆**是常量（骨骼的静态父子关系不会变），绑定时算一次即可，
            // 不必每帧现算两次 4x4 求逆
            val stringParent = parentBindGlobal(instance, base.bone(stringIndex).parentIndex()).invert()
            val stringBind = Matrix4f(instance.getGlobalTransform(stringIndex))
            val charmParent = parentBindGlobal(instance, base.bone(charmIndex).parentIndex()).invert()
            val charmBind = Matrix4f(instance.getGlobalTransform(charmIndex))

            val stringBounds = bindBounds(model, stringIndex) ?: return null
            val charmBounds = bindBounds(model, charmIndex) ?: return null

            val pivot = Vector3f(
                (stringBounds[0] + stringBounds[3]) * 0.5f,
                stringBounds[4],
                (stringBounds[2] + stringBounds[5]) * 0.5f,
            )

            // 摆长 = 绳子自己的几何长度（挂点到挂件的距离），就是 `string` 分组绑定包围盒的高度
            val length = (stringBounds[4] - stringBounds[1]).coerceAtLeast(MIN_LENGTH)

            val restCenter = Vector3f(
                (charmBounds[0] + charmBounds[3]) * 0.5f,
                (charmBounds[1] + charmBounds[4]) * 0.5f,
                (charmBounds[2] + charmBounds[5]) * 0.5f,
            )
            val restDir = restCenter.sub(pivot, Vector3f())
            if (restDir.lengthSquared() < 1.0e-8f) {
                restDir.set(0f, -1f, 0f)
            } else {
                restDir.normalize()
            }

            return CharmRig(
                stringIndex = stringIndex,
                charmIndex = charmIndex,
                charmFollowsString = inSubtree(base.bones(), stringIndex, charmIndex),
                stringFollowsCharm = inSubtree(base.bones(), charmIndex, stringIndex),
                stringParentGlobalInverse = stringParent,
                stringBindGlobal = stringBind,
                charmParentGlobalInverse = charmParent,
                charmBindGlobal = charmBind,
                pivot = pivot,
                length = length.coerceAtLeast(MIN_LENGTH),
                restDir = restDir,
            )
        }

        /** 摆长下限：再短的绳子也没有"摆"可言，取个下限免得重力被算成 0 */
        private const val MIN_LENGTH = 1.0e-4f

        /** 某个骨骼父级的绑定全局变换；没有父级时是单位阵 */
        private fun parentBindGlobal(instance: TreeModelInstance, parentIndex: Int): Matrix4f =
            if (parentIndex < 0) Matrix4f() else Matrix4f(instance.getGlobalTransform(parentIndex))

        /** [target] 是否落在 [root] 的子树里（不含 [root] 自己） */
        private fun inSubtree(bones: Array<TreeBoneDefinition>, root: Int, target: Int): Boolean {
            val stack = ArrayDeque<Int>()
            for (child in bones[root].children()) stack.addLast(child)
            while (stack.isNotEmpty()) {
                val index = stack.removeLast()
                if (index == target) return true
                for (child in bones[index].children()) stack.addLast(child)
            }
            return false
        }

        /**
         * 某个骨骼**整棵子树**在绑定姿态下的包围盒：`[minX, minY, minZ, maxX, maxY, maxZ]`（方块）
         */
        private fun bindBounds(model: BedrockAttachmentModel, rootIndex: Int): FloatArray? {
            val base = model.baseModel
            val instance = model.instance

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            var any = false

            val stack = ArrayDeque<Int>()
            stack.addLast(rootIndex)
            while (stack.isNotEmpty()) {
                val index = stack.removeLast()
                val definition = base.bone(index)
                for (child in definition.children()) stack.addLast(child)

                val bounds: LocalCubeBounds = definition.ownCubeBounds() ?: continue
                val matrix = instance.getGlobalTransform(index)
                any = true

                for (corner in 0 until 8) {
                    val x = if (corner and 1 == 0) bounds.minX() else bounds.maxX()
                    val y = if (corner and 2 == 0) bounds.minY() else bounds.maxY()
                    val z = if (corner and 4 == 0) bounds.minZ() else bounds.maxZ()
                    val v = matrix.transformPosition(Vector3f(x, y, z))
                    if (v.x < minX) minX = v.x
                    if (v.y < minY) minY = v.y
                    if (v.z < minZ) minZ = v.z
                    if (v.x > maxX) maxX = v.x
                    if (v.y > maxY) maxY = v.y
                    if (v.z > maxZ) maxZ = v.z
                }
            }

            return if (any) floatArrayOf(minX, minY, minZ, maxX, maxY, maxZ) else null
        }
    }
}

class CharmSnapshot {
    @JvmField
    val string = CharmBoneSnapshot()

    @JvmField
    val charm = CharmBoneSnapshot()
}

/** 单根骨骼的 `x/y/z + rotation + rotationInEuler` 快照 */
class CharmBoneSnapshot {
    private var valid = false
    private var x = 0f
    private var y = 0f
    private var z = 0f
    private val rotation = Quaternionf()
    private val euler = Vector3f()

    fun save(bone: BoneState?) {
        if (bone == null) {
            valid = false
            return
        }
        valid = true
        x = bone.x
        y = bone.y
        z = bone.z
        rotation.set(bone.rotation)
        euler.set(bone.rotationInEuler)
    }

    fun restore(bone: BoneState?) {
        if (!valid || bone == null) return
        bone.x = x
        bone.y = y
        bone.z = z
        bone.rotation.set(rotation)
        bone.rotationInEuler.set(euler)
    }
}
