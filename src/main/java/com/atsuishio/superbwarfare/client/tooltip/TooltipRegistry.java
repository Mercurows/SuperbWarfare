package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TooltipRegistry {

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
        // TODO block entity renderer
//        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get(), context -> new ContainerBlockEntityRenderer());
//        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get(), context -> new FuMO25BlockEntityRenderer());
//        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get(), context -> new ChargingStationBlockEntityRenderer());
    }
}
