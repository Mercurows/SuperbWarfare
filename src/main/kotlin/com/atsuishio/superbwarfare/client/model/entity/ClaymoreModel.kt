package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.ClaymoreEntity
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class ClaymoreModel : GeoModel<ClaymoreEntity>() {
    override fun getAnimationResource(entity: ClaymoreEntity) = null

    override fun getModelResource(entity: ClaymoreEntity) = loc("geo/claymore.geo.json")

    override fun getTextureResource(entity: ClaymoreEntity): ResourceLocation {
        val uuid = entity.getUUID()
        if (uuid.leastSignificantBits % 514 == 0L) {
            return loc("textures/entity/claymore_alter.png")
        }
        return loc("textures/entity/claymore.png")
    }
}
