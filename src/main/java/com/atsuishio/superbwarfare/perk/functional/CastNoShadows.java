package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public class CastNoShadows extends Perk {
    public CastNoShadows() {
        super("cast_no_shadows", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void onMeleeAttack(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        super.onMeleeAttack(data, instance, target, source);

        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
            attacker = player;
        }

        if (attacker == null) return;

        float rate = 0.2f + (instance.level() - 1) * 0.03f;

        Player finalAttacker = attacker;
        PlayerVariable.modify(attacker, cap -> {
            int mag = data.get(GunProp.MAGAZINE);
            int ammo = data.ammo.get();
            int ammoReload = (int) Math.min(mag, mag * rate);
            int ammoNeed = Math.min(mag - ammo, ammoReload);

            boolean flag = finalAttacker.isCreative() || InventoryTool.hasCreativeAmmoBox(finalAttacker);

            int ammoFinal = Math.min(data.countBackupAmmo(finalAttacker), ammoNeed);
            if (flag) {
                ammoFinal = ammoNeed;
            } else {
                data.consumeBackupAmmo(finalAttacker, ammoFinal);
            }
            data.ammo.set(Math.min(mag, ammo + ammoFinal));
        });
    }
}
