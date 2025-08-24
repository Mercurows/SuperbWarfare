package com.atsuishio.superbwarfare.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;
import org.jetbrains.annotations.NotNull;

/**
 * 强行设置自定义耐久的物品属性，适用于需要使用Tier又不想使用Tier提供的耐久的物品的属性注册
 */
public class CustomDamageProperty extends Item.Properties {

    /**
     * 创建有限耐久的物品
     */
    public CustomDamageProperty(int maxDamage) {
        super.durability(maxDamage);
    }

    /**
     * 创建无限耐久的物品
     */
    public CustomDamageProperty(boolean showInTooltip) {
        this.component(DataComponents.UNBREAKABLE, new Unbreakable(showInTooltip));
    }

    public @NotNull CustomDamageProperty durability(int maxDamage) {
        return this;
    }
}
