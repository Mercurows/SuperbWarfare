package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.FiringParameters;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(Registries.ITEM, ModUtils.MODID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModUtils.MODID);

    public static final DeferredHolder<Item, FiringParameters> FIRING_PARAMETERS = ITEMS.register("firing_parameters", FiringParameters::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);

        REGISTRY.register(bus);
    }

}
