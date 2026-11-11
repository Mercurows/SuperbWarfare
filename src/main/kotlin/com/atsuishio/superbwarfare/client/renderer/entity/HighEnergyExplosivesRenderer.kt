package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.TntRenderer

/** Vanilla fuse swelling and flash, using the entity's synchronized explosive block state. */
class HighEnergyExplosivesRenderer(context: EntityRendererProvider.Context) : TntRenderer(context)
