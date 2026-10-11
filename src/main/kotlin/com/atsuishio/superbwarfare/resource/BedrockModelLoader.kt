package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.client.renderer.gun.GunEmissiveTextures
import com.atsuishio.superbwarfare.resource.model.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(
    bus = Mod.EventBusSubscriber.Bus.MOD,
    modid = com.atsuishio.superbwarfare.Mod.MODID,
    value = [Dist.CLIENT]
)
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
        // 只是清一下"哪张枪械贴图有 _e 自发光层"的缓存：这个答案只在换资源包时才会变。
        event.registerReloadListener(GunEmissiveTextures)
    }
}
