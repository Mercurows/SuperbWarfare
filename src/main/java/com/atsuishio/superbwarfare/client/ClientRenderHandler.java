package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.client.overlay.*;
import com.atsuishio.superbwarfare.client.renderer.block.ChargingStationBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.ContainerBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.FuMO25BlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.SmallContainerBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.tooltip.*;
import com.atsuishio.superbwarfare.client.tooltip.component.*;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRenderHandler {

    @SubscribeEvent
    public static void registerTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(GunImageComponent.class, ClientGunImageTooltip::new);
        event.register(BocekImageComponent.class, ClientBocekImageTooltip::new);
        event.register(EnergyImageComponent.class, ClientEnergyImageTooltip::new);
        event.register(CellImageComponent.class, ClientCellImageTooltip::new);
        event.register(SentinelImageComponent.class, ClientSentinelImageTooltip::new);
        event.register(LauncherImageComponent.class, ClientLauncherImageTooltip::new);
        event.register(SecondaryCataclysmImageComponent.class, ClientSecondaryCataclysmImageTooltip::new);
        event.register(ChargingStationImageComponent.class, ClientChargingStationImageTooltip::new);
        event.register(DogTagImageComponent.class, ClientDogTagImageTooltip::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get(), context -> new ContainerBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get(), context -> new FuMO25BlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get(), context -> new ChargingStationBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get(), context -> new SmallContainerBlockEntityRenderer());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerBelowAll(KillMessageOverlay.ID, new KillMessageOverlay());
        event.registerBelow(KillMessageOverlay.ID, JavelinHudOverlay.ID, new JavelinHudOverlay());
        event.registerBelow(JavelinHudOverlay.ID, ArmorPlateOverlay.ID, new ArmorPlateOverlay());
        event.registerBelow(ArmorPlateOverlay.ID, VehicleHudOverlay.ID, new VehicleHudOverlay());
        event.registerBelow(VehicleHudOverlay.ID, VehicleMgHudOverlay.ID, new VehicleMgHudOverlay());
        event.registerBelowAll(StaminaOverlay.ID, new StaminaOverlay());
        event.registerBelowAll(VehicleTeamOverlay.ID, new VehicleTeamOverlay());
        event.registerBelowAll(Yx100SwarmDroneHudOverlay.ID, new Yx100SwarmDroneHudOverlay());
        event.registerBelowAll(AmmoBarOverlay.ID, new AmmoBarOverlay());
        event.registerBelowAll(AmmoCountOverlay.ID, new AmmoCountOverlay());
        event.registerBelowAll(ItemRendererFixOverlay.ID, new ItemRendererFixOverlay());
        event.registerBelowAll(CannonHudOverlay.ID, new CannonHudOverlay());
        event.registerBelowAll(CrossHairOverlay.ID, new CrossHairOverlay());
        event.registerBelowAll(DroneHudOverlay.ID, new DroneHudOverlay());
        event.registerBelowAll(GrenadeLauncherOverlay.ID, new GrenadeLauncherOverlay());
        event.registerBelowAll(RedTriangleOverlay.ID, new RedTriangleOverlay());
        event.registerBelowAll(HandsomeFrameOverlay.ID, new HandsomeFrameOverlay());
        event.registerBelowAll(SpyglassRangeOverlay.ID, new SpyglassRangeOverlay());
        event.registerBelowAll(HelicopterHudOverlay.ID, new HelicopterHudOverlay());
        event.registerBelowAll(AircraftOverlay.ID, new AircraftOverlay());
        event.registerBelowAll(MortarInfoOverlay.ID, new MortarInfoOverlay());
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.CONTAINER.get(), new ContainerItemDecorator());
    }
}
