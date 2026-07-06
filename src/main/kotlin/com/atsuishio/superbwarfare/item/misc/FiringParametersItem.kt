package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.FiringParametersScreen
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.item.ItemScreenProvider
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
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

var ItemStack.firingParameters: FiringParametersItem.Parameters
    get() = this.getOrDefault(ModDataComponents.FIRING_PARAMETERS, FiringParametersItem.Parameters())
    set(value) {
        set(ModDataComponents.FIRING_PARAMETERS, value)
    }

class FiringParametersItem : Item(Properties().stacksTo(1)), ItemScreenProvider, IVehicleInteract {
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
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
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
    override fun getItemScreen(stack: ItemStack, player: Player, hand: InteractionHand): Screen {
        return FiringParametersScreen(stack, hand)
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (vehicle !is ArtilleryEntity) return null
        if (!player.isShiftKeyDown) return null
        vehicle.setTarget(stack, player, "Main")
        return InteractionResult.SUCCESS
    }
}
