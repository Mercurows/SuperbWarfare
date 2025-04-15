package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AmmoSupplierItem extends Item {

    public final Ammo type;
    public final int ammoToAdd;

    public AmmoSupplierItem(Ammo type, int ammoToAdd, Properties properties) {
        super(properties);
        this.type = type;
        this.ammoToAdd = ammoToAdd;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.ammo_supplier").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int count = stack.getCount();
        player.getCooldowns().addCooldown(this, 10);

        if (!player.isCreative()) {
            stack.shrink(count);
        }

        ItemStack offhandItem = player.getOffhandItem();

        if (offhandItem.is(ModItems.AMMO_BOX.get())) {
            this.type.add(offhandItem, ammoToAdd * count);
        } else {
            var capability = player.getData(ModAttachments.PLAYER_VARIABLE).watch();

            this.type.add(capability, ammoToAdd * count);
            player.setData(ModAttachments.PLAYER_VARIABLE, capability);
            capability.sync(player);
        }

        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("item.superbwarfare.ammo_supplier.supply", Component.translatable(this.type.translationKey), ammoToAdd * count), true);
            level.playSound(null, player.blockPosition(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.success(stack);
    }
}
