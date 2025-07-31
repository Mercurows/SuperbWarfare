package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.subdata.*;
import com.atsuishio.superbwarfare.data.gun.value.*;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class GunData {

    public final ItemStack stack;
    public final GunItem item;
    public final CompoundTag tag;
    public final CompoundTag data;
    public final CompoundTag perkTag;
    public final CompoundTag attachmentTag;
    public final String id;
    public final List<AmmoConsumer> ammoConsumers;

    public static final LoadingCache<ItemStack, GunData> dataCache = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<>() {
                public @NotNull GunData load(@NotNull ItemStack stack) {
                    return new GunData(stack);
                }
            });

    private GunData(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        this.id = getRegistryId(stack.getItem());

        this.tag = stack.getOrCreateTag();

        data = getOrPut("GunData");
        perkTag = getOrPut("Perks");
        attachmentTag = getOrPut("Attachments");
        ammoConsumers = getDefault().ammoConsumers.list.stream().map(c -> c.value.setData(this)).toList();

        // 可持久化属性
        reload = new Reload(this);
        charge = new Charge(this);
        bolt = new Bolt(this);
        attachment = new Attachment(this);
        perk = new Perks(this);

        insertedItem = new ItemStackValue(data, "InsertedItem");
        selectedAmmoType = new IntValue(data, "SelectedAmmoType");

        ammo = new IntValue(data, "Ammo");
        virtualAmmo = new IntValue(data, "VirtualAmmo");

        var defaultFireMode = getDefault().defaultFireMode;
        if (defaultFireMode == null) {
            defaultFireMode = FireMode.SEMI;
        }

        fireMode = new StringEnumValue<>(data, "FireMode", defaultFireMode, FireMode::fromValue);
        level = new IntValue(data, "Level");
        exp = new DoubleValue(data, "Exp");
        upgradePoint = new DoubleValue(data, "UpgradePoint");

        isEmpty = new BooleanValue(data, "IsEmpty");
        closeHammer = new BooleanValue(data, "CloseHammer");
        stopped = new BooleanValue(data, "Stopped");
        forceStop = new BooleanValue(data, "ForceStop");
        loadIndex = new IntValue(data, "LoadIndex");
        holdOpen = new BooleanValue(data, "HoldOpen");
        hideBulletChain = new BooleanValue(data, "HideBulletChain");
        draw = new BooleanValue(data, "Draw");
        sensitivity = new IntValue(data, "Sensitivity");
        heat = new DoubleValue(data, "Heat");
        overHeat = new BooleanValue(data, "OverHeat");
    }

    private CompoundTag getOrPut(String name) {
        CompoundTag tag;
        if (!this.tag.contains(name)) {
            tag = new CompoundTag();
            this.tag.put(name, tag);
        } else {
            tag = this.tag.getCompound(name);
        }
        return tag;
    }

    public boolean initialized() {
        return data.hasUUID("UUID");
    }

    public void initialize() {
        if (initialized()) return;

        data.putUUID("UUID", UUID.randomUUID());
    }

    public static GunData from(ItemStack stack) {
        return dataCache.getUnchecked(stack);
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
        return data;
    }

    public CompoundTag perk() {
        return perkTag;
    }

    public CompoundTag attachment() {
        return attachmentTag;
    }

    public static DefaultGunData getDefault(String id) {
        var isDefault = !GunsTool.gunsData.containsKey(id);
        var data = GunsTool.gunsData.getOrDefault(id, new DefaultGunData());
        data.isDefaultData = isDefault;
        return data;
    }

    public DefaultGunData getDefault() {
        return getDefault(this.id);
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

//    public int maxDurability() {
//        return Math.max(0, getDefault().maxDurability);
//    }

    // 枪械本体属性开始
    public double rawDamage() {
        return getDefault().damage;
    }

    public double perkDamageRate() {
        var perk = this.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            if (ammoPerk.slug) {
                return ammoPerk.damageRate * rawProjectileAmount();
            }
            return ammoPerk.damageRate;
        }
        return 1;
    }

    public double damage() {
        return (rawDamage() + item.getCustomDamage(stack)) * perkDamageRate();
    }

    public double meleeDamage() {
        return getDefault().meleeDamage;
    }

    public int meleeDuration() {
        return Math.max(0, getDefault().meleeDuration);
    }

    public int meleeDamageTime() {
        return Math.min(meleeDuration(), getDefault().meleeDamageTime);
    }

    public double explosionDamage() {
        return getDefault().explosionDamage;
    }

    public double explosionRadius() {
        return getDefault().explosionRadius;
    }

    public double velocity() {
        return getDefault().velocity + item.getCustomVelocity(stack);
    }

    public double spread() {
        return getDefault().spread;
    }

    public int magazine() {
        return getDefault().magazine + item.getCustomMagazine(stack);
    }

    /**
     * 武器是否直接使用背包内弹药
     */
    public boolean useBackpackAmmo() {
        return magazine() <= 0;
    }

    public ProjectileInfo projectileInfo() {
        var info = getDefault().projectile.value;
        if (info == null) return new ProjectileInfo();

        return info;
    }

    public String projectileType() {
        return projectileInfo().type;
    }

    public int rawProjectileAmount() {
        return getDefault().projectileAmount;
    }

    public int projectileAmount() {
        var perk = this.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
            return 1;
        }
        return getDefault().projectileAmount;
    }

    public double headshot() {
        return getDefault().headshot + item.getCustomHeadshot(stack);
    }

    public Set<ReloadType> reloadTypes() {
        if (getDefault().reloadTypes == null) return Set.of();

        return getDefault().reloadTypes;
    }

    public int defaultNormalReloadTime() {
        return getDefault().normalReloadTime;
    }

    public int defaultEmptyReloadTime() {
        return getDefault().emptyReloadTime;
    }

    public int defaultIterativeTime() {
        return getDefault().iterativeTime;
    }

    public int iterativeAmmoLoadTime() {
        return getDefault().iterativeAmmoLoadTime;
    }

    public int iterativeLoadAmount() {
        return getDefault().iterativeLoadAmount;
    }

    public int defaultPrepareTime() {
        return getDefault().prepareTime;
    }

    public int defaultPrepareLoadTime() {
        return getDefault().prepareLoadTime;
    }

    public int prepareAmmoLoadTime() {
        return getDefault().prepareAmmoLoadTime;
    }


    public int defaultPrepareEmptyTime() {
        return getDefault().prepareEmptyTime;
    }

    public int defaultFinishTime() {
        return getDefault().finishTime;
    }

    public int defaultActionTime() {
        return getDefault().boltActionTime + item.getCustomBoltActionTime(stack());
    }

    public double soundRadius() {
        return getDefault().soundRadius + item.getCustomSoundRadius(stack);
    }

    public double bypassArmor() {
        return getDefault().bypassArmor + item.getCustomBypassArmor(stack);
    }

    public double recoilX() {
        return getDefault().recoilX;
    }

    public double recoilY() {
        return getDefault().recoilY;
    }

    public double recoil() {
        return getDefault().recoil;
    }

    public double weight() {
        return getDefault().weight + customWeight();
    }

    public double customWeight() {
        return item.getCustomWeight(stack);
    }

    public double defaultZoom() {
        return getDefault().defaultZoom;
    }

    public double minZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? getDefault().minZoom : 1.25;
    }

    public double maxZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? getDefault().maxZoom : 114514;
    }

    public double zoom() {
        if (minZoom() == maxZoom()) return defaultZoom();

        return Mth.clamp(defaultZoom() + item.getCustomZoom(stack), minZoom(), maxZoom());
    }

    public int rpm() {
        return Mth.clamp(getDefault().rpm + item.getCustomRPM(stack), 1, 114514);
    }

    public int burstAmount() {
        return getDefault().burstAmount;
    }

    public int shootDelay() {
        return getDefault().shootDelay;
    }

    public double heatPerShoot() {
        return getDefault().heatPerShoot;
    }

    public enum AmmoConsumeType {
        PLAYER_AMMO, ITEM, TAG, INVALID,
    }

    public record AmmoTypeInfo(AmmoConsumeType type, String value) {
        /**
         * 尝试返回Ammo类型
         */
        public @Nullable Ammo playerAmmoType() {
            if (type != AmmoConsumeType.PLAYER_AMMO) return null;
            return toPlayerAmmoType();
        }

        public @NotNull Ammo toPlayerAmmoType() {
            if (type != AmmoConsumeType.PLAYER_AMMO) throw new IllegalArgumentException("not PLAYER_AMMO type!");
            return Objects.requireNonNull(Ammo.getType(value));
        }

        public @NotNull TagKey<Item> toTag() {
            if (type != AmmoConsumeType.TAG) throw new IllegalArgumentException("not TAG type!");
            return ItemTags.create(Objects.requireNonNull(ResourceLocation.tryParse(this.value)));
        }

        public @NotNull Item toItem() {
            if (type != AmmoConsumeType.ITEM) throw new IllegalArgumentException("not ITEM type!");
            return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(this.value)));
        }
    }

    // TODO 清理这个
    public AmmoTypeInfo ammoTypeInfo() {
        var ammoType = getDefault().ammoType;
        if (ammoType == null || ammoType.isEmpty()) {
            return new AmmoTypeInfo(AmmoConsumeType.INVALID, "");
        }

        // 玩家弹药
        if (ammoType.startsWith("@")) {
            if (Ammo.getType(ammoType.substring(1)) == null) {
                return new AmmoTypeInfo(AmmoConsumeType.INVALID, ammoType.substring(1));
            }
            return new AmmoTypeInfo(AmmoConsumeType.PLAYER_AMMO, ammoType.substring(1));
        }

        // 物品Tag
        if (ammoType.startsWith("#")) {
            if (ResourceLocation.tryParse(ammoType.substring(1)) == null) {
                return new AmmoTypeInfo(AmmoConsumeType.INVALID, ammoType.substring(1));
            }
            return new AmmoTypeInfo(AmmoConsumeType.TAG, ammoType.substring(1));
        }

        // 普通物品
        if (ResourceLocation.tryParse(ammoType) == null) {
            return new AmmoTypeInfo(AmmoConsumeType.INVALID, ammoType);
        }
        return new AmmoTypeInfo(AmmoConsumeType.ITEM, ammoType);
    }

    public AmmoConsumer selectedAmmoConsumer() {
        if (this.ammoConsumers.isEmpty()) {
            return AmmoConsumer.INVALID;
        }
        return this.ammoConsumers.get(Mth.clamp(this.selectedAmmoType.get(), 0, this.ammoConsumers.size() - 1));
    }

    // 开火相关流程开始

    /*
     * 开火相关流程描述
     * 1. 调用hasEnoughAmmoToShoot(@Nullable Entity shooter)查看是否拥有足够的枪内弹药开火，没有弹药时可以尝试调用startReload()开始换弹流程
     * 2. 调用canShoot(@Nullable Entity shooter)查看当前状态是否能够开火，如果能够开火则调用shootBullet进行开火
     * 3. 调用tick(@Nullable Entity shooter)执行枪械tick任务，包括换弹流程、过热计算、拉栓等
     *
     * 可选项：
     * 1. 使用GunData.virtualAmmo.set来设置虚拟弹药数量
     * 2. 传入带有IItemHandler能力的任意Entity来提供额外弹药
     *
     */

    /**
     * 是否还有剩余弹药（不考虑枪内弹药）
     */
    public boolean hasBackupAmmo(@Nullable Entity entity) {
        return countBackupAmmo(entity) > 0;
    }

    public int countBackupAmmo(@Nullable Entity entity) {
        if (entity == null) return virtualAmmo.get();
        if (entity instanceof Player player && player.isCreative() || InventoryTool.hasCreativeAmmoBox(entity))
            return Integer.MAX_VALUE;

        return this.selectedAmmoConsumer().count(entity) * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get();
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    public int countBackupAmmo(@Nullable IItemHandler handler) {
        if (handler == null) return virtualAmmo.get();
        if (InventoryTool.hasCreativeAmmoBox(handler)) return Integer.MAX_VALUE;

        return this.selectedAmmoConsumer().count(handler) * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get();
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    public void consumeBackupAmmo(@Nullable Entity entity, int count) {
        if (count <= 0 || entity instanceof Player player && player.isCreative()) return;

        if (virtualAmmo.get() > 0) {
            var consumed = Math.min(virtualAmmo.get(), count);
            virtualAmmo.add(-consumed);
            count -= consumed;
        }
        if (count <= 0 || entity == null) return;

        var consumer = this.selectedAmmoConsumer();
        var loadAmount = consumer.loadAmount;
        if (count % loadAmount != 0) {
            var required = (count / loadAmount) + 1;
            var consumed = consumer.consume(entity, required);
            count -= consumed * loadAmount;

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count);
            }
        } else {
            consumer.consume(entity, count / loadAmount);
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
        }
        if (count <= 0 || handler == null) return;

        var consumer = selectedAmmoConsumer();
        var loadAmount = consumer.loadAmount;

        if (count % loadAmount != 0) {
            var required = (count / loadAmount) + 1;
            var consumed = consumer.consume(handler, required);
            count -= consumed * loadAmount;

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count);
            }
        } else {
            consumer.consume(handler, count / loadAmount);
        }
    }

    /**
     * 是否拥有足够的弹药进行开火
     */
    public boolean hasEnoughAmmoToShoot(@Nullable Entity entity) {
        return useBackpackAmmo() ? hasBackupAmmo(entity) : this.ammo.get() > 0;
    }

    /**
     * 开始换弹流程，换弹将在tick内被执行
     */
    public void startReload() {
        this.reload.reloadStarter.markStart();
    }

    /**
     * 换弹完成后装填弹药，请确保在换弹完成后再调用
     */
    public void reloadAmmo(@Nullable Entity entity) {
        reloadAmmo(entity, false);
    }

    /**
     * 换弹完成后装填弹药，请确保在换弹完成后再调用
     */
    public void reloadAmmo(@Nullable Entity entity, boolean extraOne) {
        if (useBackpackAmmo()) return;

        int mag = magazine();
        int ammo = this.ammo.get();
        int ammoNeeded = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && defaultActionTime() > 0) {
            bolt.needed.set(false);
        }

        var available = countBackupAmmo(entity);
        var ammoToAdd = Math.min(ammoNeeded, available);

        consumeBackupAmmo(entity, ammoToAdd);
        this.ammo.set(ammo + ammoToAdd);

        reload.setState(ReloadState.NOT_RELOADING);
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
    public void withdrawAmmo(@NotNull Entity shooter) {
        var amount = this.virtualAmmo.get() + this.ammo.get();
        this.virtualAmmo.reset();
        this.ammo.reset();

        // 直接丢弃余数（恼）
        var itemAmount = amount / selectedAmmoConsumer().loadAmount;
        selectedAmmoConsumer().withdraw(shooter, itemAmount);

        this.insertedItem.set(selectedAmmoConsumer().toItemStack());
    }

    /**
     * 返还弹匣内弹药，在换弹和切换弹匣配件时调用
     */
    public void withdrawAmmo(@NotNull IItemHandler handler) {
        var amount = this.virtualAmmo.get() + this.ammo.get();

        // 直接丢弃余数（恼）
        var itemAmount = amount / selectedAmmoConsumer().loadAmount;
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
        var perkNames = getDefault().availablePerks.list;
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

        var perks = RegistryManager.ACTIVE.getRegistry(ModPerks.PERK_KEY).getEntries();
        var perkValues = perks.stream().map(Map.Entry::getValue).toList();
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

    public Set<FireMode> getAvailableFireModes() {
        if (getDefault().availableFireModes == null) return Set.of();

        return getDefault().availableFireModes;
    }

    public DamageReduce getRawDamageReduce() {
        return getDefault().damageReduce;
    }

    public double getRawDamageReduceRate() {
        return getRawDamageReduce().getRate();
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

    public double getRawDamageReduceMinDistance() {
        return getRawDamageReduce().getMinDistance();
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

    // 可持久化属性开始

    public final ItemStackValue insertedItem;
    public final IntValue selectedAmmoType;

    public final IntValue ammo;
    public final IntValue virtualAmmo;
    public final StringEnumValue<FireMode> fireMode;
    public final IntValue level;
    public final DoubleValue exp;
    public final DoubleValue upgradePoint;
    public final DoubleValue heat;

    public final BooleanValue overHeat;

    public boolean canAdjustZoom() {
        return item.canAdjustZoom(stack);
    }

    public boolean canSwitchScope() {
        return item.canSwitchScope(stack);
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
    public final BooleanValue stopped;
    public final BooleanValue forceStop;
    public final IntValue loadIndex;

    public final BooleanValue holdOpen;
    public final BooleanValue hideBulletChain;
    public final BooleanValue draw;
    public final IntValue sensitivity;

    // 其他子级属性
    public final Bolt bolt;
    public final Attachment attachment;
    public final Perks perk;
}
