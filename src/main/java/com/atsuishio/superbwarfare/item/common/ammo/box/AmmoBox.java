package com.atsuishio.superbwarfare.item.common.ammo.box;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
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

import javax.annotation.ParametersAreNonnullByDefault;
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

        player.getCooldowns().addCooldown(this, 10);

        var info = stack.get(ModDataComponents.AMMO_BOX_INFO);
        if (info == null) info = new AmmoBoxInfo("All", false);
        String selectedType = info.type();

        var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
        if (!level.isClientSide()) {
            var types = selectedType.equals("All") ? Ammo.values() : new Ammo[]{Ammo.getType(selectedType)};

            for (var type : types) {
                if (type == null) continue;

                if (player.isCrouching()) {
                    // 存入弹药
                    type.add(stack, type.get(cap));
                    type.set(cap, 0);
                } else {
                    // 取出弹药
                    type.add(cap, type.get(stack));
                    type.set(stack, 0);
                }
            }
            player.setData(ModAttachments.PLAYER_VARIABLE, cap);
            cap.sync(player);
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1, 1);

            // 取出弹药时，若弹药盒为掉落物版本，则移除弹药盒物品
            if (!player.isCrouching() && info.isDrop()) {
                stack.shrink(1);
            }
        }
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
    @ParametersAreNonnullByDefault
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player player && player.isCrouching()) {
            var info = stack.get(ModDataComponents.AMMO_BOX_INFO) == null ? new AmmoBoxInfo("All", false) : stack.get(ModDataComponents.AMMO_BOX_INFO);
            assert info != null;

            var index = Math.max(0, ammoTypeList.indexOf(info.type()));
            var typeString = ammoTypeList.get((index + 1) % ammoTypeList.size());

            stack.set(ModDataComponents.AMMO_BOX_INFO, new AmmoBoxInfo(typeString, info.isDrop()));
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
    public void appendHoverText(ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        var info = stack.get(ModDataComponents.AMMO_BOX_INFO);
        if (info == null) info = new AmmoBoxInfo("All", false);
        var type = Ammo.getType(info.type());

        tooltipComponents.add(Component.translatable("des.superbwarfare.ammo_box").withStyle(ChatFormatting.GRAY));

        for (var ammo : Ammo.values()) {
            tooltipComponents.add(Component.translatable("des.superbwarfare.ammo_box." + ammo.name).withStyle(ammo.color)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format0D(ammo.get(stack)) + ((type != ammo) ? " " : " ←-")).withStyle(ChatFormatting.BOLD)));
        }
    }
}
