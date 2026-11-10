package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.block.entity.BoobyTrapBlockEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.HitResult
import snownee.jade.api.Accessor
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.callback.JadeRayTraceCallback

/** Use Jade's normal target replacement pipeline, as its vanilla camouflage overrides do. */
class BoobyTrapCamouflageProvider(private val registration: IWailaClientRegistration) : JadeRayTraceCallback {
    override fun onRayTrace(
        hitResult: HitResult,
        accessor: Accessor<*>?,
        originalAccessor: Accessor<*>?
    ): Accessor<*>? {
        val block = accessor as? BlockAccessor ?: return accessor
        if (block.block !is BoobyTrapBlock || !block.blockState.getValue(BoobyTrapBlock.CAMOUFLAGED)) return accessor
        val material = (block.blockEntity as? BoobyTrapBlockEntity)?.camouflage ?: return accessor
        return registration.blockAccessor().from(block)
            .blockState(material)
            .blockEntity { null }
            .fakeBlock(ItemStack(material.block))
            .build()
    }
}
