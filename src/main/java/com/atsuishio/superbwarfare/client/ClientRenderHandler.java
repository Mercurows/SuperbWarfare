package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.decorator.ContainerItemDecorator;
import com.atsuishio.superbwarfare.client.decorator.LuckyContainerItemDecorator;
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel;
import com.atsuishio.superbwarfare.client.overlay.*;
import com.atsuishio.superbwarfare.client.renderer.block.*;
import com.atsuishio.superbwarfare.client.renderer.curio.ParachuteRenderer;
import com.atsuishio.superbwarfare.client.tooltip.*;
import com.atsuishio.superbwarfare.client.tooltip.component.*;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRenderHandler {

    @SubscribeEvent
    public static void registerTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(GunImageComponent.class, ClientGunImageTooltip::new);
        event.register(BocekImageComponent.class, ClientBocekImageTooltip::new);
        event.register(CellImageComponent.class, ClientCellImageTooltip::new);
        event.register(SentinelImageComponent.class, ClientSentinelImageTooltip::new);
        event.register(ChargingStationImageComponent.class, ClientChargingStationImageTooltip::new);
        event.register(DogTagImageComponent.class, ClientDogTagImageTooltip::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get(), context -> new ContainerBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get(), context -> new FuMO25BlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get(), context -> new ChargingStationBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get(), context -> new SmallContainerBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.LUCKY_CONTAINER.get(), context -> new LuckyContainerBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), context -> new VehicleAssemblingTableBlockEntityRenderer());
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll(KillMessageOverlay.ID, new KillMessageOverlay());
        event.registerBelow(Mod.loc(KillMessageOverlay.ID), ArmorPlateOverlay.ID, new ArmorPlateOverlay());
        event.registerBelow(Mod.loc(ArmorPlateOverlay.ID), AmmoBarOverlay.ID, new AmmoBarOverlay());
        event.registerBelow(Mod.loc(AmmoBarOverlay.ID), IFFOverlay.ID, new IFFOverlay());
        event.registerBelow(Mod.loc(IFFOverlay.ID), VehicleTeamOverlay.ID, new VehicleTeamOverlay());
        event.registerBelow(Mod.loc(VehicleTeamOverlay.ID), JavelinHudOverlay.ID, new JavelinHudOverlay());
        event.registerBelow(Mod.loc(JavelinHudOverlay.ID), IglaHudOverlay.ID, new IglaHudOverlay());
        event.registerBelow(Mod.loc(IglaHudOverlay.ID), AACalculatorOverlay.ID, new AACalculatorOverlay());
        event.registerBelow(Mod.loc(AACalculatorOverlay.ID), VehicleHudOverlay.ID, new VehicleHudOverlay());
        event.registerBelow(Mod.loc(VehicleHudOverlay.ID), VehicleMgHudOverlay.ID, new VehicleMgHudOverlay());
        event.registerBelowAll(StaminaOverlay.ID, new StaminaOverlay());
        event.registerBelowAll(Yx100SwarmDroneHudOverlay.ID, new Yx100SwarmDroneHudOverlay());
        event.registerBelowAll(AmmoCountOverlay.ID, new AmmoCountOverlay());
        event.registerBelowAll(ItemRendererFixOverlay.ID, new ItemRendererFixOverlay());
        event.registerBelowAll(CannonHudOverlay.ID, new CannonHudOverlay());
        event.registerBelowAll(CrossHairOverlay.ID, new CrossHairOverlay());
        event.registerBelowAll(HeatBarOverlay.ID, new HeatBarOverlay());
        event.registerBelowAll(DroneHudOverlay.ID, new DroneHudOverlay());
        event.registerBelowAll(RedTriangleOverlay.ID, new RedTriangleOverlay());
        event.registerBelowAll(HandsomeFrameOverlay.ID, new HandsomeFrameOverlay());
        event.registerBelowAll(SpyglassRangeOverlay.ID, new SpyglassRangeOverlay());
        event.registerBelowAll(TowOverlay.ID, new TowOverlay());
        event.registerBelowAll(HelicopterHudOverlay.ID, new HelicopterHudOverlay());
        event.registerBelowAll(AircraftOverlay.ID, new AircraftOverlay());
        event.registerBelowAll(MortarInfoOverlay.ID, new MortarInfoOverlay());
        event.registerBelowAll(Type63InfoOverlay.ID, new Type63InfoOverlay());
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.CONTAINER.get(), new ContainerItemDecorator());
        event.register(ModItems.LUCKY_CONTAINER.get(), new LuckyContainerItemDecorator());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CuriosRendererRegistry.register(ModItems.PARACHUTE.get(), ParachuteRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ParachuteModel.LAYER_LOCATION, ParachuteModel::createBodyLayer);
    }
}
