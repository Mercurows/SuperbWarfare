package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.client.overlay.AmmoBarOverlay;
import com.atsuishio.superbwarfare.client.overlay.AmmoCountOverlay;
import com.atsuishio.superbwarfare.client.overlay.ArmorPlateOverlay;
import com.atsuishio.superbwarfare.client.renderer.block.*;
import com.atsuishio.superbwarfare.client.tooltip.*;
import com.atsuishio.superbwarfare.client.tooltip.component.*;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRenderHandler {

    @SubscribeEvent
    public static void registerTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(GunImageComponent.class, ClientGunImageTooltip::new);
        event.register(ShotgunImageComponent.class, ClientShotgunImageTooltip::new);
        event.register(BocekImageComponent.class, ClientBocekImageTooltip::new);
        event.register(EnergyImageComponent.class, ClientEnergyImageTooltip::new);
        event.register(CellImageComponent.class, ClientCellImageTooltip::new);
        event.register(SentinelImageComponent.class, ClientSentinelImageTooltip::new);
        event.register(LauncherImageComponent.class, ClientLauncherImageTooltip::new);
        event.register(SecondaryCataclysmImageComponent.class, ClientSecondaryCataclysmImageTooltip::new);
        event.register(ChargingStationImageComponent.class, ClientChargingStationImageTooltip::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get(), context -> new ContainerBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get(), context -> new FuMO25BlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get(), context -> new ChargingStationBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.CREATIVE_CHARGING_STATION.get(), context -> new CreativeChargingStationBlockEntityRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get(), context -> new SmallContainerBlockEntityRenderer());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ArmorPlateOverlay.ID, new ArmorPlateOverlay());
        event.registerBelowAll(AmmoBarOverlay.ID, new AmmoBarOverlay());
        event.registerBelowAll(AmmoCountOverlay.ID, new AmmoCountOverlay());
    }
}