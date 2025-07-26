package com.atsuishio.superbwarfare.item.curio;

import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class Parachute extends Item implements ICurioItem {
    public Parachute() {
        super(new Properties().stacksTo(1).durability(600));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .flatMap(c -> c.findFirstCurio(this))
                .isEmpty();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        var tag = NBTTool.getTag(stack);
        if ((entity.onGround() || entity.isInWater()) && tag.getBoolean("open")) {
            tag.putBoolean("open", false);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PARACHUTE_CLOSE.get(), SoundSource.PLAYERS, 1f, 1);
            NBTTool.saveTag(stack, tag);
        }
        if (tag.getBoolean("open") && entity instanceof Player player) {
            if (player.level().isClientSide) {
                player.addDeltaMovement(new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize().scale(0.05));
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.03, 0.75, 1.03));
            }
            if (player.tickCount % 40 == 0 && player.level() instanceof ServerLevel serverLevel) {
                stack.hurtAndBreak(1, serverLevel, player, item -> {
                });
            }
            player.resetFallDistance();
        }
    }
}
