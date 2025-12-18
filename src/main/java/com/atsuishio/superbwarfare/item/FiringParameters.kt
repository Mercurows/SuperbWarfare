package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.FiringParametersScreen
import com.atsuishio.superbwarfare.tools.component1
import com.atsuishio.superbwarfare.tools.component2
import com.atsuishio.superbwarfare.tools.component3
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

var ItemStack.firingParameters: FiringParameters.Parameters
    get() {
        val tag = tag ?: return FiringParameters.Parameters()
        val x = tag.getInt("TargetX")
        val y = tag.getInt("TargetY")
        val z = tag.getInt("TargetZ")
        val radius = tag.getInt("Radius")
        val isDepressed = tag.getBoolean("IsDepressed")
        return FiringParameters.Parameters(BlockPos(x, y, z), radius, isDepressed)
    }
    set(value) {
        val tag = orCreateTag
        tag.putInt("TargetX", value.pos.x)
        tag.putInt("TargetY", value.pos.y)
        tag.putInt("TargetZ", value.pos.z)
        tag.putInt("Radius", value.radius)
        tag.putBoolean("IsDepressed", value.isDepressed)
    }

class FiringParameters : Item(Properties().stacksTo(1)), ItemScreenProvider {

    @JvmRecord
    data class Parameters(val pos: BlockPos, val radius: Int, val isDepressed: Boolean) {
        constructor(pos: BlockPos, isDepressed: Boolean) : this(pos, 0, isDepressed)

        @JvmOverloads
        constructor(pos: BlockPos = BlockPos(0, 0, 0)) : this(pos, 0, false)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS

        val stack = context.itemInHand
        val pos = context.clickedPos.relative(context.clickedFace)

        if (player.isShiftKeyDown) {
            stack.firingParameters = stack.firingParameters.copy(pos = pos)
        }

        return InteractionResult.SUCCESS
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        TooltipTool.addScreenProviderText(tooltipComponents)

        val (pos, radius, isDepressed) = stack.firingParameters
        val (x, y, z) = pos

        tooltipComponents.add(
            Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[$x, $y, $z]"))
        )
        tooltipComponents.add(
            Component.translatable("tips.superbwarfare.mortar.target_pos.radius", radius).withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable(
                if (isDepressed)
                    "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
                else
                    "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    @OnlyIn(Dist.CLIENT)
    override fun getItemScreen(stack: ItemStack, player: Player?, hand: InteractionHand?): Screen {
        return FiringParametersScreen(stack, hand)
    }
}
