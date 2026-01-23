package com.atsuishio.superbwarfare.api.event

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemDisplayContext
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus
import software.bernie.geckolib.cache.`object`.GeoBone

@Cancelable
@ApiStatus.AvailableSince("0.8.7.1")
class RenderPlayerArmEvent(
    val localPlayer: LocalPlayer,
    val transformType: ItemDisplayContext,
    val stack: PoseStack,
    val arm: HumanoidArm,
    val bone: GeoBone,
    val currentBuffer: MultiBufferSource,
    val renderType: RenderType,
    val packedLightIn: Int,
    @get:JvmName("isUseOldHandRender") val useOldHandRender: Boolean,
) : Event()
