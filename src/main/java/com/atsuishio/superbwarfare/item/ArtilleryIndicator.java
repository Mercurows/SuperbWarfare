package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.screens.ArtilleryIndicatorScreen;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class ArtilleryIndicator extends Item implements ItemScreenProvider {

    public static final String TAG_CANNON = "Cannons";

    public ArtilleryIndicator() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        TooltipTool.addScreenProviderText(tooltipComponents);
        tooltipComponents.add(Component.translatable("des.superbwarfare.artillery_indicator_1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.superbwarfare.artillery_indicator_2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.superbwarfare.artillery_indicator_3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.superbwarfare.artillery_indicator_4").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(" ").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.superbwarfare.artillery_indicator_5").withStyle(ChatFormatting.GRAY));
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
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(pPlayer.getItemInHand(pHand));
        }
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

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Screen getItemScreen(ItemStack stack, Player player, InteractionHand hand) {
        return new ArtilleryIndicatorScreen(stack, hand);
    }
}
