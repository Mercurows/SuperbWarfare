package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

public class Crowbar extends SwordItem {
    public Crowbar() {
        super(new Tier() {
            public int getUses() {
                return 400;
            }

            public float getSpeed() {
                return 4f;
            }

            public float getAttackDamageBonus() {
                return 3.5f;
            }

            @Override
            public @NotNull TagKey<Block> getIncorrectBlocksForDrops() {
                return BlockTags.INCORRECT_FOR_IRON_TOOL;
            }

            public int getLevel() {
                return 1;
            }

            public int getEnchantmentValue() {
                return 9;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.IRON_INGOT));
            }
        }, new Properties());
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack retval = new ItemStack(this);
        retval.setDamageValue(itemstack.getDamageValue() + 1);
        if (retval.getDamageValue() >= retval.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return retval;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack itemstack) {
        return true;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        super.useOn(context);
        if ((context.getLevel().getBlockState(BlockPos.containing(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()))).getBlock() == ModBlocks.JUMP_PAD.get()) {
            context.getLevel().setBlock(BlockPos.containing(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()), Blocks.AIR.defaultBlockState(), 3);

            if (context.getPlayer() != null) {
                ItemHandlerHelper.giveItemToPlayer(context.getPlayer(), new ItemStack(ModItems.JUMP_PAD.get()));
            }
        }
        return InteractionResult.SUCCESS;
    }


}
