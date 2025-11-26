package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.atsuishio.superbwarfare.data.gun.subdata.*;
import com.atsuishio.superbwarfare.data.gun.value.*;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class GunData implements DefaultDataSupplier<DefaultGunData> {

    public static final LoadingCache<ItemStack, GunData> DATA_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(new CacheLoader<>() {
                public @NotNull GunData load(@NotNull ItemStack stack) {
                    return new GunData(stack);
                }
            });

    public final ItemStack stack;
    public final GunItem item;
    public final CompoundTag tag;
    public final CompoundTag gunDataTag;
    public final CompoundTag perkTag;
    public final CompoundTag attachmentTag;
    public final StringValue propertyOverrideString;
    public final String id;

    @NotNull
    public Supplier<DefaultGunData> defaultDataSupplier;

    private GunData(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        this.id = getRegistryId(stack.getItem());

        this.defaultDataSupplier = () -> gunItem.getDefaultData(this);

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        this.tag = customData != null ? customData.copyTag() : new CompoundTag();

        gunDataTag = getOrPut("GunData");
        perkTag = getOrPut("Perks");
        attachmentTag = getOrPut("Attachments");
        propertyOverrideString = new StringValue(this.gunDataTag, "Override");

        selectedAmmoType = new IntValue(gunDataTag, "SelectedAmmoType");
        selectedFireMode = new IntValue(gunDataTag, "SelectedFireMode", 0);
        fireIndex = new IntValue(gunDataTag, "FireIndex", 0);

        // 可持久化属性
        reload = new Reload(this);
        charge = new Charge(this);
        bolt = new Bolt(this);
        attachment = new Attachment(this);
        perk = new Perks(this);

        ammo = new IntValue(gunDataTag, "Ammo");
        virtualAmmo = new IntValue(gunDataTag, "VirtualAmmo");
        backupAmmoCount = new IntValue(gunDataTag, "BackupAmmoCount");
        ammoSlot = new AmmoSlot(gunDataTag);
        burstAmount = new IntValue(gunDataTag, "BurstAmount");

        level = new IntValue(gunDataTag, "Level");
        exp = new DoubleValue(gunDataTag, "Exp");

        isEmpty = new BooleanValue(gunDataTag, "IsEmpty");
        closeHammer = new BooleanValue(gunDataTag, "CloseHammer");
        closeStrike = new BooleanValue(gunDataTag, "CloseStrike");
        stopped = new BooleanValue(gunDataTag, "Stopped");
        forceStop = new BooleanValue(gunDataTag, "ForceStop");
        loadIndex = new IntValue(gunDataTag, "LoadIndex");
        holdOpen = new BooleanValue(gunDataTag, "HoldOpen");
        hideBulletChain = new BooleanValue(gunDataTag, "HideBulletChain");
        sensitivity = new IntValue(gunDataTag, "Sensitivity");
        heat = new DoubleValue(gunDataTag, "Heat");
        shootAnimationTimer = new IntValue(gunDataTag, "ShootAnimationTimer");
        overHeat = new BooleanValue(gunDataTag, "OverHeat");
        zooming = new BooleanValue(gunDataTag, "Zooming");

        var defaultFireMode = compute(false).defaultFireMode;
        if (defaultFireMode == null) {
            defaultFireMode = FireMode.SEMI.name;
        }

        var fireModes = compute(false).availableFireModes();
        for (int i = 0; i < fireModes.size(); i++) {
            if (fireModes.get(i).name.equals(defaultFireMode)) {
                selectedFireMode.defaultValue = i;
                break;
            }
        }
    }

    private CompoundTag getOrPut(String name) {
        if (!this.tag.contains(name)) {
            this.tag.put(name, new CompoundTag());
        }
        return this.tag.getCompound(name);
    }

    public boolean initialized() {
        return item.isInitialized(this);
    }

    public void initialize() {
        item.init(this);
    }

    public static GunData create(Item item) {
        return from(new ItemStack(item));
    }

    public static GunData from(ItemStack stack) {
        return DATA_CACHE.getUnchecked(stack);
    }

    public GunItem item() {
        return item;
    }

    public ItemStack stack() {
        return stack;
    }

    public CompoundTag tag() {
        return tag;
    }

    public CompoundTag data() {
        return gunDataTag;
    }

    public CompoundTag perk() {
        return perkTag;
    }

    public CompoundTag attachment() {
        return attachmentTag;
    }

    public static DefaultGunData getDefault(String id) {
        var isDefault = !com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.containsKey(id);
        var data = com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.getOrElseGet(id, DefaultGunData::new);
        data.isDefaultData = isDefault;
        return data;
    }

    @Override
    public DefaultGunData getDefault() {
        return this.defaultDataSupplier.get();
    }

    public static DefaultGunData getDefault(ItemStack stack) {
        return getDefault(stack.getItem());
    }

    public static DefaultGunData getDefault(Item item) {
        return getDefault(getRegistryId(item));
    }

    public static String getRegistryId(Item item) {
        var id = item.getDescriptionId();
        id = id.substring(id.indexOf(".") + 1).replace('.', ':');
        return id;
    }

    public void clearTempModifications() {
        tempModifications = null;
    }

    private final JsonPropertyModifier<GunData, DefaultGunData> jsonPropModifier = new JsonPropertyModifier<>();

    private DefaultGunData cache = null;

    public static DefaultGunData compute(ItemStack stack) {
        return GunData.from(stack).compute();
    }

    public Function<DefaultGunData, DefaultGunData> tempModifications = null;

    public DefaultGunData compute() {
        return compute(true);
    }

    public DefaultGunData compute(boolean useCache) {
        if (cache != null && useCache) return cache;

        var rawData = getDefault().copy();

        // property override tag
        jsonPropModifier.update(propertyOverrideString.get());
        rawData = jsonPropModifier.computeProperties(this, rawData);

        // gun modifiers
        rawData = item.computeProperties(this, rawData);

        // FireMode
        rawData = selectedFireModeInfo(rawData.availableFireModes()).computeProperties(this, rawData);

        // AmmoConsumer
        rawData = selectedAmmoConsumer(rawData.getAmmoConsumers()).computeProperties(this, rawData);

        // perk
        if (perk != null) {
            for (var type : Perk.Type.values()) {
                var instance = perk.get(type);
                if (instance == null) continue;

                rawData = instance.computeProperties(this, rawData);
            }
        }

        // 临时属性修改
        if (tempModifications != null) {
            rawData = tempModifications.apply(rawData);
        }

        rawData.limit();
        if (useCache) {
            cache = rawData;
        }

        return rawData;
    }

    public void update() {
        this.cache = null;
    }

    /**
     * use compute() instead
     */
    @SuppressWarnings("removal")
    @Deprecated(forRemoval = true)
    public <T> T get(GunProp<T> prop) {
        return prop.asModifier(this).compute(compute());
    }

    public boolean hasInfiniteBackupAmmo(@Nullable Entity shooter) {
        return shooter instanceof Player player && player.isCreative()
                || selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.INFINITE
                || meleeOnly()
                || InventoryTool.hasCreativeAmmoBox(shooter);
    }

    /**
     * 武器是否直接使用背包内弹药
     */
    public boolean useBackpackAmmo() {
        return compute().magazine <= 0;
    }

    // TODO 这什么b scope判断
    public double minZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? Math.max(getDefault().minZoom, 1.25) : 1.25;
    }

    // TODO 这什么b scope判断
    public double maxZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? getDefault().maxZoom : 114514;
    }

    public double zoom() {
        if (minZoom() >= maxZoom()) return compute().defaultZoom;
        return Mth.clamp(compute().defaultZoom, minZoom(), maxZoom());
    }

    public AmmoConsumer selectedAmmoConsumer(List<AmmoConsumer> consumers) {
        if (consumers == null || consumers.isEmpty()) {
            return AmmoConsumer.INVALID;
        }
        return consumers.get(Mth.clamp(this.selectedAmmoType.get(), 0, consumers.size() - 1));
    }

    public AmmoConsumer selectedAmmoConsumer() {
        return selectedAmmoConsumer(compute().getAmmoConsumers());
    }

    public void changeAmmoConsumer(int index, @Nullable Entity ammoSupplier) {
        var consumers = this.compute().getAmmoConsumers();
        var targetIndex = Mth.clamp(index, 0, consumers.size() - 1);
        if (targetIndex == selectedAmmoType.get()) return;

        if (!(ammoSupplier instanceof Player player && player.isCreative())) {
            var currentConsumer = selectedAmmoConsumer();
            var targetConsumer = consumers.get(selectedAmmoType.get());

            var currentSlot = currentConsumer.ammoSlot;
            var targetSlot = targetConsumer.ammoSlot;

            if (currentSlot == null) currentSlot = "Default";
            if (targetSlot == null) targetSlot = "Default";

            if (currentSlot.equals(targetSlot) && ammoSupplier != null && targetConsumer.shouldUnload) {
                this.withdrawAmmo(ammoSupplier);
            } else {
                var ammo = this.ammo.get();
                var virtualAmmo = this.virtualAmmo.get();
                this.ammoSlot.set(currentSlot, ammo, virtualAmmo);

                this.ammo.set(this.ammoSlot.getAmmo(targetSlot));
                this.virtualAmmo.set(this.ammoSlot.getVirtualAmmo(targetSlot));
                this.ammoSlot.reset(targetSlot);
            }
        }

        this.selectedAmmoType.set(targetIndex);

        if (ammoSupplier instanceof Player player && player.isCreative()) {
            this.ammo.set(this.compute().magazine);
        }

        this.item.whenNoAmmo(this);
        this.closeHammer.set(false);
        this.fireIndex.reset();

        resetStatus();
    }

    public void resetStatus() {
        this.reload.stage.reset();
        this.reload.setState(ReloadState.NOT_RELOADING);
        this.reload.iterativeLoadTimer.reset();
        this.reload.reloadTimer.reset();
        this.reload.finishTimer.reset();
        this.reload.prepareTimer.reset();
        this.reload.prepareLoadTimer.reset();
        this.reload.reloadStarter.finish();
        this.reload.singleReloadStarter.finish();
        this.reload.singleReloadStarter.finish();
        this.bolt.actionTimer.reset();
        this.bolt.needed.reset();
        this.charge.starter.finish();
        this.charge.timer.reset();
    }

    public FireModeInfo selectedFireModeInfo(List<FireModeInfo> fireModes) {
        if (fireModes == null || fireModes.isEmpty()) {
            return new FireModeInfo();
        }
        return fireModes.get(Mth.clamp(this.selectedFireMode.get(), 0, fireModes.size() - 1));
    }

    public FireModeInfo selectedFireModeInfo() {
        return selectedFireModeInfo(compute().availableFireModes());
    }

    // 开火相关流程开始

    /*
     * 开火相关流程描述
     * 1. 调用shouldStartReloading和shouldStartBolt查看当前状态是否应该开始换弹或拉栓，是则调用startReloading或startBolt开始换弹/拉栓流程
     * 2. 调用canShoot(@Nullable Entity shooter)查看当前状态是否能够开火，如果能够开火则调用shootBullet进行开火
     * 3. 调用tick(@Nullable Entity shooter)执行枪械tick任务，包括换弹流程、过热计算、拉栓等
     *
     * 可选项：
     * 1. 使用GunData.virtualAmmo.set来设置虚拟弹药数量
     * 2. 传入带有IItemHandler能力的任意Entity来提供额外弹药
     *
     */

    /**
     * 是否应该开始换弹
     */
    public boolean shouldStartReloading(@Nullable Entity entity) {
        return !reloading()
                && !useBackpackAmmo()
                && !hasEnoughAmmoToShoot(entity)
                && hasBackupAmmo(entity);
    }

    /**
     * 是否应该开始换弹
     */
    public boolean shouldStartBolt() {
        return this.bolt.actionTimer.get() == 0 && this.bolt.needed.get();
    }

    /**
     * 开始换弹流程，换弹将在tick内被执行
     */
    public void startReload() {
        this.reload.reloadStarter.markStart();
    }

    /**
     * 开始拉栓流程，换弹将在tick内被执行
     */
    public void startBolt() {
        this.bolt.actionTimer.set(this.compute().boltActionTime + 1);
    }

    /**
     * 是否还有剩余弹药（不考虑枪内弹药）
     */
    public boolean hasBackupAmmo(@Nullable Entity entity) {
        return countBackupAmmo(entity) > 0;
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    public int countBackupAmmo(@Nullable Entity entity) {
        if (entity == null) return virtualAmmo.get();
        if (entity instanceof Player player && player.isCreative() || InventoryTool.hasCreativeAmmoBox(entity))
            return Integer.MAX_VALUE;

        return Math.toIntExact(Mth.clamp((long) countBackupAmmoItem(entity) * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(), 0, Integer.MAX_VALUE));
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    public int countBackupAmmo(@Nullable IItemHandler handler) {
        if (handler == null) return virtualAmmo.get();
        if (InventoryTool.hasCreativeAmmoBox(handler)) return Integer.MAX_VALUE;

        return Math.toIntExact(Mth.clamp((long) countBackupAmmoItem(handler) * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(), 0, Integer.MAX_VALUE));
    }

    public int countBackupAmmoItem(@Nullable Entity entity) {
        return this.selectedAmmoConsumer().count(this, entity);
    }

    public int countBackupAmmoItem(@Nullable IItemHandler handler) {
        return this.selectedAmmoConsumer().count(this, handler);
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    public void consumeBackupAmmo(@Nullable Entity entity, int count) {
        if (count <= 0 || entity instanceof Player player && player.isCreative() || InventoryTool.hasCreativeAmmoBox(entity))
            return;

        if (virtualAmmo.get() > 0) {
            var consumed = Math.min(virtualAmmo.get(), count);
            virtualAmmo.add(-consumed);
            count -= consumed;
            save();
        }
        if (count <= 0 || entity == null) return;

        var consumer = this.selectedAmmoConsumer();
        var loadAmount = consumer.loadAmount;
        if (count % loadAmount != 0) {
            var required = (count / loadAmount) + 1;
            var consumed = consumer.consume(this, entity, required);
            count -= consumed * loadAmount;

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count);
            }
        } else {
            consumer.consume(this, entity, count / loadAmount);
        }
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    public void consumeBackupAmmo(@Nullable IItemHandler handler, int count) {
        if (count <= 0 || InventoryTool.hasCreativeAmmoBox(handler)) return;

        if (virtualAmmo.get() > 0) {
            var consumed = Math.min(virtualAmmo.get(), count);
            virtualAmmo.add(-consumed);
            count -= consumed;
            save();
        }
        if (count <= 0 || handler == null) return;

        var consumer = selectedAmmoConsumer();
        var loadAmount = consumer.loadAmount;

        if (count % loadAmount != 0) {
            var required = (count / loadAmount) + 1;
            var consumed = consumer.consume(this, handler, required);
            count -= consumed * loadAmount;

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count);
            }
        } else {
            consumer.consume(this, handler, count / loadAmount);
        }
    }

    /**
     * 当前状态在换弹前的可用射击次数
     */
    public int currentAvailableShots(@Nullable Entity entity) {
        var ammoCost = compute().ammoCostPerShoot;
        if (ammoCost <= 0) return Integer.MAX_VALUE;

        return currentAvailableAmmo(entity) / ammoCost;
    }

    /**
     * 当前枪内可用弹药数量
     */
    public int currentAvailableAmmo(@Nullable Entity entity) {
        return useBackpackAmmo() ? countBackupAmmo(entity) : this.ammo.get();
    }

    /**
     * 当前状态枪内是否拥有足够的弹药进行开火
     */
    public boolean hasEnoughAmmoToShoot(@Nullable Entity entity) {
        return compute().ammoCostPerShoot <= currentAvailableAmmo(entity);
    }

    /**
     * 换弹完成后装填弹药，在换弹流程完成后调用
     */
    public void reloadAmmo(@Nullable Entity entity) {
        reloadAmmo(entity, false);
    }

    /**
     * 换弹完成后装填弹药，在换弹流程完成后调用
     */
    public void reloadAmmo(@Nullable Entity entity, boolean extraOne) {
        if (useBackpackAmmo()) return;

        int mag = compute().magazine;
        int ammo = this.ammo.get();
        int ammoNeeded = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && compute().boltActionTime > 0) {
            bolt.needed.set(false);
        }

        var available = countBackupAmmo(entity);
        var ammoToAdd = Math.min(ammoNeeded, available);

        consumeBackupAmmo(entity, ammoToAdd);
        this.ammo.set(ammo + ammoToAdd);

        reload.setState(ReloadState.NOT_RELOADING);
        this.fireIndex.reset();
    }

    /**
     * 当前状态能否开火
     */
    public boolean canShoot(@Nullable Entity shooter) {
        return item.canShoot(this, shooter);
    }

    /**
     * 无实体情况下开火
     */
    public void shoot(@NotNull ServerLevel level, @NotNull Vec3 shootPosition, @NotNull Vec3 shootDirection, double spread, boolean zoom) {
        this.item.shoot(level, shootPosition, shootDirection, this, spread, zoom, null);
    }

    /**
     * 有实体情况下开火
     */
    public void shoot(@NotNull Entity entity, double spread, boolean zoom, @Nullable UUID uuid) {
        this.item.shoot(this, entity, spread, zoom, uuid);
    }

    public void shoot(@NotNull Entity entity, double spread, boolean zoom, @Nullable UUID uuid, @Nullable Vec3 targetPos) {
        this.item.shoot(this, entity, spread, zoom, uuid, targetPos);
    }

    public void shoot(@NotNull ShootParameters parameters) {
        this.item.shoot(parameters);
    }

    /**
     * 执行tick更新枪械数据
     * <br>
     * 在玩家背包里时会使用GunItem.inventoryTick自动执行
     * <br>
     * 若需要在其他地方使用，请手动调用该方法
     *
     * @param inMainHand 枪械是否在主手上，用于控制部分tick流程是否执行
     */
    public void tick(@Nullable Entity shooter, boolean inMainHand) {
        GunEventHandler.gunTick(shooter, this, inMainHand);
    }

    // 开火相关流程结束

    /**
     * 返还弹匣内弹药，在换弹和切换弹匣配件时调用
     */
    public void withdrawAmmo(@NotNull Entity ammoSupplier) {
        var itemAmount = withdrawAmmoCount();

        this.virtualAmmo.reset();
        this.ammo.reset();

        selectedAmmoConsumer().withdraw(ammoSupplier, itemAmount);
    }

    public int withdrawAmmoCount() {
        return (this.virtualAmmo.get() + this.ammo.get()) / selectedAmmoConsumer().loadAmount;
    }

    /**
     * 返还弹匣内弹药，在换弹和切换弹匣配件时调用
     */
    public void withdrawAmmo(@NotNull IItemHandler handler) {
        var itemAmount = withdrawAmmoCount();

        this.virtualAmmo.reset();
        this.ammo.reset();

        // 直接丢弃余数（恼）
        selectedAmmoConsumer().withdraw(handler, itemAmount);
    }

    private static int getPerkPriority(String s) {
        if (s == null || s.isEmpty()) return 2;

        return switch (s.charAt(0)) {
            case '@' -> 0;
            case '!' -> 2;
            default -> 1;
        };
    }

    public List<Perk> availablePerks() {
        List<Perk> availablePerks = new ArrayList<>();
        var perkNames = compute().availablePerks();
        if (perkNames == null || perkNames.isEmpty()) return availablePerks;

        List<String> sortedNames = new ArrayList<>(perkNames);

        sortedNames.sort((s1, s2) -> {
            int p1 = getPerkPriority(s1);
            int p2 = getPerkPriority(s2);

            if (p1 != p2) {
                return Integer.compare(p1, p2);
            } else {
                return s1.compareTo(s2);
            }
        });

        // TODO 正确实现注册项读取
        var perks = new ArrayList<DeferredHolder<Perk, ? extends Perk>>();
        perks.addAll(ModPerks.AMMO_PERKS.getEntries());
        perks.addAll(ModPerks.DAMAGE_PERKS.getEntries());
        perks.addAll(ModPerks.FUNC_PERKS.getEntries());

        var perkValues = perks.stream().map(DeferredHolder::get).toList();
        var perkKeys = perks.stream().map(perk -> perk.getKey().location().toString()).toList();

        for (String name : sortedNames) {
            if (name.startsWith("@")) {
                String type = name.substring(1);
                switch (type) {
                    case "Ammo" ->
                            availablePerks.addAll(perkValues.stream().filter(perk -> perk.type == Perk.Type.AMMO).toList());
                    case "Functional" ->
                            availablePerks.addAll(perkValues.stream().filter(perk -> perk.type == Perk.Type.FUNCTIONAL).toList());
                    case "Damage" ->
                            availablePerks.addAll(perkValues.stream().filter(perk -> perk.type == Perk.Type.DAMAGE).toList());
                }
            } else if (name.startsWith("!")) {
                String n = name.substring(1);
                var index = perkKeys.indexOf(n);
                if (index != -1) {
                    availablePerks.remove(perkValues.get(index));
                } else {
                    Mod.LOGGER.info("Perk {} not found", n);
                }
            } else {
                var index = perkKeys.indexOf(name);
                if (index != -1) {
                    availablePerks.add(perkValues.get(index));
                } else {
                    Mod.LOGGER.info("Perk {} not found", name);
                }
            }
        }
        return availablePerks;
    }

    public boolean canApplyPerk(Perk perk) {
        return availablePerks().contains(perk);
    }

    public DamageReduce getRawDamageReduce() {
        return getDefault().damageReduce;
    }

    public double getDamageReduceRate() {
        for (Perk.Type type : Perk.Type.values()) {
            var instance = this.perk.getInstance(type);
            if (instance != null) {
                return instance.perk().getModifiedDamageReduceRate(getRawDamageReduce());
            }
        }
        return getRawDamageReduce().getRate();
    }

    public double getDamageReduceMinDistance() {
        for (Perk.Type type : Perk.Type.values()) {
            var instance = this.perk.getInstance(type);
            if (instance != null) {
                return instance.perk().getModifiedDamageReduceMinDistance(getRawDamageReduce());
            }
        }
        return getRawDamageReduce().getMinDistance();
    }

    public boolean meleeOnly() {
        return compute().projectileAmount <= 0 && compute().meleeDamage > 0;
    }

    public boolean isShotgun(DefaultGunData gunData) {
        return gunData.projectileAmount > 1;
    }

    public boolean isShotgun() {
        return isShotgun(compute());
    }

    public Vec3 firePosition() {
        var list = this.compute().shootPos.positions;
        var size = list.size();
        if (size == 0) {
            return Vec3.ZERO;
        }

        if (this.compute().shootPos.boundUpWithAmmoAmount) {
            return list.get(Mth.clamp(this.ammo.get() - 1, 0, size));
        } else {
            return list.get(this.fireIndex.get() % size);
        }
    }

    public Vec3 firePositionForHud() {
        if (this.compute().shootPos.shootPositionForHud != null) {
            return this.compute().shootPos.shootPositionForHud;
        } else {
            return firePosition();
        }
    }

    public StringOrVec3 fireDirection() {
        var list = this.compute().shootPos.directions;
        var size = list.size();
        if (size == 0) {
            return new StringOrVec3("Default");
        }

        return list.get(this.fireIndex.get() % size);
    }

    public StringOrVec3 fireDirectionForHud() {
        return this.compute().shootPos.shootDirectionForHud;
    }

    public IEnergyStorage getEnergyProvider(@Nullable Entity ammoSupplier) {
        return this.item.getEnergyProvider(this, ammoSupplier);
    }

    public void shakePlayers(@Nullable Entity source) {
        if (source == null) return;

        var shootShake = compute().shootShake;
        if (shootShake == null) return;

        ShakeClientMessage.sendToNearbyPlayers(source, shootShake.x, shootShake.y, shootShake.z);
    }

    // 可持久化属性开始

    public final IntValue selectedAmmoType;

    public final IntValue ammo;
    public final IntValue virtualAmmo;

    // backup ammo count override
    public final IntValue backupAmmoCount;

    public final AmmoSlot ammoSlot;

    public final IntValue burstAmount;
    public final IntValue selectedFireMode;
    public final IntValue fireIndex;

    public final IntValue level;
    public final DoubleValue exp;

    // Max: 100
    public final DoubleValue heat;
    public final IntValue shootAnimationTimer;
    public final BooleanValue overHeat;

    public boolean canAdjustZoom() {
        return item.canAdjustZoom(this);
    }

    public boolean canSwitchScope() {
        return item.canSwitchScope(this);
    }

    public final Reload reload;

    /**
     * 是否正在换弹
     */
    public boolean reloading() {
        return reload.state() != ReloadState.NOT_RELOADING;
    }

    public final Charge charge;

    public boolean charging() {
        return charge.time() > 0;
    }

    public final BooleanValue isEmpty;
    public final BooleanValue closeHammer;
    public final BooleanValue closeStrike;
    public final BooleanValue stopped;
    public final BooleanValue forceStop;
    public final IntValue loadIndex;

    public final BooleanValue holdOpen;
    public final BooleanValue hideBulletChain;
    public final IntValue sensitivity;

    public final BooleanValue zooming;

    // 其他子级属性
    public final Bolt bolt;
    public final Attachment attachment;
    public final Perks perk;

    public void save() {
        var keysToRemove = new ArrayList<String>();
        for (var key : perkTag.getAllKeys()) {
            if (perkTag.get(key) instanceof CompoundTag compoundTag && compoundTag.isEmpty()) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.forEach(perkTag::remove);

        var cleanedTag = tag.copy();

        if (perkTag.isEmpty()) {
            cleanedTag.remove("Perks");
        }

        if (attachmentTag.isEmpty()) {
            cleanedTag.remove("Attachments");
        }

        if (gunDataTag.isEmpty()) {
            cleanedTag.remove("GunData");
        }

        if (!tag.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cleanedTag));
        } else {
            stack.remove(DataComponents.CUSTOM_DATA);
        }

        update();
    }

    public static StreamCodec<RegistryFriendlyByteBuf, GunData> VEHICLE_GUN_STREAM_CODEC = new StreamCodec<>() {

        public @NotNull GunData decode(@NotNull RegistryFriendlyByteBuf buf) {
            return GunData.from(new ItemStack(ModItems.VEHICLE_GUN, 1, DataComponentPatch.STREAM_CODEC.decode(buf)));
        }

        public void encode(@NotNull RegistryFriendlyByteBuf buf, GunData data) {
            var newData = data.copy();
            newData.save();
            DataComponentPatch.STREAM_CODEC.encode(buf, newData.stack.getComponentsPatch());
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GunData otherData)) return false;

        return ItemStack.isSameItemSameComponents(otherData.stack, this.stack);
    }

    public GunData copy() {
        var data = GunData.from(this.stack.copy());
        data.defaultDataSupplier = this.defaultDataSupplier;
        return data;
    }
}
