package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.vehicle.remote.RCOperatorBodyEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import java.lang.ref.WeakReference
import java.util.*

open class RCOperatorBodyRenderer(context: EntityRendererProvider.Context) : EntityRenderer<RCOperatorBodyEntity>(context) {
    private val players = WeakHashMap<RCOperatorBodyEntity, RemotePlayer>()

    protected open fun getRenderPlayer(body: RCOperatorBodyEntity): RemotePlayer? {
        val profile = body.operatorProfile ?: return null
        val level = body.level() as? ClientLevel ?: return null
        return players.getOrPut(body) {
            val bodyRef = WeakReference(body)
            object : RemotePlayer(level, profile) {
                override fun getVehicle(): Entity? = bodyRef.get()?.vehicle

                override fun isPassenger(): Boolean = getVehicle() != null

                override fun getRootVehicle(): Entity = getVehicle()?.rootVehicle ?: this
            }.apply { id = body.id }
        }
    }

    override fun render(
        entity: RCOperatorBodyEntity,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
    ) {
        val player = getRenderPlayer(entity) ?: return
        player.moveTo(entity.x, entity.y, entity.z, entity.yRot, entity.xRot)
        player.yBodyRot = entity.yBodyRot
        player.yBodyRotO = entity.yBodyRot
        player.yHeadRot = entity.yHeadRot
        player.yHeadRotO = entity.yHeadRot
        player.pose = entity.pose
        player.setMainArm(entity.mainArm)
        EquipmentSlot.entries.forEach { player.setItemSlot(it, entity.getItemBySlot(it)) }
        entityRenderDispatcher.getRenderer(player).render(player, yaw, partialTick, poseStack, buffer, packedLight)
    }

    override fun getTextureLocation(entity: RCOperatorBodyEntity): ResourceLocation =
        getRenderPlayer(entity)?.skinTextureLocation ?: DefaultPlayerSkin.getDefaultSkin()
}
