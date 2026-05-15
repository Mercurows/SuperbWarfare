package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.client.MouseMovementHandler
import com.atsuishio.superbwarfare.client.renderer.molang.MolangVariable
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import com.atsuishio.superbwarfare.compat.coldsweat.ColdSweatCompatHandler
import com.atsuishio.superbwarfare.config.CLIENT_CONFIG
import com.atsuishio.superbwarfare.config.COMMON_CONFIG
import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.network.initializeNetwork
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddPackFindersEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private typealias Task = AbstractMap.SimpleEntry<Runnable, Int>

@Mod(com.atsuishio.superbwarfare.Mod.MODID)
class Mod(bus: IEventBus, container: ModContainer) {
    init {
        with(container) {
            registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG)
            registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG)
            registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG)
        }

        ModPerks.register(bus)
        ModSerializers.REGISTRY.register(bus)
        ModSounds.REGISTRY.register(bus)
        ModBlocks.REGISTRY.register(bus)
        ModBlockEntities.REGISTRY.register(bus)
        ModItems.register(bus)
        ModDataComponents.register(bus)
        ModTabs.TABS.register(bus)
        ModEntities.REGISTRY.register(bus)
        ModMobEffects.REGISTRY.register(bus)
        ModParticleTypes.REGISTRY.register(bus)
        ModPotions.register(bus)
        ModMenuTypes.REGISTRY.register(bus)
        ModVillagers.register(bus)
        ModRecipes.register(bus)
        ModArmorMaterial.MATERIALS.register(bus)
        ModAttributes.ATTRIBUTES.register(bus)
        ModCriteriaTriggers.REGISTRY.register(bus)
        ModAttachments.ATTACHMENT_TYPES.register(bus)
        ModCommandArguments.COMMAND_ARGUMENT_TYPES.register(bus)

        bus.addListener<FMLClientSetupEvent> { onClientSetup(it) }
        bus.addListener<FMLCommonSetupEvent> { onCommonSetup(bus, it) }
        bus.addListener<FMLCommonSetupEvent> { ModItems.registerDispenserBehavior() }

        bus.addListener<RegisterPayloadHandlersEvent> { initializeNetwork(it) }
        bus.addListener<AddPackFindersEvent> { onRegisterBuiltInResourcePacks(it) }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CompatHolder.hasMod(CompatHolder.CLOTH_CONFIG) { ClothConfigHelper.registerScreen() }
        }

        if (ColdSweatCompatHandler.hasMod()) {
            NeoForge.EVENT_BUS.addListener(ColdSweatCompatHandler::onPlayerInVehicle)
        }

        NeoForge.EVENT_BUS.register(this)

        CustomData.load()
    }

    @SubscribeEvent
    @Suppress("unused")
    private fun tick(event: ServerTickEvent.Post) = executeWork(SERVER_QUEUE)

    @SubscribeEvent
    @Suppress("unused")
    private fun tick(event: ClientTickEvent.Post) = executeWork(CLIENT_QUEUE)

    private fun executeWork(workQueueC: MutableCollection<Task>) {
        workQueueC.removeAll(
            workQueueC
                .onEach { it.setValue(it.value - 1) }
                .filter { it.value <= 0 }
                .onEach { it.key.run() }
                .toSet()
        )
    }

    private fun onCommonSetup(bus: IEventBus, event: FMLCommonSetupEvent) {
        bus.post(RegisterContainersEvent())
        event.enqueueWork { ModGameRules.bootstrap() }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        MouseMovementHandler.init()
        MolangVariable.register()
        event.enqueueWork { ModSoundInstances.init() }
    }

    private fun onRegisterBuiltInResourcePacks(event: AddPackFindersEvent) {
        event.addPackFinders(
            loc("resourcepacks/sbw_legacy"),
            PackType.CLIENT_RESOURCES,
            Component.translatable("pack.superbwarfare.sbw_legacy"),
            PackSource.BUILT_IN,
            false,
            Pack.Position.TOP
        )
    }

    companion object {
        const val MODID: String = "superbwarfare"

        @JvmField
        val ATTRIBUTE_MODIFIER: ResourceLocation = loc("attribute_modifier")

        @JvmField
        val LOGGER: Logger = LogManager.getLogger(Mod::class.java)

        @JvmStatic
        fun loc(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MODID, path)

        private val SERVER_QUEUE: MutableCollection<Task> = ConcurrentLinkedQueue()
        private val CLIENT_QUEUE: MutableCollection<Task> = ConcurrentLinkedQueue()

        @JvmStatic
        fun queueServerWork(tick: Int, action: Runnable) = SERVER_QUEUE.add(AbstractMap.SimpleEntry(action, tick))

        @JvmStatic
        fun queueClientWork(tick: Int, action: Runnable) = CLIENT_QUEUE.add(AbstractMap.SimpleEntry(action, tick))
    }
}
