package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

public class Subsistence extends Perk {

    public Subsistence() {
        super("subsistence", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
            attacker = player;
        }

        if (DamageTypeTool.isGunDamage(source) && attacker != null) {
            ItemStack stack = data.stack;

            float rate = instance.level() * (0.1f + (stack.is(ModTags.Items.SMG) || stack.is(ModTags.Items.RIFLE) ? 0.07f : 0f));

            var cap = attacker.getData(ModAttachments.PLAYER_VARIABLE).watch();

            int mag = data.magazine();
            int ammo = data.ammo.get();
            int ammoReload = (int) Math.min(mag, mag * rate);
            int ammoNeed = Math.min(mag - ammo, ammoReload);

            boolean flag = attacker.isCreative() || InventoryTool.hasCreativeAmmoBox(attacker);

            int ammoFinal = Math.min(data.countBackupAmmo(attacker), ammoNeed);
            if (flag) {
                ammoFinal = ammoNeed;
            } else {
                data.consumeBackupAmmo(attacker, ammoFinal);
            }
            data.ammo.set(Math.min(mag, ammo + ammoFinal));

            cap.sync(attacker);
        }
    }
}
