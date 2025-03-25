package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

//public class Rocket extends Item implements GeoItem {
// TODO rewrite
public class Rocket extends Item {
    //    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public Rocket() {
        super(new Properties().stacksTo(16));
    }

//    @Override
//    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
//        super.initializeClient(consumer);
//        consumer.accept(new IClientItemExtensions() {
//            private final BlockEntityWithoutLevelRenderer renderer = new RocketItemRenderer();
//
//            @Override
//            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
//                return renderer;
//            }
//        });
//    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }


//    @Override
//    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
//    }

//    @Override
//    public AnimatableInstanceCache getAnimatableInstanceCache() {
//        return this.cache;
//    }

//    @Override
//    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
//        Multimap<Attribute, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
//        if (slot == EquipmentSlot.MAINHAND) {
//            map = HashMultimap.create(map);
//            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier", 6d, AttributeModifier.Operation.ADDITION));
//            map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Item modifier", -2.4, AttributeModifier.Operation.ADDITION));
//        }
//        return map;
//    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, LivingEntity entity, @NotNull LivingEntity source) {
        if (entity.level() instanceof ServerLevel level && Math.random() < 0.25) {

            level.explode(source, source.getX(), source.getY() + 1, source.getZ(), 6, Level.ExplosionInteraction.NONE);
            level.explode(null, source.getX(), source.getY() + 1, source.getZ(), 6, Level.ExplosionInteraction.NONE);

            if (!source.level().isClientSide() && source.getServer() != null) {
                ParticleTool.spawnMediumExplosionParticles(source.level(), source.getPosition(0));
            }

            if (source instanceof ServerPlayer player) {
                // TODO criteria
//                CriteriaRegister.RPG_MELEE_EXPLOSION.trigger(player);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            } else {
                stack.shrink(1);
            }
        }

        return super.hurtEnemy(stack, entity, source);
    }

}