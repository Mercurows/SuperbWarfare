package com.atsuishio.superbwarfare.client.renderer.armor

import com.atsuishio.superbwarfare.client.model.armor.UsChestIotvModel
import com.atsuishio.superbwarfare.item.armor.UsChestIotvItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoArmorRenderer

class UsChestIotvArmorRenderer : GeoArmorRenderer<UsChestIotvItem?>(UsChestIotvModel()) {
    init {
        this.body = GeoBone(null, "armorBody", false, 0.0, false, false)
        this.rightArm = GeoBone(null, "armorRightArm", false, 0.0, false, false)
        this.leftArm = GeoBone(null, "armorLeftArm", false, 0.0, false, false)
    }

    override fun getRenderType(
        animatable: UsChestIotvItem?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
