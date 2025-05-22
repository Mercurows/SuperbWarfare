package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.Mk82Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class MediumAerialBomb extends Item implements ProjectileItem {

    public MediumAerialBomb() {
        super(new Properties().stacksTo(2));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.medium_aerial_bomb").withStyle(ChatFormatting.GRAY));
    }

    public static class MediumAerialBombDispenseBehavior extends ProjectileDispenseBehavior {
        public MediumAerialBombDispenseBehavior() {
            super(ModItems.MEDIUM_AERIAL_BOMB.get());
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.BOMB_RELEASE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
                .power(0.5F)
                .uncertainty(1)
                .build();
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new Mk82Entity(ModEntities.MK_82.get(), pos.x(), pos.y(), pos.z(), level);
    }
}