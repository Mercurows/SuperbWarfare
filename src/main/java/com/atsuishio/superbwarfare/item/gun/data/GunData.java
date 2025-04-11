package com.atsuishio.superbwarfare.item.gun.data;

import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.subdata.*;
import com.atsuishio.superbwarfare.item.gun.data.value.*;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
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
        fireMode = new IntValue(data, "FireMode", (int) getGunData("FireMode"));
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
        maxAmmo = new IntValue(data, "MaxAmmo");
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

    double getGunData(String key) {
        return getGunData(key, 0);
    }


    // 枪械本体属性开始

    private double getGunData(String key, double defaultValue) {
        return GunsTool.gunsData.getOrDefault(id, new HashMap<>()).getOrDefault(key, defaultValue);
    }

    public double rawDamage() {
        return getGunData("Damage");
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
        return getGunData("ExplosionDamage");
    }

    public double explosionRadius() {
        return getGunData("ExplosionRadius");
    }

    public double velocity() {
        return getGunData("Velocity") + item.getCustomVelocity(stack);
    }

    public double spread() {
        return getGunData("Spread");
    }

    public int magazine() {
        return (int) (getGunData("Magazine") + item.getCustomMagazine(stack));
    }

    public int projectileAmount() {
        return (int) getGunData("ProjectileAmount", 1);
    }

    public double headshot() {
        return getGunData("Headshot", 1.5) + item.getCustomHeadshot(stack);
    }

    public int defaultNormalReloadTime() {
        return (int) getGunData("NormalReloadTime");
    }

    public int defaultEmptyReloadTime() {
        return (int) getGunData("EmptyReloadTime");
    }

    public int defaultIterativeTime() {
        return (int) getGunData("IterativeTime");
    }

    public int defaultPrepareTime() {
        return (int) getGunData("PrepareTime");
    }

    public int defaultPrepareLoadTime() {
        return (int) getGunData("PrepareLoadTime");
    }

    public int defaultPrepareEmptyTime() {
        return (int) getGunData("PrepareEmptyTime");
    }

    public int defaultFinishTime() {
        return (int) getGunData("FinishTime");
    }

    public int defaultActionTime() {
        return (int) getGunData("BoltActionTime") + item.getCustomBoltActionTime(stack());
    }

    public double soundRadius() {
        return getGunData("SoundRadius", 15) + item.getCustomSoundRadius(stack);
    }

    public double bypassArmor() {
        return getGunData("BypassesArmor") + item.getCustomBypassArmor(stack);
    }

    public double recoilX() {
        return getGunData("RecoilX");
    }

    public double recoilY() {
        return getGunData("RecoilY");
    }

    public double weight() {
        return getGunData("Weight") + customWeight();
    }

    public double customWeight() {
        return item.getCustomWeight(stack);
    }


    public double defaultZoom() {
        return getGunData("DefaultZoom", 1.25);
    }

    public double minZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? getGunData("MinZoom", 1.25) : 1.25;
    }

    public double maxZoom() {
        int scopeType = this.attachment.get(AttachmentType.SCOPE);
        return scopeType == 3 ? getGunData("MaxZoom", 1) : 114514;
    }

    public double zoom() {
        if (minZoom() == maxZoom()) return defaultZoom();

        return Mth.clamp(defaultZoom() + item.getCustomZoom(stack), minZoom(), maxZoom());
    }

    public int rpm() {
        return (int) (getGunData("RPM") + item.getCustomRPM(stack));
    }

    public int burstAmount() {
        return (int) getGunData("BurstAmount");
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
    public final IntValue maxAmmo;

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
