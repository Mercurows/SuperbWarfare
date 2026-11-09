package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.SeaMineItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.SeaMineEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("sea_mine")
open class SeaMineItem : Item(Properties().stacksTo(16)), DispenserLaunchable {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { SeaMineItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels) }
            override fun getCustomRenderer() = renderer
        })
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        val chains = chainLength(stack)
        lines.add(
            Component.translatable(
                when {
                    chains > 0 -> "des.superbwarfare.sea_mine.moored"
                    hasBallast(stack) -> "des.superbwarfare.sea_mine.bottom"
                    else -> "des.superbwarfare.sea_mine.floating"
                }
            ).withStyle(ChatFormatting.GRAY)
        )
        if (chains > 0) lines.add(
            Component.translatable("des.superbwarfare.sea_mine.chain", chains).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!level.isClientSide) {
            val mine = SeaMineEntity(ModEntities.SEA_MINE.get(), level)
            mine.configure(stack, player.uuid)
            mine.setPos(player.x, player.eyeY - 0.2, player.z)
            mine.deltaMovement = player.lookAngle.scale(0.45).add(player.deltaMovement)
            level.addFreshEntity(mine)
            if (!player.isCreative) stack.shrink(1)
        }
        player.cooldowns.addCooldown(this, 12)
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun getLaunchBehavior() = object : DefaultDispenseItemBehavior() {
        override fun execute(source: BlockSource, stack: ItemStack): ItemStack {
            val direction = source.blockState.getValue(DispenserBlock.FACING)
            val pos = DispenserBlock.getDispensePosition(source)
            val mine = SeaMineEntity(ModEntities.SEA_MINE.get(), source.level)
            mine.configure(stack, null)
            mine.setPos(pos.x(), pos.y(), pos.z())
            mine.setDeltaMovement(direction.stepX * 0.3, direction.stepY * 0.3, direction.stepZ * 0.3)
            source.level.addFreshEntity(mine)
            stack.shrink(1)
            return stack
        }
    }

    companion object {
        const val BALLAST = "Ballast"
        const val CHAIN_LENGTH = "ChainLength"
        const val MAX_CHAIN_LENGTH = 256

        @JvmStatic
        @JvmOverloads
        fun makeInstance(ballast: Boolean = false, chainLength: Int = 0): ItemStack {
            val chains = chainLength.coerceIn(0, MAX_CHAIN_LENGTH)
            return ItemStack(ModItems.SEA_MINE.get()).apply {
                if (ballast || chains > 0) orCreateTag.putBoolean(BALLAST, true)
                if (chains > 0) orCreateTag.putInt(CHAIN_LENGTH, chains)
            }
        }

        @JvmStatic
        fun chainLength(stack: ItemStack) = (stack.tag?.getInt(CHAIN_LENGTH) ?: 0).coerceIn(0, MAX_CHAIN_LENGTH)

        @JvmStatic
        fun hasBallast(stack: ItemStack) = stack.tag?.getBoolean(BALLAST) == true || chainLength(stack) > 0
    }
}
