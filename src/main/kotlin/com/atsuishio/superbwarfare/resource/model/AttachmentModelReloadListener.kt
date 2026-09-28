package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener.animPath
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

/**
 * 配件模型 + 配件动画的加载器。
 *
 * `animPath` 是**四期加上去的**（§9.8.7）：在此之前配件只有模型、没有动画，
 * 所以副武器的换弹动画只能做在宿主枪的动画文件里（"左手换弹"那条路）。
 * 现在副武器是一把独立的枪（有自己的 `GunData` / `GunResource`），
 * 它的换弹动画就该由**它自己的模型**播 —— 于是给它一个动画目录即可。
 *
 * **配对完全靠文件名 id**（基类 `prepare` 的 `animPathToIds` / `idToModelPaths`）：
 * `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`
 * 自动绑到 `models/bedrock/attachment/sub_weapon_gp_25.geo.json`。
 * 两边缺一个就只是"那一半为空"，不会报错 —— 所以**动画还没做出来时这段是安全的空转**。
 *
 * ## ⚠ 两个"配错了也不报错"的坑（五期实测：GP-25 换弹动画不出效果就是踩了它们）
 *
 * 1. **文件必须放在 [animPath] 这个目录下**。放 `animations/bedrock/gun/` 是无效的 ——
 *    那边是**主武器**动画表（`GunModelReloadListener`），副武器换弹只读本表
 *    （`updateSubWeaponReload` → [findAnimation]）。两边文件名可以完全一样，放错了却毫无提示。
 * 2. **clip 必须驱动"附件模型里存在的"骨骼**。本表用 [BedrockAnimation.createAnimation] 把
 *    clip 绑到**附件模型**上，所以照抄枪模型的骨骼名（`righthand` / `camera` / `head` /
 *    `undefined`）会被**静默丢弃**；症状是"runner 建起来了、模型纹丝不动"，
 *    与"压根没做动画"的外观完全一致。
 *    `sub_weapon_gp_25` 的可用骨骼：`root` / `flare` / `projectile` / `lefthand` /
 *    `lefthand_pos` / `gun` / `tube`（`bone2..bone7` 在它下面）/ `trigger` / `iron_view`。
 *
 * 两条失败路径都会由 `GeoGunAnimationInstance.logSubWeaponReloadMissOnce` 打一条
 * 带定位信息的日志（`melee_debug_log` 打开时可见）。
 */
object AttachmentModelReloadListener : BedrockModelReloadListener<BedrockAttachmentModel>(
    "models/bedrock/attachment",
    "animations/bedrock/attachment"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.models.clear()
        this.animations.clear()

        map.forEach { (location, pojo) ->
            this.models[location] = BedrockAttachmentModel(TreeBedrockModel.bake(pojo))
        }

        // 动画必须绑在**它自己那个模型的骨骼**上（`BedrockAnimation.createAnimation` 会按模型解析骨骼名），
        // 所以这里与 `GunModelReloadListener` 一样按文件名 id 找模型。
        this.animFiles.forEach { (location, file) ->
            val id = this.animPathToIds[location] ?: return@forEach
            val path = this.idToModelPaths[id] ?: return@forEach
            val model = this.models[path]?.baseModel ?: return@forEach
            this.animations[id] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }

    /**
     * 按 **clip 名**找配件动画。
     *
     * clip 名形如 `animation.sub_weapon_gp_25.reload`，它的**文件名 id** 就是
     * `sub_weapon_gp_25`（基类加载时把 `.animation` 后缀去掉了）—— 所以这里先剥掉
     * `animation.` 前缀、再取第一个 `.` 之前的那一段。
     *
     * 找不到就返回 `null`（**动画还没做出来时的正常路径**，调用方静默回退）。
     *
     * 这是副武器动画的**唯一**取用口径：clip 名一律来自**副武器自己的数据**
     * （`sbw/guns/<id>.json` 的 `Animation.Reload*` / `Animation.Idle`），
     * 两条链路分别是换弹（`GeoGunAnimationInstance.updateSubWeaponReload`）与
     * 部署期间的手臂锚点（同类的 `updateSubWeaponIdle`，§11.11.7.4）。
     * 曾经还有一条"按文件名约定"的旁路（`animation.<配件 id>.idle`，给握把接管用），
     * 已随那次需求的撤销一并删除 —— **不要再加回来**。
     */
    fun findAnimation(clipName: String): BedrockAnimation? {
        val path = clipName.removePrefix("animation.")
        val fileId = path.substringBefore('.')
        val namespace = if (fileId.contains(':')) fileId.substringBefore(':') else "superbwarfare"
        val idPath = fileId.substringAfter(':')
        val id = ResourceLocation.tryParse("$namespace:$idPath") ?: return null

        return this.animations[id]?.firstOrNull { it.name == clipName }
    }
}
