package com.atsuishio.superbwarfare.tools;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LivingKillRecord {
    public LivingEntity attacker;
    public Entity target;
    public ItemStack stack;
    public boolean headshot;
    public int tick;
    public boolean freeze;
    public boolean fastRemove;
    public ResourceKey<DamageType> damageType;

    public LivingKillRecord(LivingEntity attacker, Entity target, ItemStack stack, boolean headshot, ResourceKey<DamageType> damageType) {
        this.attacker = attacker;
        this.target = target;
        this.stack = stack;
        this.headshot = headshot;
        this.tick = 0;
        this.damageType = damageType;
    }
}
