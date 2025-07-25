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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
        var id = stack.getDescriptionId();
        this.id = id.substring(id.indexOf(".") + 1).replace('.', ':');

        this.tag = stack.getOrCreateTag();

        data = getOrPut("GunData");
        perkTag = getOrPut("Perks");
        attachmentTag = getOrPut("Attachments");

        reload = new Reload(this);
        charge = new Charge(this);
        bolt = new Bolt(this);
        attachment = new Attachment(this);
        perk = new Perks(this);

        ammo = new IntValue(data, "Ammo");
        virtualAmmo = new IntValue(data, "VirtualAmmo");

        var defaultFireMode = defaultGunData().defaultFireMode;
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

    DefaultGunData defaultGunData() {
        return GunsTool.gunsData.getOrDefault(id, new DefaultGunData());
    }

//    public int maxDurability() {
//        return Math.max(0, defaultGunData().maxDurability);
//    }

    // 枪械本体属性开始
    public double rawDamage() {
        return defaultGunData().damage;
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
        return defaultGunData().meleeDamage;
    }

    public int meleeDuration() {
        return Math.max(0, defaultGunData().meleeDuration);
    }

    public int meleeDamageTime() {
        return Math.min(meleeDuration(), defaultGunData().meleeDamageTime);
    }

    public double explosionDamage() {
        return defaultGunData().explosionDamage;
    }

    public double explosionRadius() {
        return defaultGunData().explosionRadius;
    }

    public double velocity() {
        return defaultGunData().velocity + item.getCustomVelocity(stack);
    }

    public double spread() {
        return defaultGunData().spread;
    }

    public int magazine() {
        return defaultGunData().magazine + item.getCustomMagazine(stack);
    }

    /**
     * 武器是否直接使用背包内弹药
     */
    public boolean useBackpackAmmo() {
        return magazine() <= 0;
    }

    public ProjectileInfo projectileInfo() {
        var info = defaultGunData().projectile.value;
        if (info == null) return new ProjectileInfo();

        return info;
    }

    public String projectileType() {
        return projectileInfo().type;
    }

    public int rawProjectileAmount() {
        return defaultGunData().projectileAmount;
    }

    public int projectileAmount() {
        var perk = this.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
            return 1;
        }
        return defaultGunData().projectileAmount;
    }

    public double headshot() {
        return defaultGunData().headshot + item.getCustomHeadshot(stack);
    }

    public Set<ReloadType> reloadTypes() {
        if (defaultGunData().reloadTypes == null) return Set.of();

        return defaultGunData().reloadTypes;
    }

    public int defaultNormalReloadTime() {
        return defaultGunData().normalReloadTime;
    }

    public int defaultEmptyReloadTime() {
        return defaultGunData().emptyReloadTime;
    }

    public int defaultIterativeTime() {
        return defaultGunData().iterativeTime;
    }

    public int iterativeAmmoLoadTime() {
        return defaultGunData().iterativeAmmoLoadTime;
    }

    public int iterativeLoadAmount() {
        return defaultGunData().iterativeLoadAmount;
    }

    public int defaultPrepareTime() {
        return defaultGunData().prepareTime;
    }

    public int defaultPrepareLoadTime() {
        return defaultGunData().prepareLoadTime;
    }

    public int prepareAmmoLoadTime() {
        return defaultGunData().prepareAmmoLoadTime;
    }


    public int defaultPrepareEmptyTime() {
        return defaultGunData().prepareEmptyTime;
    }

    public int defaultFinishTime() {
        return defaultGunData().finishTime;
    }

    public int defaultActionTime() {
        return defaultGunData().boltActionTime + item.getCustomBoltActionTime(stack());
    }

    public double soundRadius() {
        return defaultGunData().soundRadius + item.getCustomSoundRadius(stack);
    }

    public double bypassArmor() {
        return defaultGunData().bypassArmor + item.getCustomBypassArmor(stack);
    }

    public double recoilX() {
        return defaultGunData().recoilX;
    }

    public double recoilY() {
        return defaultGunData().recoilY;
    }

    public double recoil() {
        return defaultGunData().recoil;
    }

    public double weight() {
        return defaultGunData().weight + customWeight();
    }

    public double customWeight() {
        return item.getCustomWeight(stack);
    }

    public double defaultZoom() {
        return defaultGunData().defaultZoom;
    }

    public double minZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? defaultGunData().minZoom : 1.25;
    }

    public double maxZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? defaultGunData().maxZoom : 114514;
    }

    public double zoom() {
        if (minZoom() == maxZoom()) return defaultZoom();

        return Mth.clamp(defaultZoom() + item.getCustomZoom(stack), minZoom(), maxZoom());
    }

    public int rpm() {
        return Mth.clamp(defaultGunData().rpm + item.getCustomRPM(stack), 1, 114514);
    }

    public int burstAmount() {
        return defaultGunData().burstAmount;
    }

    public int shootDelay() {
        return defaultGunData().shootDelay;
    }

    public double heatPerShoot() {
        return defaultGunData().heatPerShoot;
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

    public AmmoTypeInfo ammoTypeInfo() {
        var ammoType = defaultGunData().ammoType;
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

    /**
     * 是否还有剩余弹药（不考虑枪内弹药）
     */
    public boolean hasBackupAmmo(@Nullable Entity entity) {
        return countBackupAmmo(entity) > 0;
    }

    public int countBackupAmmo(@Nullable Entity entity) {
        if (entity == null) return virtualAmmo.get();
        if (entity instanceof Player player && player.isCreative()) return Integer.MAX_VALUE;

        var info = ammoTypeInfo();
        if (info.type() == AmmoConsumeType.PLAYER_AMMO && entity instanceof Player player) {
            return Objects.requireNonNull(Ammo.getType(info.value())).get(player) + virtualAmmo.get();
        }

        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(this::countBackupAmmo)
                .orElse(virtualAmmo.get());
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    public int countBackupAmmo(@Nullable IItemHandler handler) {
        if (handler == null) return virtualAmmo.get();
        if (InventoryTool.hasCreativeAmmoBox(handler)) return Integer.MAX_VALUE;

        var info = ammoTypeInfo();
        return switch (info.type()) {
            case ITEM -> InventoryTool.countItem(handler, info.toItem());
            case TAG -> InventoryTool.countItem(handler, info.toTag());
            default -> 0;
        } + virtualAmmo.get();
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
        if (count <= 0) return;

        var info = ammoTypeInfo();
        if (info.type() == AmmoConsumeType.PLAYER_AMMO && entity instanceof Player player) {
            info.toPlayerAmmoType().add(player, -count);
        }

        if (entity != null) {
            int finalCount = count;
            entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .ifPresent(cap -> consumeBackupAmmo(cap, finalCount));
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
        if (count <= 0) return;

        var info = ammoTypeInfo();
        switch (info.type()) {
            case ITEM -> InventoryTool.consumeItem(handler, info.toItem(), count);
            case TAG -> InventoryTool.consumeItem(handler, stack -> stack.is(info.toTag()), count);
        }
    }

    /**
     * 是否拥有足够的弹药进行开火
     */
    public boolean hasEnoughAmmoToShoot(@Nullable Entity entity) {
        return useBackpackAmmo() ? hasBackupAmmo(entity) : this.ammo.get() > 0;
    }

    /**
     * 开始换弹流程
     */
    public void startReload() {
        this.reload.reloadStarter.markStart();
    }

    /**
     * 换弹完成装填弹药，请确保在换弹完成后再调用
     */
    public void reloadAmmo(@Nullable Entity entity) {
        reloadAmmo(entity, false);
    }

    /**
     * 换弹完成装填弹药，请确保在换弹完成后再调用
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
     * 执行tick
     */
    public void tick(@Nullable Entity shooter) {
        GunEventHandler.gunTick(shooter, this);
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
        var perkNames = defaultGunData().availablePerks.list;
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
        if (defaultGunData().availableFireModes == null) return Set.of();

        return defaultGunData().availableFireModes;
    }

    public DamageReduce getRawDamageReduce() {
        return defaultGunData().damageReduce;
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
