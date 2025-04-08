package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot;
import com.atsuishio.superbwarfare.network.message.receive.ContainerDataMessage;
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuCloseMessage;
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuOpenMessage;
import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public abstract class EnergyMenu extends AbstractContainerMenu {

    private final List<ContainerEnergyDataSlot> containerEnergyDataSlots = Lists.newArrayList();
    private final List<ServerPlayer> usingPlayers = new ArrayList<>();

    public EnergyMenu(@Nullable MenuType<?> pMenuType, int id, ContainerEnergyData containerData) {
        super(pMenuType, id);

        for (int i = 0; i < containerData.getCount(); ++i) {
            addDataSlot(DataSlot.standalone());
            this.containerEnergyDataSlots.add(ContainerEnergyDataSlot.forContainer(containerData, i));
        }
    }

    @Override
    public void broadcastChanges() {
        List<ContainerDataMessage.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < this.containerEnergyDataSlots.size(); ++i) {
            ContainerEnergyDataSlot dataSlot = this.containerEnergyDataSlots.get(i);
            if (dataSlot.checkAndClearUpdateFlag())
                pairs.add(new ContainerDataMessage.Pair(i, dataSlot.get()));
        }

        if (!pairs.isEmpty()) {
            this.usingPlayers.forEach(p -> PacketDistributor.sendToPlayer(p, new ContainerDataMessage(this.containerId, pairs)));
        }

        super.broadcastChanges();
    }

    public void setData(int id, int data) {
        super.setData(id, data);
        this.containerEnergyDataSlots.get(id).set(data);
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof EnergyMenu menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
            menu.usingPlayers.add(serverPlayer);

            List<ContainerDataMessage.Pair> toSync = new ArrayList<>();
            for (int i = 0; i < menu.containerEnergyDataSlots.size(); ++i) {
                toSync.add(new ContainerDataMessage.Pair(i, menu.containerEnergyDataSlots.get(i).get()));
            }
            PacketDistributor.sendToPlayer(serverPlayer, new ContainerDataMessage(menu.containerId, toSync));
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof EnergyMenu menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
            menu.usingPlayers.remove(serverPlayer);
        }
    }


    @SubscribeEvent
    public static void onFuMO25Opened(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof FuMO25Menu fuMO25Menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
            fuMO25Menu.getSelfPos().ifPresent(pos -> PacketDistributor.sendToPlayer(serverPlayer, new RadarMenuOpenMessage(pos)));
        }
    }

    @SubscribeEvent
    public static void onFuMO25Closed(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof FuMO25Menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new RadarMenuCloseMessage(0));
        }
    }
}
