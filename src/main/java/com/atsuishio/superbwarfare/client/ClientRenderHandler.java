package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.*;
import com.atsuishio.superbwarfare.client.renderer.block.ChargingStationBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.ContainerBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.FuMO25BlockEntityRenderer;
import com.atsuishio.superbwarfare.client.renderer.block.SmallContainerBlockEntityRenderer;
import com.atsuishio.superbwarfare.client.tooltip.*;
import com.atsuishio.superbwarfare.client.tooltip.component.*;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll(KillMessageOverlay.ID, new KillMessageOverlay());
        event.registerBelow(Mod.loc(KillMessageOverlay.ID), JavelinHudOverlay.ID, new JavelinHudOverlay());
        event.registerBelow(Mod.loc(JavelinHudOverlay.ID), ArmorPlateOverlay.ID, new ArmorPlateOverlay());
        event.registerBelow(Mod.loc(ArmorPlateOverlay.ID), VehicleHudOverlay.ID, new VehicleHudOverlay());
        event.registerBelow(Mod.loc(VehicleHudOverlay.ID), VehicleMgHudOverlay.ID, new VehicleMgHudOverlay());
        event.registerBelowAll(StaminaOverlay.ID, new StaminaOverlay());
        event.registerBelowAll(VehicleTeamOverlay.ID, new VehicleTeamOverlay());
        event.registerBelowAll(Yx100SwarmDroneHudOverlay.ID, new Yx100SwarmDroneHudOverlay());
        event.registerBelowAll(AmmoBarOverlay.ID, new AmmoBarOverlay());
        event.registerBelowAll(AmmoCountOverlay.ID, new AmmoCountOverlay());
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
}
