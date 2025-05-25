package com.atsuishio.superbwarfare.perk;

import com.atsuishio.superbwarfare.data.gun.DamageReduce;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.PerkItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

public class Perk {

    public final String descriptionId;
    public final String name;
    public final Type type;
    public int level = 1;

    public Perk(String descriptionId, Type type) {
        this.descriptionId = descriptionId;
        this.type = type;

        StringBuilder builder = new StringBuilder();
        boolean useUpperCase = false;
        boolean isFirst = true;
        for (char c : descriptionId.toCharArray()) {
            if (isFirst || useUpperCase) {
                builder.append(Character.toUpperCase(c));
                isFirst = false;
                useUpperCase = false;
            } else if (c == '_') {
                useUpperCase = true;
            } else {
                builder.append(c);
            }
        }

        this.name = builder.toString();
    }

    public DeferredHolder<Item, ? extends Item> getItem() {
        var result = ModItems.PERKS.getEntries().stream().filter(p -> {
            if (p.get() instanceof PerkItem<?> perkItem) {
                return perkItem.getPerk() == this;
            }
            return false;
        }).findFirst();
        if (result.isEmpty()) throw new IllegalStateException("Perk " + this.name + " not found");

        return result.get();
    }

    /**
     * 在背包中每Tick触发
     */
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
    }

    public void preReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
    }

    public void postReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
    }

    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
    }

    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
    }

    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        return damage;
    }

    public int getModifiedRPM(int rpm, GunData data, PerkInstance instance) {
        return rpm;
    }

    public int getModifiedCustomRPM(int rpm, GunData data, PerkInstance instance) {
        return rpm;
    }

    /**
     * 在切换物品时触发
     */
    public void onChangeSlot(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
    }

    public boolean shouldCancelHurtEvent(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        return false;
    }

    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
    }

    /**
     * 用于武器伤害信息显示
     */
    public double getDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return damage;
    }

    /**
     * 用于武器额外伤害信息显示，默认为负数
     */
    public double getExtraDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return -1;
    }

    /**
     * 用于处理武器伤害衰减比率
     */
    public double getModifiedDamageReduceRate(DamageReduce reduce) {
        return reduce.getRate();
    }

    /**
     * 用于处理武器伤害衰减最小距离
     */
    public double getModifiedDamageReduceMinDistance(DamageReduce reduce) {
        return reduce.getMinDistance();
    }

    public enum Type {
        AMMO("Ammo", ChatFormatting.YELLOW),
        FUNCTIONAL("Functional", ChatFormatting.GREEN),
        DAMAGE("Damage", ChatFormatting.RED);
        private final String name;
        private final ChatFormatting color;

        Type(String type, ChatFormatting color) {
            this.name = type;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public ChatFormatting getColor() {
            return color;
        }
    }
}
