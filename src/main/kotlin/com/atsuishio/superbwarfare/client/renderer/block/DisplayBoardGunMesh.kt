package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshRenderer
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.render.EmissiveMeshRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener

/** Only the board opts in. Held guns, item frames and inventory previews keep their usual path. */
object DisplayBoardGunMesh : ResourceManagerReloadListener {
    private val owner = ResourceLocation("superbwarfare", "display_board_guns")
    private var depth = 0

    fun <T> renderItem(render: () -> T): T {
        depth++
        return try { render() } finally { depth-- }
    }

    private class Key(
        val model: GeoGunModel,
        val texture: ResourceLocation,
        val quads: RenderType,
        val triangles: RenderType,
        val emissive: ResourceLocation?,
        val bones: IntArray
    ) {
        override fun equals(other: Any?): Boolean = other is Key && model === other.model &&
                texture == other.texture && quads == other.quads && triangles == other.triangles &&
                emissive == other.emissive && bones.contentEquals(other.bones)

        override fun hashCode(): Int = 31 * (31 * (31 * (31 * (31 *
                System.identityHashCode(model) + texture.hashCode()) + quads.hashCode()) +
                triangles.hashCode()) + (emissive?.hashCode() ?: 0)) + bones.contentHashCode()
    }

    fun tryRender(
        model: GeoGunModel, texture: ResourceLocation?, quads: RenderType, triangles: RenderType,
        emissive: ResourceLocation?, pose: PoseStack, sourceBuffer: MultiBufferSource, light: Int, overlay: Int
    ): Boolean {
        if (depth == 0 || texture == null || sourceBuffer !is MultiBufferSource.BufferSource ||
            ShaderCompat.isShaderPackActive() ||
            !StaticMeshRenderer.isOwnerEnabled(owner)) return false

        // Snapshot the FINAL pose and visibility, including magazine, projectile, script and ammo-bar
        // changes. Equal meshes share a VBO; animated poses never accidentally reuse stale geometry.
        val bones = IntArray(model.baseModel.boneCount() * 15)
        var cursor = 0
        for (definition in model.baseModel.bones()) {
            val bone = model.instance.getBone(definition.index()) ?: continue
            bones[cursor++] = bone.x.toBits()
            bones[cursor++] = bone.y.toBits()
            bones[cursor++] = bone.z.toBits()
            bones[cursor++] = bone.rotation.x.toBits()
            bones[cursor++] = bone.rotation.y.toBits()
            bones[cursor++] = bone.rotation.z.toBits()
            bones[cursor++] = bone.rotation.w.toBits()
            bones[cursor++] = bone.rotationInEuler.x.toBits()
            bones[cursor++] = bone.rotationInEuler.y.toBits()
            bones[cursor++] = bone.rotationInEuler.z.toBits()
            bones[cursor++] = bone.xScale.toBits()
            bones[cursor++] = bone.yScale.toBits()
            bones[cursor++] = bone.zScale.toBits()
            bones[cursor++] = if (bone.visible) 1 else 0
            bones[cursor++] = if (bone.illuminated) 1 else 0
        }
        val key = Key(model, texture, quads, triangles, emissive, bones)
        // Attachments were submitted before the gun. Flush them before an immediate VBO draw.
        sourceBuffer.endBatch()
        return StaticMeshRenderer.tryDraw(owner, key, { collector ->
            val localPose = PoseStack()
            val source = MultiBufferSource { type ->
                val mode = type.mode()
                // Split fullbright bones from ordinary vertices: instance light is supplied at draw
                // time, while the illuminated material must stay fullbright after the split.
                val illuminated = if (type == triangles) EmissiveMeshRenderTypes.cutout(texture, mode)
                    else EmissiveMeshRenderTypes.create(texture, mode)
                collector.buffer(type, illuminated, mode)
            }
            model.baseModel.renderToBuffer(
                model.instance, localPose, source, quads, triangles, 0, 0,
                1f, 1f, 1f, 1f, true
            )
            if (emissive != null) {
                val glow = MultiBufferSource { type -> collector.buffer(type, type.mode()) }
                model.baseModel.renderToBuffer(
                    model.instance, localPose, glow, RenderType.eyes(emissive),
                    com.atsuishio.superbwarfare.client.renderer.ModRenderTypes.polyMeshEyes(emissive),
                    0, 0, 1f, 1f, 1f, 1f, true
                )
            }
            true
        }, pose, light, overlay).drawn()
    }

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        StaticMeshRenderer.invalidateOwner(owner)
    }
}
