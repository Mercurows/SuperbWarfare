package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.client.animation.gun.GunSprintAnimationLoader
import com.atsuishio.superbwarfare.client.renderer.block.DisplayBoardGunMesh
import com.atsuishio.superbwarfare.client.renderer.gun.GunEmissiveTextures
import com.atsuishio.superbwarfare.resource.model.*
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber
object BedrockModelLoader {
    @SubscribeEvent
    fun onAddClientResourceListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(VehicleModelReloadListener)
        event.registerReloadListener(VehicleLODModelReloadListener)
        event.registerReloadListener(ProjectileModelReloadListener)
        event.registerReloadListener(EntityModelReloadListener)
        event.registerReloadListener(ArmorModelReloadListener)
        event.registerReloadListener(BlockModelReloadListener)
        event.registerReloadListener(ItemModelReloadListener)
        event.registerReloadListener(GunModelReloadListener)
        event.registerReloadListener(GunLODModelReloadListener)
        event.registerReloadListener(ShellModelReloadListener)
        event.registerReloadListener(AttachmentModelReloadListener)
        // 奔跑样张：第一人称的程序性奔跑姿势从它的 json 里读。
        event.registerReloadListener(GunSprintAnimationLoader)
        // 只是清一下"哪张枪械贴图有 _e 自发光层"的缓存：这个答案只在换资源包时才会变。
        event.registerReloadListener(GunEmissiveTextures)
        event.registerReloadListener(DisplayBoardGunMesh)
    }
}
