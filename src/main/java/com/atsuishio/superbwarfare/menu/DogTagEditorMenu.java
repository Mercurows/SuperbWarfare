package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMenuTypes;
import com.atsuishio.superbwarfare.network.message.receive.DogTagEditorMessage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class DogTagEditorMenu extends AbstractContainerMenu {

    protected final Container container;
    protected final ContainerLevelAccess access;
    @Nullable
    private String itemName;

    public ItemStack stack;

    public DogTagEditorMenu(int pContainerId) {
        this(pContainerId, new SimpleContainer(0), ContainerLevelAccess.NULL, ItemStack.EMPTY);
    }

    public DogTagEditorMenu(int pContainerId, ContainerLevelAccess access, ItemStack stack) {
        this(pContainerId, new SimpleContainer(0), access, stack);
    }

    public DogTagEditorMenu(int pContainerId, Container container, ContainerLevelAccess pContainerLevelAccess, ItemStack stack) {
        super(ModMenuTypes.DOG_TAG_EDITOR_MENU.get(), pContainerId);

        checkContainerSize(container, 0);

        this.container = container;
        this.access = pContainerLevelAccess;
        this.stack = stack;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.isAlive();
    }

    public boolean setItemName(String name) {
        String s = validateName(name);
        if (s != null && !s.equals(this.itemName)) {
            this.itemName = s;
            if (!this.stack.isEmpty()) {
                if (StringUtil.isBlank(s)) {
                    this.stack.remove(DataComponents.CUSTOM_NAME);
                } else {
                    this.stack.set(DataComponents.CUSTOM_NAME, Component.literal(s));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    private static String validateName(String pItemName) {
        String s = StringUtil.filterText(pItemName);
        return s.length() <= 30 ? s : null;
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof DogTagEditorMenu menu && event.getEntity() instanceof ServerPlayer serverPlayer) {
            var stack = serverPlayer.getItemInHand(serverPlayer.getUsedItemHand());
            if (stack.is(ModItems.DOG_TAG.get())) {
                PacketDistributor.sendToPlayer(serverPlayer, new DogTagEditorMessage(menu.containerId, stack));
            }
        }
    }

    public void finishEdit(List<Short> colors, String name) {
        if (this.stack.isEmpty()) return;

        stack.set(ModDataComponents.DOG_TAG_IMAGE, colors);

        if (!name.isEmpty()) {
            this.stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
    }
}
