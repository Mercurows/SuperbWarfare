package com.atsuishio.superbwarfare.item.gun.data;

import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.subdata.*;
import com.atsuishio.superbwarfare.item.gun.data.value.*;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;
import java.util.WeakHashMap;

public class GunData {
    public final ItemStack stack;
    public final GunItem item;
    public final CompoundTag tag;
    public final CompoundTag data;
    public final CompoundTag perkTag;
    public final CompoundTag attachmentTag;
    public final String id;

    private static final WeakHashMap<ItemStack, GunData> dataCache = new WeakHashMap<>();

    private GunData(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            throw new IllegalArgumentException("stack is not GunItem!");
        }

        this.item = gunItem;
        this.stack = stack;
        var id = stack.getDescriptionId();
        this.id = id.substring(id.lastIndexOf(".") + 1);

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
        fireMode = new IntValue(data, "FireMode", defaultGunData().fireMode);
        level = new IntValue(data, "Level");
        exp = new DoubleValue(data, "Exp");
        upgradePoint = new DoubleValue(data, "UpgradePoint");

        canImmediatelyShoot = new BooleanValue(data, "CanImmediatelyShoot");
        DA = new BooleanValue(data, "DA");
        isEmpty = new BooleanValue(data, "IsEmpty");
        closeHammer = new BooleanValue(data, "CloseHammer");
        stopped = new BooleanValue(data, "Stopped");
        forceStop = new BooleanValue(data, "ForceStop");
        loadIndex = new IntValue(data, "LoadIndex");
        holdOpen = new BooleanValue(data, "HoldOpen");
        hideBulletChain = new BooleanValue(data, "HideBulletChain");
        draw = new BooleanValue(data, "Draw");
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
        var value = dataCache.get(stack);
        if (value == null) {
            value = new GunData(stack);
            dataCache.put(stack, value);
        }
        return value;
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
            return ammoPerk.damageRate;
        }
        return 1;
    }

    public double damage() {
        return (rawDamage() + item.getCustomDamage(stack)) * perkDamageRate();
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

    public int projectileAmount() {
        return defaultGunData().projectileAmount;
    }

    public double headshot() {
        return defaultGunData().headshot + item.getCustomHeadshot(stack);
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

    public int defaultPrepareTime() {
        return defaultGunData().prepareTime;
    }

    public int defaultPrepareLoadTime() {
        return defaultGunData().prepareLoadTime;
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

    public enum AmmoConsumeType {
        PLAYER_AMMO, ITEM, TAG, INVALID,
    }

    public record AmmoTypeInfo(AmmoConsumeType type, String value) {
    }

    public AmmoTypeInfo ammoTypeInfo() {
        var ammoType = defaultGunData().ammoType;
        if (ammoType.isEmpty()) {
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

    public boolean hasAmmo(Player player) {
        return countAmmo(player) > 0;
    }

    public int countAmmo(Player player) {
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
                    p -> p.is(ItemTags.create(ResourceLocation.parse(info.value()))),
                    0,
                    player.inventoryMenu.getCraftSlots()
            );
            case INVALID -> 0;
        };
    }

    public void consumeAmmo(Player player, int count) {
        if (player.isCreative() || InventoryTool.hasCreativeAmmoBox(player)) return;

        var info = ammoTypeInfo();
        switch (info.type()) {
            case PLAYER_AMMO -> {
                var type = Ammo.getType(info.value());
                assert type != null;

                type.set(player, type.get(player) - count);
            }
            case ITEM -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.getItem().toString().equals(info.value()),
                    count,
                    player.inventoryMenu.getCraftSlots()
            );
            case TAG -> player.getInventory().clearOrCountMatchingItems(
                    p -> p.is(ItemTags.create(ResourceLocation.parse(info.value()))),
                    count,
                    player.inventoryMenu.getCraftSlots()
            );
        }
    }

    public void reload(Player player) {
        reload(player, false);
    }

    public void reload(Player player, boolean extraOne) {
        int mag = magazine();
        int ammo = this.ammo.get();
        int ammoNeeded = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && defaultActionTime() > 0 && !stack.is(ModTags.Items.REVOLVER)) {
            bolt.needed.set(false);
        }

        var available = countAmmo(player);
        var ammoToAdd = Math.min(ammoNeeded, available);

        consumeAmmo(player, ammoToAdd);
        this.ammo.set(ammo + ammoToAdd);

        reload.setState(ReloadState.NOT_RELOADING);
    }

    // 可持久化属性开始

    public final IntValue ammo;
    public final IntValue fireMode;
    public final IntValue level;
    public final DoubleValue exp;
    public final DoubleValue upgradePoint;

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

    public final BooleanValue canImmediatelyShoot;
    public final BooleanValue DA;
    public final BooleanValue isEmpty;
    public final BooleanValue closeHammer;
    public final BooleanValue stopped;
    public final BooleanValue forceStop;
    public final IntValue loadIndex;

    public final BooleanValue holdOpen;
    public final BooleanValue hideBulletChain;
    public final BooleanValue draw;

    // 其他子级属性

    public final Bolt bolt;
    public final Attachment attachment;
    public final Perks perk;

    public void save() {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
