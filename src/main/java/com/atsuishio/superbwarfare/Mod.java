package com.atsuishio.superbwarfare;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.client.renderer.molang.MolangVariable;
import com.atsuishio.superbwarfare.client.sound.ModSoundInstances;
import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import com.atsuishio.superbwarfare.compat.coldsweat.ColdSweatCompatHandler;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.config.ClientConfig;
import com.atsuishio.superbwarfare.config.CommonConfig;
import com.atsuishio.superbwarfare.config.ServerConfig;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@net.neoforged.fml.common.Mod(Mod.MODID)
public class Mod {

    public static final String MODID = "superbwarfare";
    public static final ResourceLocation ATTRIBUTE_MODIFIER = loc("attribute_modifier");

    public static final Logger LOGGER = LogManager.getLogger(Mod.class);

    public Mod(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.init());
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.init());

        ModPerks.register(bus);
        ModSerializers.REGISTRY.register(bus);
        ModSounds.REGISTRY.register(bus);
        ModBlocks.REGISTRY.register(bus);
        ModBlockEntities.REGISTRY.register(bus);
        ModItems.register(bus);
        ModDataComponents.register(bus);
        ModTabs.TABS.register(bus);
        ModEntities.REGISTRY.register(bus);
        ModMobEffects.REGISTRY.register(bus);
        ModParticleTypes.REGISTRY.register(bus);
        ModPotion.POTIONS.register(bus);
        ModMenuTypes.REGISTRY.register(bus);
        ModVillagers.register(bus);
        ModRecipes.RECIPE_SERIALIZERS.register(bus);
        ModArmorMaterials.MATERIALS.register(bus);
        ModAttributes.ATTRIBUTES.register(bus);
        ModCriteriaTriggers.REGISTRY.register(bus);
        ModAttachments.ATTACHMENT_TYPES.register(bus);

        bus.addListener(this::onClientSetup);
        bus.addListener(FMLCommonSetupEvent.class, event -> onCommonSetup(bus));
        bus.addListener(ModItems::registerDispenserBehavior);

        bus.addListener(NetworkRegistry::register);

        registerDataTickets();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CompatHolder.hasMod(CompatHolder.CLOTH_CONFIG, ClothConfigHelper::registerScreen);
        }
        if (ColdSweatCompatHandler.hasMod()) {
            NeoForge.EVENT_BUS.addListener(ColdSweatCompatHandler::onPlayerInVehicle);
        }

        NeoForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }


    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();
    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueueC = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
    }

    public static void queueClientWork(int tick, Runnable action) {
        workQueueC.add(new AbstractMap.SimpleEntry<>(action, tick));
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        executeWork(workQueue);
    }

    @SubscribeEvent
    public void tick(ClientTickEvent.Post event) {
        executeWork(workQueueC);
    }

    private void executeWork(Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueueC) {
        List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
        workQueueC.forEach(work -> {
            work.setValue(work.getValue() - 1);
            if (work.getValue() == 0)
                actions.add(work);
        });
        actions.forEach(e -> e.getKey().run());
        workQueueC.removeAll(actions);
    }

    public void onCommonSetup(IEventBus bus) {
        bus.post(new RegisterContainersEvent());
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        MouseMovementHandler.init();
        MolangVariable.register();
        event.enqueueWork(ModSoundInstances::init);
    }

    private void registerDataTickets() {
        FuMO25BlockEntity.FUMO25_TICK = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(loc("fumo25_tick")));
    }
}
