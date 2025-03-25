package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot;
import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

//@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public abstract class EnergyMenu extends AbstractContainerMenu {

    private final List<ContainerEnergyDataSlot> containerEnergyDataSlots = Lists.newArrayList();
    private final List<ServerPlayer> usingPlayers = new ArrayList<>();

    public EnergyMenu(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public EnergyMenu(@Nullable MenuType<?> pMenuType, int id, ContainerEnergyData containerData) {
        super(pMenuType, id);

        for (int i = 0; i < containerData.getCount(); ++i) {
            this.containerEnergyDataSlots.add(ContainerEnergyDataSlot.forContainer(containerData, i));
        }
    }

    @Override
    public void broadcastChanges() {
        // TODO network
//        List<ContainerDataMessage.Pair> pairs = new ArrayList<>();
//        for (int i = 0; i < this.containerEnergyDataSlots.size(); ++i) {
//            ContainerEnergyDataSlot dataSlot = this.containerEnergyDataSlots.get(i);
//            if (dataSlot.checkAndClearUpdateFlag())
//                pairs.add(new ContainerDataMessage.Pair(i, dataSlot.get()));
//        }
//
//        if (!pairs.isEmpty()) {
//            PacketDistributor.PacketTarget target = PacketDistributor.NMLIST.with(this.usingPlayers.stream().map(serverPlayer -> serverPlayer.connection.connection)::toList);
//            ModUtils.PACKET_HANDLER.send(target, new ContainerDataMessage(this.containerId, pairs));
//        }

        super.broadcastChanges();
    }

    public void setData(int id, int data) {
        this.containerEnergyDataSlots.get(id).set(data);
    }

    public void setData(int id, long data) {
        this.containerEnergyDataSlots.get(id).set(data);
    }

//    @SubscribeEvent
//    public static void onContainerOpened(PlayerContainerEvent.Open event) {
//        if (event.getContainer() instanceof EnergyMenu menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
//            menu.usingPlayers.add(serverPlayer);
//
////            List<ContainerDataMessage.Pair> toSync = new ArrayList<>();
////            for (int i = 0; i < menu.containerEnergyDataSlots.size(); ++i) {
////                toSync.add(new ContainerDataMessage.Pair(i, menu.containerEnergyDataSlots.get(i).get()));
////            }
////            ModUtils.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ContainerDataMessage(menu.containerId, toSync));
//        }
//    }
//
//    @SubscribeEvent
//    public static void onContainerClosed(PlayerContainerEvent.Close event) {
//        if (event.getContainer() instanceof EnergyMenu menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
//            menu.usingPlayers.remove(serverPlayer);
//        }
//    }
}
