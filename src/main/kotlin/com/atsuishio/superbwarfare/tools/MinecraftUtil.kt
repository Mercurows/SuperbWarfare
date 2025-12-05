package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
val mc: Minecraft = Minecraft.getInstance()

@OnlyIn(Dist.CLIENT)
val localPlayer = mc.player

@OnlyIn(Dist.CLIENT)
val font: Font = mc.font

@OnlyIn(Dist.CLIENT)
val options: Options = mc.options

operator fun BlockPos.component1() = this.x
operator fun BlockPos.component2() = this.y
operator fun BlockPos.component3() = this.z

fun Player?.isNullOrSpector() = this == null || this.isSpectator

fun Vec3?.toFormattedString(): String {
    if (this == null) return "[ ---, ---, --- ]"
    return "[ " + format0D(x) + ", " + format0D(y) + ", " + format0D(z) + " ]"
}