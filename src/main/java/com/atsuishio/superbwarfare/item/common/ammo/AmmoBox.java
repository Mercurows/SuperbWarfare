package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AmmoBox extends Item {

    public AmmoBox() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.fail(stack);

        CompoundTag tag = stack.getOrCreateTag();
        player.getCooldowns().addCooldown(this, 10);
        String type = tag.getString("Type").isEmpty() ? "All" : tag.getString("Type");

        PlayerVariable.modify(player, cap -> {
            var types = (type.equals("All") || tag.getBoolean("IsDrop")) ? Ammo.values() : new Ammo[]{Ammo.getType(type)};

            for (var ammoType : types) {
                if (ammoType == null) return;

                if (player.isCrouching() && !tag.getBoolean("IsDrop")) {
                    // 存入弹药
                    ammoType.add(tag, ammoType.get(cap));
                    ammoType.set(cap, 0);
                } else {
                    // 取出弹药
                    ammoType.add(cap, ammoType.get(tag));
                    ammoType.set(tag, 0);
                }
            }

            if (!level.isClientSide()) {
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1, 1);
            }

            // 取出弹药时，若弹药盒为掉落物版本，则移除弹药盒物品
            if (tag.getBoolean("IsDrop")) {
                stack.shrink(1);
            }
        });
        return InteractionResultHolder.consume(stack);
    }

    private static final List<String> ammoTypeList = generateAmmoTypeList();

    private static List<String> generateAmmoTypeList() {
        var list = new ArrayList<String>();
        list.add("All");

        for (var ammoType : Ammo.values()) {
            list.add(ammoType.serializationName);
        }

        return list;
    }


    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.isCrouching()) {
            var tag = stack.getOrCreateTag();
            if (tag.getBoolean("IsDrop")) return false;

            var index = Math.max(0, ammoTypeList.indexOf(tag.getString("Type")));
            var typeString = ammoTypeList.get((index + 1) % ammoTypeList.size());

            tag.putString("Type", typeString);
            entity.playSound(ModSounds.FIRE_RATE.get(), 1f, 1f);

            var type = Ammo.getType(typeString);
            if (type == null) {
                player.displayClientMessage(Component.translatable("des.superbwarfare.ammo_box.type.all").withStyle(ChatFormatting.WHITE), true);
                return true;
            }

            player.displayClientMessage(
                    Component.translatable("des.superbwarfare.ammo_box.type." + type.name).withStyle(type.color),
                    true
            );
        }

        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        var type = Ammo.getType(stack.getOrCreateTag().getString("Type"));

        tooltip.add(Component.translatable("des.superbwarfare.ammo_box").withStyle(ChatFormatting.GRAY));

        for (var ammo : Ammo.values()) {
            tooltip.add(Component.translatable("des.superbwarfare.ammo_box." + ammo.name).withStyle(ammo.color)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format0D(ammo.get(stack)) + ((type != ammo) ? " " : " ←-")).withStyle(ChatFormatting.BOLD)));
        }
    }
}
