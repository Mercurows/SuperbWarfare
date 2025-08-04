package com.atsuishio.superbwarfare.item.curio;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ParachuteItem extends Item implements ICurioItem {

    public static final String TAG_OPEN = "Open";

    public ParachuteItem() {
        super(new Properties().stacksTo(1).durability(600));
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack pStack, ItemStack pRepairCandidate) {
        return pRepairCandidate.is(Items.PHANTOM_MEMBRANE);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(c -> c.findFirstCurio(this))
                .isEmpty();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (stack.getOrCreateTag().getBoolean(TAG_OPEN)) {
            if ((entity.onGround() || entity.isInWater()) || entity.isFallFlying() || entity.getVehicle() != null || (entity instanceof Player player && player.getAbilities().flying)) {
                stack.getOrCreateTag().putBoolean(TAG_OPEN, false);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PARACHUTE_CLOSE.get(), SoundSource.PLAYERS, 1f, 1);
            }
            if (entity instanceof Player player) {
                if (player.level().isClientSide) {
                    player.addDeltaMovement(new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize().scale(0.05));
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1.03, 0.75, 1.03));
                }
            }
            if (entity.tickCount % 40 == 0) {
                stack.hurtAndBreak(1, entity, p -> {
                });
            }
            entity.resetFallDistance();
        }
    }

    public static boolean isParachuteOpen(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(c -> c.findFirstCurio(ModItems.PARACHUTE.get())
                        .map(slotResult -> slotResult.stack().getOrCreateTag().getBoolean(ParachuteItem.TAG_OPEN))
                        .orElse(false)
                ).orElse(false);
    }
}
