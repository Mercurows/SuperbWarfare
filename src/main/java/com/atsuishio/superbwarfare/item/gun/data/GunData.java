package com.atsuishio.superbwarfare.item.gun.data;

import com.atsuishio.superbwarfare.item.gun.GunItem;
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
    private final ItemStack stack;
    private final GunItem item;
    private final CompoundTag tag;
    private final CompoundTag data;
    private final CompoundTag perkTag;
    private final CompoundTag attachmentTag;
    private final String id;

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

    public int ammo() {
        return data.getInt("Ammo");
    }

    public void setAmmo(int ammo) {
        data.putInt("Ammo", ammo);
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

    public int fireMode() {
        if (data.contains("FireMode")) {
            return data.getInt("FireMode");
        }
        return (int) getGunData("FireMode");
    }

    public void setFireMode(int fireMode) {
        data.putInt("FireMode", fireMode);
    }

    public int level() {
        return data.getInt("Level");
    }

    public void setLevel(int level) {
        data.putInt("Level", level);
    }

    public double exp() {
        return data.getDouble("Exp");
    }

    public void setExp(double exp) {
        data.putDouble("Exp", exp);
    }

    public double upgradePoint() {
        return data.getDouble("UpgradePoint");
    }

    public void setUpgradePoint(double upgradePoint) {
        data.putDouble("UpgradePoint", upgradePoint);
    }

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

    public boolean canImmediatelyShoot() {
        return data.getBoolean("CanImmediatelyShoot");
    }

    public void setCanImmediatelyShoot(boolean value) {
        if (!value) {
            data.remove("CanImmediatelyShoot");
        } else {
            data.putBoolean("CanImmediatelyShoot", true);
        }
    }

    public boolean DA() {
        return data.getBoolean("DA");
    }

    public void setDA(boolean value) {
        if (!value) {
            data.remove("DA");
        } else {
            data.putBoolean("DA", true);
        }
    }

    public boolean isEmpty() {
        return data.getBoolean("IsEmpty");
    }

    public void setIsEmpty(boolean value) {
        if (!value) {
            data.remove("IsEmpty");
        } else {
            data.putBoolean("IsEmpty", true);
        }
    }

    public boolean closeHammer() {
        return data.getBoolean("CloseHammer");
    }

    public void setCloseHammer(boolean value) {
        if (!value) {
            data.remove("CloseHammer");
        } else {
            data.putBoolean("CloseHammer", true);
        }
    }

    public boolean stopped() {
        return data.getBoolean("Stopped");
    }

    public void setStopped(boolean value) {
        if (!value) {
            data.remove("Stopped");
        } else {
            data.putBoolean("Stopped", true);
        }
    }

    public boolean forceStop() {
        return data.getBoolean("ForceStop");
    }

    public void setForceStop(boolean value) {
        if (!value) {
            data.remove("ForceStop");
        } else {
            data.putBoolean("ForceStop", true);
        }
    }

    public int loadIndex() {
        return data.getInt("LoadIndex");
    }

    public void setLoadIndex(int value) {
        if (value == 0) {
            data.remove("LoadIndex");
            return;
        }
        data.putInt("LoadIndex", value);
    }

    public int maxAmmo() {
        return data.getInt("MaxAmmo");
    }

    public void setMaxAmmo(int value) {
        if (value == 0) {
            data.remove("MaxAmmo");
            return;
        }
        data.putInt("MaxAmmo", value);
    }

    public final Bolt bolt;
    public final Attachment attachment;
    public final Perks perk;

    public void save() {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
