package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public abstract class VehicleWeapon {

    // 武器的图标
    public ResourceLocation icon = Mod.loc("textures/screens/vehicle_weapon/empty.png");
    // 武器的名称
    public Component name;
    // 武器使用的弹药类型
    public Item ammo;
    // 装弹类型
    public AmmoType ammoType = AmmoType.INDIRECT;
    // 最大装弹量（对直接读取备弹的武器无效）
    public int maxAmmo;
    // 当前弹药量（对直接读取备弹的武器无效）
    public int currentAmmo;
    // 备弹量
    public int backupAmmo;

    public SoundEvent sound;
    // 第一人称射击音效
    public SoundEvent sound1p;
    // 第三人称射击音效
    public SoundEvent sound3p;
    public SoundEvent sound3pFar;
    // 第一人称射击音效
    public SoundEvent sound3pVeryFar;


    public VehicleWeapon icon(ResourceLocation icon) {
        this.icon = icon;
        return this;
    }

    public VehicleWeapon name(Component name) {
        this.name = name;
        return this;
    }

    public VehicleWeapon name(String name) {
        this.name = Component.literal(name);
        return this;
    }

    public VehicleWeapon ammo(Item ammo) {
        this.ammo = ammo;
        return this;
    }

    public VehicleWeapon direct() {
        this.ammoType = AmmoType.DIRECT;
        this.maxAmmo = 0;
        this.currentAmmo = 0;
        return this;
    }

    /**
     * 切换到该武器时的音效
     *
     * @param sound 音效
     */
    public VehicleWeapon sound(SoundEvent sound) {
        this.sound = sound;
        return this;
    }

    /**
     * 载具武器的装弹类型
     * INDIRECT - 需要先进行上弹，再发射
     * DIRECT - 直接读取载具存储的弹药
     */
    public enum AmmoType {
        INDIRECT,
        DIRECT,
    }

    public VehicleWeapon maxAmmo(int maxAmmo) {
        this.maxAmmo = maxAmmo;
        return this;
    }

    public VehicleWeapon sound1p(SoundEvent sound1p) {
        this.sound1p = sound1p;
        return this;
    }

    public VehicleWeapon sound3p(SoundEvent sound3p) {
        this.sound3p = sound3p;
        return this;
    }

    public VehicleWeapon sound3pFar(SoundEvent sound3pFar) {
        this.sound3pFar = sound3pFar;
        return this;
    }

    public VehicleWeapon sound3pVeryFar(SoundEvent sound3pVeryFar) {
        this.sound3pVeryFar = sound3pVeryFar;
        return this;
    }
}
