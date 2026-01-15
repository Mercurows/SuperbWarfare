package com.atsuishio.superbwarfare.perk;

import com.atsuishio.superbwarfare.data.gun.DamageReduce;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunPropertyModifier;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.PerkItem;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class Perk implements GunPropertyModifier {

    public final String descriptionId;
    public final String name;
    public final Type type;

    public Perk(String descriptionId, Type type) {
        this.descriptionId = descriptionId;
        this.type = type;

        StringBuilder builder = new StringBuilder();
        boolean useUpperCase = false;
        boolean isFirst = true;
        for (char c : descriptionId.toCharArray()) {
            if (isFirst || useUpperCase) {
                builder.append(String.valueOf(c).toUpperCase(Locale.ROOT));
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
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
    }

    public void preReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
    }

    public void postReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
    }

    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
    }

    public void onHurtEntity(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
    }

    public void onHit(LivingEntity attacker, GunData data, PerkInstance instance, Entity target) {
    }

    public int getModifiedCustomRPM(int rpm, GunData data, PerkInstance instance) {
        return rpm;
    }

    /**
     * 在切换物品时触发
     */
    public void onChangeSlot(GunData data, PerkInstance instance, @Nullable Entity living) {
    }

    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        return damage;
    }

    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
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

    /**
     * 用于处理武器近战攻击后的逻辑
     */
    public void onMeleeAttack(GunData data, PerkInstance instance, Entity target, DamageSource source) {
    }

    public boolean is(TagKey<Perk> tag) {
        return ModPerks.PERK_REGISTRY.getHolder(ResourceKey.create(ModPerks.PERK_KEY, ResourceLocation.parse(this.descriptionId)))
                .map(a -> a.is(tag)).orElse(false);
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
