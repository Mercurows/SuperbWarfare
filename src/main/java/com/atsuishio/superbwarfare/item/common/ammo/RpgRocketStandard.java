package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity;
import com.atsuishio.superbwarfare.init.ModCriteriaTriggers;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class RpgRocketStandard extends Item implements ProjectileItem {

    public RpgRocketStandard() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var list = new ArrayList<>(super.getDefaultAttributeModifiers(stack).modifiers());

        list.addAll(List.of(
                new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                ),
                new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
        ));

        return new ItemAttributeModifiers(list, true);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, LivingEntity entity, @NotNull LivingEntity source) {
        if (entity.level() instanceof ServerLevel level && Math.random() < 0.25) {

            level.explode(source, source.getX(), source.getY() + 1, source.getZ(), 5, Level.ExplosionInteraction.NONE);
            level.explode(null, source.getX(), source.getY() + 1, source.getZ(), 5, Level.ExplosionInteraction.NONE);

            if (!source.level().isClientSide() && source.getServer() != null) {
                ParticleTool.spawnMediumExplosionParticles(source.level(), source.position());
            }

            if (source instanceof ServerPlayer player) {
                ModCriteriaTriggers.RPG_MELEE_EXPLOSION.get().trigger(player);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            } else {
                stack.shrink(1);
            }
        }

        return super.hurtEnemy(stack, entity, source);
    }

    public static class RocketDispenseBehavior extends ProjectileDispenseBehavior {
        public RocketDispenseBehavior() {
            super(ModItems.RPG_ROCKET_STANDARD.get());
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.RPG_FIRE_3P.get(), SoundSource.BLOCKS, 1F, 1F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new RpgRocketStandardEntity(ModEntities.RPG_ROCKET_STANDARD.get(), pos.x(), pos.y(), pos.z(), level, 340, 80, 5);
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
                .power(2)
                .build();
    }
}