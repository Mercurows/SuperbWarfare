package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModMenuTypes;
import com.atsuishio.superbwarfare.item.PerkItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ReforgingTableMenu extends AbstractContainerMenu {

    protected final Container container;
    protected final ContainerLevelAccess access;

    public static final int INPUT_SLOT = 0;
    public static final int AMMO_PERK_SLOT = 1;
    public static final int FUNC_PERK_SLOT = 2;
    public static final int DAMAGE_PERK_SLOT = 3;
    public static final int RESULT_SLOT = 4;

    public static final int MAX_PERK_LEVEL = 20;
    public static final int MAX_UPGRADE_POINT = 100;

    public final DataSlot ammoPerkLevel = DataSlot.standalone();
    public final DataSlot funcPerkLevel = DataSlot.standalone();
    public final DataSlot damagePerkLevel = DataSlot.standalone();
    public final DataSlot upgradePoint = DataSlot.standalone();

    public static final int X_OFFSET = 0;
    public static final int Y_OFFSET = 11;

    public ReforgingTableMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(5), ContainerLevelAccess.NULL);
    }

    public ReforgingTableMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess access) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(5), access);
    }

    public ReforgingTableMenu(int pContainerId, Inventory inventory, Container container, ContainerLevelAccess pContainerLevelAccess) {
        super(ModMenuTypes.REFORGING_TABLE_MENU.get(), pContainerId);

        checkContainerSize(container, 5);

        this.container = container;
        this.access = pContainerLevelAccess;

        this.ammoPerkLevel.set(0);
        this.funcPerkLevel.set(0);
        this.damagePerkLevel.set(0);
        this.upgradePoint.set(0);

        this.addDataSlot(ammoPerkLevel);
        this.addDataSlot(funcPerkLevel);
        this.addDataSlot(damagePerkLevel);
        this.addDataSlot(upgradePoint);

        this.addSlot(new InputSlot(container, INPUT_SLOT, 20, 22));
        this.addSlot(new PerkSlot(container, AMMO_PERK_SLOT, Perk.Type.AMMO, 80, 25));
        this.addSlot(new PerkSlot(container, FUNC_PERK_SLOT, Perk.Type.FUNCTIONAL, 80, 45));
        this.addSlot(new PerkSlot(container, DAMAGE_PERK_SLOT, Perk.Type.DAMAGE, 80, 65));
        this.addSlot(new ResultSlot(container, RESULT_SLOT, 142, 45));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + i * 18 + Y_OFFSET));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (pIndex == INPUT_SLOT) {
                onTakeGun(stack);
                if (!this.moveItemStackTo(stack, RESULT_SLOT + 1, RESULT_SLOT + 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= AMMO_PERK_SLOT && pIndex <= DAMAGE_PERK_SLOT) {
                onTakePerk(stack);
                if (!this.moveItemStackTo(stack, RESULT_SLOT + 1, RESULT_SLOT + 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, RESULT_SLOT, RESULT_SLOT + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stack.getItem() instanceof GunItem) {
                    if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getItem() instanceof PerkItem<?> perkItem) {
                    Perk.Type type = perkItem.getPerk().type;
                    if (type == Perk.Type.AMMO) {
                        if (!this.moveItemStackTo(stack, AMMO_PERK_SLOT, AMMO_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (type == Perk.Type.FUNCTIONAL) {
                        if (!this.moveItemStackTo(stack, FUNC_PERK_SLOT, FUNC_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (type == Perk.Type.DAMAGE) {
                        if (!this.moveItemStackTo(stack, DAMAGE_PERK_SLOT, DAMAGE_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, stack);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.REFORGING_TABLE.get())
                && pPlayer.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D, true);
    }

    @Override
    public void removed(@NotNull Player pPlayer) {
        this.access.execute((level, pos) -> {
            ItemStack gun = this.container.getItem(INPUT_SLOT);
            ItemStack copy = gun.copy();

            for (int i = 0; i < this.container.getContainerSize(); ++i) {
                ItemStack itemstack = this.container.getItem(i);

                if (copy.getItem() instanceof GunItem
                        && itemstack.getItem() instanceof PerkItem<?> perkItem
                        && !copy.isEmpty()
                        && GunData.from(copy).perk.getLevel(perkItem) > 0
                ) continue;


                if (!itemstack.isEmpty()) {
                    pPlayer.getInventory().placeItemBackInInventory(itemstack);
                }

                this.container.removeItemNoUpdate(i);
            }
        });
    }

    public void setPerkLevel(Perk.Type type, boolean upgrade, boolean isCreative) {
        if (upgrade && this.upgradePoint.get() <= 0 && !isCreative) {
            return;
        }

        if (!upgrade && this.upgradePoint.get() >= MAX_UPGRADE_POINT && !isCreative) {
            return;
        }

        switch (type) {
            case AMMO ->
                    this.ammoPerkLevel.set(upgrade ? Math.min(MAX_PERK_LEVEL, this.ammoPerkLevel.get() + 1) : Math.max(1, this.ammoPerkLevel.get() - 1));
            case FUNCTIONAL ->
                    this.funcPerkLevel.set(upgrade ? Math.min(MAX_PERK_LEVEL, this.funcPerkLevel.get() + 1) : Math.max(1, this.funcPerkLevel.get() - 1));
            case DAMAGE ->
                    this.damagePerkLevel.set(upgrade ? Math.min(MAX_PERK_LEVEL, this.damagePerkLevel.get() + 1) : Math.max(1, this.damagePerkLevel.get() - 1));
        }

        if (!isCreative) {
            this.upgradePoint.set(Mth.clamp(this.upgradePoint.get() + (upgrade ? -1 : 1), 0, MAX_UPGRADE_POINT));
        }
    }

    public void handleUpgradePoint(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }
        var data = GunData.from(stack);

        double oldPoint = data.upgradePoint.get();
        int point = (int) oldPoint;
        int newPoint = this.upgradePoint.get();
        int delta = newPoint - point;

        if (delta != 0) {
            data.upgradePoint.set(oldPoint + delta);
            data.save();
        }
    }

    /**
     * 根据输入槽的枪械和Perk槽中的物品与等级，生成重铸后的武器，并放入输出槽中
     */
    public void generateResult() {
        ItemStack gun = this.container.getItem(INPUT_SLOT);
        if (!(gun.getItem() instanceof GunItem)) {
            return;
        }

        ItemStack ammo = this.container.getItem(AMMO_PERK_SLOT);
        ItemStack func = this.container.getItem(FUNC_PERK_SLOT);
        ItemStack damage = this.container.getItem(DAMAGE_PERK_SLOT);
        if (ammo.isEmpty() && func.isEmpty() && damage.isEmpty()) {
            return;
        }

        ItemStack result = gun.copy();
        var data = GunData.from(result);

        List.of(ammo, func, damage).forEach(item -> {
            if (!item.isEmpty()
                    && item.getItem() instanceof PerkItem<?> perkItem
                    && GunData.from(container.getItem(INPUT_SLOT)).canApplyPerk(perkItem.getPerk())
            ) {
                data.perk.set(new PerkInstance(perkItem.getPerk(), (short) switch (perkItem.getPerk().type) {
                    case AMMO -> this.ammoPerkLevel.get();
                    case FUNCTIONAL -> this.funcPerkLevel.get();
                    case DAMAGE -> this.damagePerkLevel.get();
                }));
                this.container.setItem(switch (perkItem.getPerk().type) {
                    case AMMO -> AMMO_PERK_SLOT;
                    case FUNCTIONAL -> FUNC_PERK_SLOT;
                    case DAMAGE -> DAMAGE_PERK_SLOT;
                }, ItemStack.EMPTY);
            }
        });

        data.save();
        handleUpgradePoint(result);

        this.ammoPerkLevel.set(0);
        this.funcPerkLevel.set(0);
        this.damagePerkLevel.set(0);
        this.upgradePoint.set(0);

        this.container.setItem(INPUT_SLOT, ItemStack.EMPTY);
        this.container.setItem(RESULT_SLOT, result);
        this.container.setChanged();
    }

    /**
     * 从Perk槽中取出对应的Perk物品时，根据其类型移除输入槽中枪械的Perk
     *
     * @param perk Perk物品
     */
    private void onTakePerk(ItemStack perk) {
        ItemStack gun = this.container.getItem(INPUT_SLOT);
        if (!(gun.getItem() instanceof GunItem)) {
            return;
        }

        if (perk.getItem() instanceof PerkItem<?> perkItem) {
            switch (perkItem.getPerk().type) {
                case AMMO -> this.ammoPerkLevel.set(0);
                case FUNCTIONAL -> this.funcPerkLevel.set(0);
                case DAMAGE -> this.damagePerkLevel.set(0);
            }

            var inputData = GunData.from(gun);
            int level = inputData.perk.getLevel(perkItem);

            if (level <= 0) {
                this.upgradePoint.set((int) inputData.upgradePoint.get());
                return;
            }

            ItemStack output = gun.copy();
            var outputData = GunData.from(output);
            outputData.perk.remove(perkItem.getPerk());

            inputData.upgradePoint.set(Math.min(MAX_UPGRADE_POINT, level - 1 + inputData.upgradePoint.get()));
            this.upgradePoint.set((int) inputData.upgradePoint.get());

            outputData.save();
            inputData.save();
            this.container.setItem(INPUT_SLOT, output);
            this.container.setChanged();
        }
    }

    /**
     * 放置perk物品时，将对应位置的level设置为1
     *
     * @param pStack Perk物品
     */
    private void onPlacePerk(ItemStack pStack) {
        if (!(pStack.getItem() instanceof PerkItem<?> perkItem)) {
            return;
        }

        switch (perkItem.getPerk().type) {
            case AMMO -> this.ammoPerkLevel.set(1);
            case FUNCTIONAL -> this.funcPerkLevel.set(1);
            case DAMAGE -> this.damagePerkLevel.set(1);
        }
    }

    /**
     * 将枪械放入输入槽中时，根据枪械上已有的Perk生成对应的Perk物品，并将等级调整为当前的等级
     *
     * @param stack 输入的枪械
     */
    private void onPlaceGun(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        int point = (int) data.upgradePoint.get();
        this.upgradePoint.set(Mth.clamp(point, 0, MAX_UPGRADE_POINT));

        for (var type : Perk.Type.values()) {
            var perkInstance = data.perk.getInstance(type);
            if (perkInstance != null) {
                switch (type) {
                    case AMMO -> this.ammoPerkLevel.set(perkInstance.level());
                    case FUNCTIONAL -> this.funcPerkLevel.set(perkInstance.level());
                    case DAMAGE -> this.damagePerkLevel.set(perkInstance.level());
                }

                var ammoPerkItem = perkInstance.perk().getItem().get();

                this.container.setItem(switch (type) {
                    case AMMO -> AMMO_PERK_SLOT;
                    case FUNCTIONAL -> FUNC_PERK_SLOT;
                    case DAMAGE -> DAMAGE_PERK_SLOT;
                }, ammoPerkItem.getDefaultInstance());
            }
        }

        this.container.setChanged();
        this.broadcastChanges();
    }

    /**
     * 拿走输入槽中的枪械时，如果Perk槽中存在放入枪械时生成的Perk物品，则将其移除，如果是没有的Perk则无视
     *
     * @param stack 输入的枪械
     */
    private void onTakeGun(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);

        for (var type : Perk.Type.values()) {
            var perk = data.perk.get(type);
            var slot = switch (type) {
                case AMMO -> AMMO_PERK_SLOT;
                case FUNCTIONAL -> FUNC_PERK_SLOT;
                case DAMAGE -> DAMAGE_PERK_SLOT;
            };

            if (perk != null &&
                    this.container.getItem(slot).getItem() instanceof PerkItem<?> perkItem
                    && perkItem.getPerk() == perk
            ) {
                this.container.setItem(slot, ItemStack.EMPTY);
            }
        }

        this.upgradePoint.set(0);
        this.ammoPerkLevel.set(0);
        this.funcPerkLevel.set(0);
        this.damagePerkLevel.set(0);

        var ammo = this.container.getItem(AMMO_PERK_SLOT);
        if (ammo != ItemStack.EMPTY) {
            this.moveItemStackTo(ammo, RESULT_SLOT + 1, RESULT_SLOT + 37, false);
        }

        var func = this.container.getItem(FUNC_PERK_SLOT);
        if (func != ItemStack.EMPTY) {
            this.moveItemStackTo(func, RESULT_SLOT + 1, RESULT_SLOT + 37, false);
        }

        var damage = this.container.getItem(DAMAGE_PERK_SLOT);
        if (damage != ItemStack.EMPTY) {
            this.moveItemStackTo(damage, RESULT_SLOT + 1, RESULT_SLOT + 37, false);
        }

        this.container.setChanged();
    }

    @Nullable
    public ItemStack getPerkItemBySlot(Perk.Type type) {
        return switch (type) {
            case AMMO -> this.container.getItem(AMMO_PERK_SLOT);
            case FUNCTIONAL -> this.container.getItem(FUNC_PERK_SLOT);
            case DAMAGE -> this.container.getItem(DAMAGE_PERK_SLOT);
        };
    }

    class InputSlot extends Slot {
        public InputSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        public boolean mayPlace(@NotNull ItemStack pStack) {
            if (pStack.getItem() instanceof GunItem) {
                ItemStack ammoPerk = this.container.getItem(AMMO_PERK_SLOT);
                ItemStack funcPerk = this.container.getItem(FUNC_PERK_SLOT);
                ItemStack damagePerk = this.container.getItem(DAMAGE_PERK_SLOT);

                boolean flag1 = ammoPerk.isEmpty();
                boolean flag2 = funcPerk.isEmpty();
                boolean flag3 = damagePerk.isEmpty();

                return flag1 && flag2 && flag3 && this.container.getItem(RESULT_SLOT).isEmpty() && this.container.getItem(INPUT_SLOT).isEmpty();
            }
            return false;
        }

        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(@NotNull Player pPlayer, @NotNull ItemStack pStack) {
            super.onTake(pPlayer, pStack);
            onTakeGun(pStack);
        }

        @Override
        public void setByPlayer(@NotNull ItemStack pStack) {
            onPlaceGun(pStack);
            super.setByPlayer(pStack);
        }
    }

    class PerkSlot extends Slot {
        public Perk.Type type;

        public PerkSlot(Container pContainer, int pSlot, Perk.Type type, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
            this.type = type;
        }

        public boolean mayPlace(@NotNull ItemStack pStack) {
            var slot = switch (type) {
                case AMMO -> AMMO_PERK_SLOT;
                case FUNCTIONAL -> FUNC_PERK_SLOT;
                case DAMAGE -> DAMAGE_PERK_SLOT;
            };

            return pStack.getItem() instanceof PerkItem<?> perkItem && perkItem.getPerk().type == type
                    && !container.getItem(INPUT_SLOT).isEmpty() && container.getItem(INPUT_SLOT).getItem() instanceof GunItem
                    && GunData.from(container.getItem(INPUT_SLOT)).canApplyPerk(perkItem.getPerk()) && container.getItem(slot).isEmpty();
        }

        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(@NotNull Player pPlayer, @NotNull ItemStack pStack) {
            onTakePerk(pStack);
            super.onTake(pPlayer, pStack);
        }

        @Override
        public void setByPlayer(@NotNull ItemStack pStack) {
            onPlacePerk(pStack);
            super.setByPlayer(pStack);
        }
    }

    static class ResultSlot extends Slot {
        public ResultSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        public boolean mayPlace(@NotNull ItemStack pStack) {
            return false;
        }

        public int getMaxStackSize() {
            return 1;
        }
    }
}
