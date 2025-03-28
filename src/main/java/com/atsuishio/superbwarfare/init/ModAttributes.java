package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, Mod.MODID);

    public static final DeferredHolder<Attribute, Attribute> BULLET_RESISTANCE = ATTRIBUTES.register("bullet_resistance", () -> (new RangedAttribute("attribute." + Mod.MODID + ".bullet_resistance", 0, 0, 1)).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        List<EntityType<? extends LivingEntity>> entityTypes = event.getTypes();
        entityTypes.forEach((e) -> {
            Class<? extends Entity> baseClass = e.getBaseClass();
            if (baseClass.isAssignableFrom(LivingEntity.class)) {
                event.add(e, BULLET_RESISTANCE);
            }
        });
    }
}
