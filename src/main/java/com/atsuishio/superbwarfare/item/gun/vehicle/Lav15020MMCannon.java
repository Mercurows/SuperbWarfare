package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Lav15020MMCannon extends GunItem {

    public Lav15020MMCannon() {
        super(new Properties().fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/m_79_icon.png");
    }


    public static void summonBullet(
            @Nullable LivingEntity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            boolean aa
    ) {

        SmallCannonShellEntity smallCannonShell = new SmallCannonShellEntity(shooter, level, data.get(GunProp.DAMAGE).floatValue(), data.get(GunProp.EXPLOSION_DAMAGE).floatValue(), data.get(GunProp.EXPLOSION_RADIUS).floatValue(), aa);
        smallCannonShell.setPos(shootPosition.x, shootPosition.y, shootPosition.z);
        smallCannonShell.shoot(shootDirection.x, shootDirection.y, shootDirection.z, data.get(GunProp.VELOCITY).floatValue(),
                data.get(GunProp.SPREAD).floatValue());
        level.addFreshEntity(smallCannonShell);
    }
}