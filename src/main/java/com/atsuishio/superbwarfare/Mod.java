package com.atsuishio.superbwarfare;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.client.molang.MolangVariable;
import com.atsuishio.superbwarfare.client.sound.ModSoundInstances;
import com.atsuishio.superbwarfare.compat.tacz.TACZGunEventHandler;
import com.atsuishio.superbwarfare.config.ClientConfig;
import com.atsuishio.superbwarfare.config.CommonConfig;
import com.atsuishio.superbwarfare.config.ServerConfig;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.ModVariables;
import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@net.minecraftforge.fml.common.Mod(Mod.MODID)
public class Mod {

    public static final String MODID = "superbwarfare";
    public static final String ATTRIBUTE_MODIFIER = "superbwarfare_attribute_modifier";

    public static final Logger LOGGER = LogManager.getLogger(Mod.class);

    public Mod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.init());

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModPerks.register(bus);
        ModSerializers.REGISTRY.register(bus);
        ModSounds.REGISTRY.register(bus);
        ModBlocks.REGISTRY.register(bus);
        ModBlockEntities.REGISTRY.register(bus);
        ModItems.register(bus);
        ModEntities.REGISTRY.register(bus);
        ModTabs.TABS.register(bus);
        ModMobEffects.REGISTRY.register(bus);
        ModParticleTypes.REGISTRY.register(bus);
        ModPotion.POTIONS.register(bus);
        ModMenuTypes.REGISTRY.register(bus);
        ModVillagers.register(bus);
        ModRecipes.RECIPE_SERIALIZERS.register(bus);

        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);
        bus.addListener(ModItems::registerDispenserBehavior);

        registerDataTickets();

        if (TACZGunEventHandler.compatCondition()) {
            MinecraftForge.EVENT_BUS.addListener(TACZGunEventHandler::entityHurtByTACZGun);
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer, Optional<NetworkDirection> direction) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer, direction);
        messageID++;
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
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
            workQueue.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0)
                    actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueue.removeAll(actions);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
            workQueueC.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0)
                    actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueueC.removeAll(actions);
        }
    }

    public void onCommonSetup(final FMLCommonSetupEvent event) {
        addNetworkMessage(ZoomMessage.class, ZoomMessage::encode, ZoomMessage::decode, ZoomMessage::handler);
        addNetworkMessage(DoubleJumpMessage.class, DoubleJumpMessage::encode, DoubleJumpMessage::decode, DoubleJumpMessage::handler);
        addNetworkMessage(GunsDataMessage.class, GunsDataMessage::encode, GunsDataMessage::decode, (message, ctx1) -> GunsDataMessage.handler(message), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(FireKeyMessage.class, FireKeyMessage::encode, FireKeyMessage::decode, FireKeyMessage::handler);
        addNetworkMessage(VehicleFireMessage.class, VehicleFireMessage::encode, VehicleFireMessage::decode, VehicleFireMessage::handler);
        addNetworkMessage(FireModeMessage.class, FireModeMessage::encode, FireModeMessage::decode, FireModeMessage::handler);
        addNetworkMessage(ReloadMessage.class, ReloadMessage::encode, ReloadMessage::decode, ReloadMessage::handler);
        addNetworkMessage(PlayerGunKillMessage.class, PlayerGunKillMessage::encode, PlayerGunKillMessage::decode, PlayerGunKillMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(ClientIndicatorMessage.class, ClientIndicatorMessage::encode, ClientIndicatorMessage::decode, ClientIndicatorMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(SensitivityMessage.class, SensitivityMessage::encode, SensitivityMessage::decode, SensitivityMessage::handler);
        addNetworkMessage(AdjustZoomFovMessage.class, AdjustZoomFovMessage::encode, AdjustZoomFovMessage::decode, AdjustZoomFovMessage::handler);
        addNetworkMessage(AdjustMortarAngleMessage.class, AdjustMortarAngleMessage::encode, AdjustMortarAngleMessage::decode, AdjustMortarAngleMessage::handler);
        addNetworkMessage(InteractMessage.class, InteractMessage::encode, InteractMessage::decode, InteractMessage::handler);
        addNetworkMessage(VehicleMovementMessage.class, VehicleMovementMessage::encode, VehicleMovementMessage::decode, VehicleMovementMessage::handler);
        addNetworkMessage(DroneFireMessage.class, DroneFireMessage::encode, DroneFireMessage::decode, DroneFireMessage::handler);
        addNetworkMessage(SimulationDistanceMessage.class, SimulationDistanceMessage::encode, SimulationDistanceMessage::decode, SimulationDistanceMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(GunReforgeMessage.class, GunReforgeMessage::encode, GunReforgeMessage::decode, GunReforgeMessage::handler);
        addNetworkMessage(SetPerkLevelMessage.class, SetPerkLevelMessage::encode, SetPerkLevelMessage::decode, SetPerkLevelMessage::handler);
        addNetworkMessage(ModVariables.SavedDataSyncMessage.class, ModVariables.SavedDataSyncMessage::buffer, ModVariables.SavedDataSyncMessage::new, ModVariables.SavedDataSyncMessage::handler);
        addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
        addNetworkMessage(ShootMessage.class, ShootMessage::encode, ShootMessage::decode, ShootMessage::handler);
        addNetworkMessage(LaserShootMessage.class, LaserShootMessage::encode, LaserShootMessage::decode, LaserShootMessage::handler);
        addNetworkMessage(ShootClientMessage.class, ShootClientMessage::encode, ShootClientMessage::decode, ShootClientMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(ShakeClientMessage.class, ShakeClientMessage::encode, ShakeClientMessage::decode, ShakeClientMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(DrawClientMessage.class, DrawClientMessage::encode, DrawClientMessage::decode, (drawClientMessage, context) -> DrawClientMessage.handle(context), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(EditMessage.class, EditMessage::encode, EditMessage::decode, EditMessage::handler);
        addNetworkMessage(SwitchScopeMessage.class, SwitchScopeMessage::encode, SwitchScopeMessage::decode, SwitchScopeMessage::handler);
        addNetworkMessage(SetFiringParametersMessage.class, SetFiringParametersMessage::encode, SetFiringParametersMessage::decode, SetFiringParametersMessage::handler);
        addNetworkMessage(ContainerDataMessage.class, ContainerDataMessage::encode, ContainerDataMessage::decode, ContainerDataMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(RadarChangeModeMessage.class, RadarChangeModeMessage::encode, RadarChangeModeMessage::decode, RadarChangeModeMessage::handler);
        addNetworkMessage(RadarSetParametersMessage.class, RadarSetParametersMessage::encode, RadarSetParametersMessage::decode, RadarSetParametersMessage::handler);
        addNetworkMessage(LungeMineAttackMessage.class, LungeMineAttackMessage::encode, LungeMineAttackMessage::decode, LungeMineAttackMessage::handler);
        addNetworkMessage(RadarMenuOpenMessage.class, RadarMenuOpenMessage::encode, RadarMenuOpenMessage::decode, RadarMenuOpenMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(RadarMenuCloseMessage.class, RadarMenuCloseMessage::encode, RadarMenuCloseMessage::decode, RadarMenuCloseMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(RadarSetPosMessage.class, RadarSetPosMessage::encode, RadarSetPosMessage::decode, RadarSetPosMessage::handler);
        addNetworkMessage(PlayerStopRidingMessage.class, PlayerStopRidingMessage::encode, PlayerStopRidingMessage::decode, PlayerStopRidingMessage::handler);
        addNetworkMessage(AimVillagerMessage.class, AimVillagerMessage::encode, AimVillagerMessage::decode, AimVillagerMessage::handler);
        addNetworkMessage(ShowChargingRangeMessage.class, ShowChargingRangeMessage::encode, ShowChargingRangeMessage::decode, ShowChargingRangeMessage::handler);
        addNetworkMessage(MeleeAttackMessage.class, MeleeAttackMessage::encode, MeleeAttackMessage::decode, MeleeAttackMessage::handler);
        addNetworkMessage(ResetCameraTypeMessage.class, ResetCameraTypeMessage::encode, ResetCameraTypeMessage::decode, ResetCameraTypeMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(SwitchVehicleWeaponMessage.class, SwitchVehicleWeaponMessage::encode, SwitchVehicleWeaponMessage::decode, SwitchVehicleWeaponMessage::handler);
        addNetworkMessage(RadarSetTargetMessage.class, RadarSetTargetMessage::encode, RadarSetTargetMessage::decode, RadarSetTargetMessage::handler);
        addNetworkMessage(ChangeVehicleSeatMessage.class, ChangeVehicleSeatMessage::encode, ChangeVehicleSeatMessage::decode, ChangeVehicleSeatMessage::handler);
        addNetworkMessage(ClientMotionSyncMessage.class, ClientMotionSyncMessage::encode, ClientMotionSyncMessage::decode, ClientMotionSyncMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(TacticalSprintMessage.class, TacticalSprintMessage::encode, TacticalSprintMessage::decode, TacticalSprintMessage::handler);
        addNetworkMessage(ClientTacticalSprintSyncMessage.class, ClientTacticalSprintSyncMessage::encode, ClientTacticalSprintSyncMessage::decode, ClientTacticalSprintSyncMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(DogTagEditorMessage.class, DogTagEditorMessage::encode, DogTagEditorMessage::decode, DogTagEditorMessage::handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(DogTagFinishEditMessage.class, DogTagFinishEditMessage::encode, DogTagFinishEditMessage::decode, DogTagFinishEditMessage::handler);
        addNetworkMessage(VehiclesDataMessage.class, VehiclesDataMessage::encode, VehiclesDataMessage::decode, (msg, ctx) -> VehiclesDataMessage.handler(msg), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        addNetworkMessage(MouseMoveMessage.class, MouseMoveMessage::encode, MouseMoveMessage::decode, MouseMoveMessage::handler);

        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)),
                Ingredient.of(Items.LIGHTNING_ROD), PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotion.SHOCK.get())));
        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotion.SHOCK.get())),
                Ingredient.of(Items.REDSTONE), PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotion.LONG_SHOCK.get())));
        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotion.SHOCK.get())),
                Ingredient.of(Items.GLOWSTONE_DUST), PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotion.STRONG_SHOCK.get())));

        var registerContainerEvent = new RegisterContainersEvent();
        FMLJavaModLoadingContext.get().getModEventBus().post(registerContainerEvent);
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
