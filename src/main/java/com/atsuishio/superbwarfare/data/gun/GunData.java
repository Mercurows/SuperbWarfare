package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.subdata.*;
import com.atsuishio.superbwarfare.data.gun.value.*;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        this.tag = customData != null ? customData.copyTag() : new CompoundTag();

        data = getOrPut("GunData");
        perkTag = getOrPut("Perks");
        attachmentTag = getOrPut("Attachments");

        reload = new Reload(this);
        charge = new Charge(this);
        bolt = new Bolt(this);
        attachment = new Attachment(this);
        perk = new Perks(this);

        ammo = new IntValue(data, "Ammo");

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
        save();
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
        var info = defaultGunData().projectile;
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
        return (defaultGunData().rpm + item.getCustomRPM(stack));
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

        public TagKey<Item> toTag() {
            if (type != AmmoConsumeType.TAG) throw new IllegalArgumentException("not TAG type!");
            return ItemTags.create(ResourceLocation.parse(this.value()));
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
    public boolean hasBackupAmmo(Player player) {
        return countBackupAmmo(player) > 0;
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    public int countBackupAmmo(Player player) {
        if (player.isCreative() || InventoryTool.hasCreativeAmmoBox(player)) return Integer.MAX_VALUE;

        var info = ammoTypeInfo();
        return switch (info.type()) {
            case PLAYER_AMMO -> {
                var type = Ammo.getType(info.value());
                assert type != null;

                yield type.get(player);
            }
            case ITEM -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.getItem().toString().equals(info.value()),
                    0,
                    player.inventoryMenu.getCraftSlots()
            );
            case TAG -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.is(info.toTag()),
                    0,
                    player.inventoryMenu.getCraftSlots()
            );
            case INVALID -> 0;
        };
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    public void consumeBackupAmmo(Player player, int count) {
        if (player.isCreative() || InventoryTool.hasCreativeAmmoBox(player) || count <= 0) return;

        var info = ammoTypeInfo();
        switch (info.type()) {
            case PLAYER_AMMO -> info.toPlayerAmmoType().set(player, info.toPlayerAmmoType().get(player) - count);
            case ITEM -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.getItem().toString().equals(info.value()),
                    count,
                    player.inventoryMenu.getCraftSlots()
            );
            case TAG -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.is(info.toTag()),
                    count,
                    player.inventoryMenu.getCraftSlots()
            );
        }
    }

    /**
     * 是否拥有足够的弹药进行开火
     */
    public boolean hasEnoughAmmoToShoot(Player player) {
        return useBackpackAmmo() ? hasBackupAmmo(player) : this.ammo.get() > 0;
    }

    public void reload(Player player) {
        reload(player, false);
    }

    public void reload(Player player, boolean extraOne) {
        if (useBackpackAmmo()) return;

        int mag = magazine();
        int ammo = this.ammo.get();
        int ammoNeeded = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && defaultActionTime() > 0) {
            bolt.needed.set(false);
        }

        var available = countBackupAmmo(player);
        var ammoToAdd = Math.min(ammoNeeded, available);

        consumeBackupAmmo(player, ammoToAdd);
        this.ammo.set(ammo + ammoToAdd);

        reload.setState(ReloadState.NOT_RELOADING);
    }

    private static int getPriority(String s) {
        if (s == null || s.isEmpty()) return 2;
        if (s.startsWith("@")) return 0;
        else if (s.startsWith("!")) return 2;
        else return 1;
    }

    public List<Perk> availablePerks() {
        List<Perk> availablePerks = new ArrayList<>();
        var perkNames = defaultGunData().availablePerks;
        if (perkNames == null || perkNames.isEmpty()) return availablePerks;

        List<String> sortedNames = new ArrayList<>(perkNames);

        sortedNames.sort((s1, s2) -> {
            int p1 = getPriority(s1);
            int p2 = getPriority(s2);

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

    public void save() {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
