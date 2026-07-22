package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.client.MouseMovementHandler
import com.atsuishio.superbwarfare.client.molang.MolangVariable
import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import com.atsuishio.superbwarfare.compat.coldsweat.ColdSweatCompatHandler
import com.atsuishio.superbwarfare.compat.tacz.TACZGunEventHandler
import com.atsuishio.superbwarfare.config.CLIENT_CONFIG
import com.atsuishio.superbwarfare.config.COMMON_CONFIG
import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.network.NetworkRegistry
import com.atsuishio.superbwarfare.sound.SoundLimit
import net.minecraft.SharedConstants
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.AddPackFindersEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.registries.DataPackRegistryEvent
import net.minecraftforge.resource.PathPackResources
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private typealias Task = AbstractMap.SimpleEntry<Runnable, Int>

@Mod(com.atsuishio.superbwarfare.Mod.MODID)
class Mod {
    init {
        with(ModLoadingContext.get()) {
            registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG)
            registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG)
            registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG)
        }

        val bus = MOD_BUS

        ModPerks.register(bus)
        ModSerializers.REGISTRY.register(bus)
        ModSounds.REGISTRY.register(bus)
        ModBlocks.REGISTRY.register(bus)
        ModBlockEntities.REGISTRY.register(bus)
        ModItems.register(bus)
        ModEntities.REGISTRY.register(bus)
        ModTabs.TABS.register(bus)
        ModMobEffects.REGISTRY.register(bus)
        ModParticleTypes.REGISTRY.register(bus)
        ModPotions.register(bus)
        ModMenuTypes.REGISTRY.register(bus)
        ModVillagers.register(bus)
        ModRecipes.register(bus)
        ModCommandArguments.COMMAND_ARGUMENT_TYPES.register(bus)

        bus.addListener<FMLCommonSetupEvent> { onCommonSetup(it) }
        bus.addListener<FMLClientSetupEvent> { onClientSetup(it) }
        bus.addListener<FMLCommonSetupEvent> { ModItems.registerDispenserBehavior() }
        bus.addListener<AddPackFindersEvent> { onRegisterBuiltInResourcePacks(it) }
        bus.addListener<DataPackRegistryEvent.NewRegistry> { ModDatapackRegistries.onNewRegistry(it) }
        bus.addListener<RegisterShadersEvent> { ModParticleRenderTypes.onRegisterShaders(it) }

        if (TACZGunEventHandler.compatCondition()) {
            MinecraftForge.EVENT_BUS.addListener(TACZGunEventHandler::entityHurtByTACZGun)
        }
        if (ColdSweatCompatHandler.hasMod()) {
            MinecraftForge.EVENT_BUS.addListener(ColdSweatCompatHandler::onPlayerInVehicle)
        }

        MinecraftForge.EVENT_BUS.register(this)

        if (FMLEnvironment.dist == Dist.CLIENT) {
            SoundLimit.init()
        }

        CustomData.load()
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            executeWork(SERVER_QUEUE)
        }
    }

    @SubscribeEvent
    fun tick(event: ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            executeWork(CLIENT_QUEUE)
        }
    }

    private fun executeWork(workQueueC: MutableCollection<Task>) {
        workQueueC.removeAll(
            workQueueC
                .onEach { it.setValue(it.value - 1) }
                .filter { it.value <= 0 }
                .onEach { it.key.run() }
                .toSet()
        )
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        NetworkRegistry.register()
        MOD_BUS.post(RegisterContainersEvent())
        event.enqueueWork { ModGameRules.bootstrap() }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        MouseMovementHandler.init()
        MolangVariable.register()
        event.enqueueWork { ModScreens.register() }
        event.enqueueWork { ModSoundInstances.init() }
    }

    private fun onRegisterBuiltInResourcePacks(event: AddPackFindersEvent) {
        val info = Pack.Info(
            Component.literal("Superb Warfare - Legacy Textures"),
            SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA),
            SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
            FeatureFlagSet.of(),
            false
        )

        val path = ModList.get().getModFileById(MODID).file.findResource("resourcepacks/sbw_legacy")

        val pack = Pack.create(
            "${MODID}:resourcepacks/sbw_legacy",
            Component.translatable("pack.superbwarfare.sbw_legacy"),
            false,
            { PathPackResources(it, true, path) },
            info,
            PackType.CLIENT_RESOURCES,
            Pack.Position.TOP,
            false,
            PackSource.BUILT_IN
        )

        event.addRepositorySource { it.accept(pack) }
    }

    companion object {
        const val MODID = "superbwarfare"
        const val ATTRIBUTE_MODIFIER = "superbwarfare_attribute_modifier"

        @JvmField
        val LOGGER: Logger = LogManager.getLogger(com.atsuishio.superbwarfare.Mod::class.java)

        @JvmStatic
        fun loc(path: String): ResourceLocation = ResourceLocation(MODID, path)

        private val SERVER_QUEUE = ConcurrentLinkedQueue<Task>()
        private val CLIENT_QUEUE = ConcurrentLinkedQueue<Task>()

        @JvmStatic
        fun queueServerWork(tick: Int, action: Runnable) = SERVER_QUEUE.add(Task(action, tick))

        @JvmStatic
        fun queueClientWork(tick: Int, action: Runnable) = CLIENT_QUEUE.add(Task(action, tick))
    }
}
