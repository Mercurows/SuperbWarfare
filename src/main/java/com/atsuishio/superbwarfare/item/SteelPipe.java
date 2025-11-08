package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SteelPipe extends SwordItem {

    public SteelPipe() {
        super(ModItemTier.STEEL, new CustomDamageProperty(810)
                .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 4, -3))
        );
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pAttacker.level().playSound(null, pTarget.getOnPos(), ModSounds.STEEL_PIPE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    // TODO 音效，特殊攻击效果等

    @Override
    @ParametersAreNonnullByDefault
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

        if (stack.isEmpty()) {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Holder.direct(Items.STICK), 1, stack.getComponentsPatch()));
        }
    }

    @SubscribeEvent
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean shouldBobAsEntity(@NotNull ItemStack stack) {
                return false;
            }
        }, ModItems.STEEL_PIPE);
    }
}
