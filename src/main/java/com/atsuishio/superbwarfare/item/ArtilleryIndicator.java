package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class ArtilleryIndicator extends Item {

    public static final String TAG_CANNON = "Cannons";

    public ArtilleryIndicator() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        pPlayer.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
        pPlayer.startUsingItem(pHand);
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        pLivingEntity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
        return pStack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pTimeCharged) {
        pLivingEntity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }

    public boolean addCannon(ItemStack stack, String uuid) {
        var tag = NBTTool.getTag(stack);
        ListTag tags = tag.getList(TAG_CANNON, Tag.TAG_COMPOUND);
        List<CompoundTag> list = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            list.add(tags.getCompound(i));
        }
        for (var t : list) {
            if (t.getString("UUID").equals(uuid)) {
                return false;
            }
        }
        CompoundTag mortar = new CompoundTag();
        mortar.putString("UUID", uuid);
        list.add(mortar);

        ListTag listTag = new ListTag();
        listTag.addAll(list);
        tag.put(TAG_CANNON, listTag);
        NBTTool.saveTag(stack, tag);

        return true;
    }

    public boolean removeCannon(ItemStack stack, String uuid) {
        var tag = NBTTool.getTag(stack);
        ListTag tags = tag.getList(TAG_CANNON, Tag.TAG_COMPOUND);
        List<CompoundTag> list = new ArrayList<>();
        boolean flag = false;
        for (int i = 0; i < tags.size(); i++) {
            var t = tags.getCompound(i);
            if (t.getString("UUID").equals(uuid)) {
                flag = true;
                continue;
            }
            list.add(tags.getCompound(i));
        }
        if (flag) {
            ListTag listTag = new ListTag();
            listTag.addAll(list);
            tag.put(TAG_CANNON, listTag);
            NBTTool.saveTag(stack, tag);
        }

        return flag;
    }

//    @Override
//    @ParametersAreNonnullByDefault
//    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
//        if (!player.isCrouching()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
//
//        var stack = player.getItemInHand(usedHand);
//        var isDepressed = !stack.getOrCreateTag().getBoolean("IsDepressed");
//
//        stack.getOrCreateTag().putBoolean("IsDepressed", isDepressed);
//
//        player.displayClientMessage(Component.translatable(
//                isDepressed
//                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
//                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
//        ).withStyle(ChatFormatting.GREEN), true);
//
//        return InteractionResultHolder.success(stack);
//    }
//
//    @Override
//    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
//        pTooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
//                .append(Component.literal("[" + pStack.getOrCreateTag().getInt("TargetX")
//                        + "," + pStack.getOrCreateTag().getInt("TargetY")
//                        + "," + pStack.getOrCreateTag().getInt("TargetZ") + "]")));
//
//
//        pTooltipComponents.add(Component.translatable(
//                pStack.getOrCreateTag().getBoolean("IsDepressed")
//                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
//                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
//        ).withStyle(ChatFormatting.GRAY));
//    }
}
