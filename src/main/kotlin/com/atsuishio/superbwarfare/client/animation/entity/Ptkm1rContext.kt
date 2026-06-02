package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import net.minecraft.resources.ResourceLocation

open class Ptkm1rContext(entity: Ptkm1rEntity) : BasicEntityContext<Ptkm1rEntity>(entity, ANIM) {
    override fun init(location: ResourceLocation) {
        val ani = ProjectileModelReloadListener.getAnimation(location)
        for (entry in ani!!) {
            animations[entry.name] = entry
        }
    }

    companion object {
        val ANIM = loc("animations/bedrock/projectile/ptkm_1r.animation.json")
    }
}