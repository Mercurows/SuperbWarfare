package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

public class ClientGunImageTooltip implements ClientTooltipComponent {

    protected final int width;
    protected final int height;
    protected final ItemStack stack;
    protected final GunData data;

    public ClientGunImageTooltip(GunImageComponent tooltip) {
        this.width = tooltip.width;
        this.height = tooltip.height;
        this.stack = tooltip.stack;
        this.data = GunData.from(stack);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();

        renderDamageAndRpmTooltip(font, guiGraphics, x, y);
        renderLevelAndUpgradePointTooltip(font, guiGraphics, x, y + 10);

        int yo = 20;
        if (shouldRenderBypassAndHeadshotTooltip()) {
            renderBypassAndHeadshotTooltip(font, guiGraphics, x, y + yo);
            yo += 10;
        }

        if (shouldRenderEnergyTooltip()) {
            yo += 10;
            renderEnergyTooltip(font, guiGraphics, x, y + yo);
            yo += 10;
        }

        if (shouldRenderEditTooltip()) {
            renderWeaponEditTooltip(font, guiGraphics, x, y + yo);
            yo += 20;
        }

        if (shouldRenderPerks()) {
            if (!Screen.hasShiftDown()) {
                renderPerksShortcut(font, guiGraphics, x, y + yo);
            } else {
                renderPerks(font, guiGraphics, x, y + yo);
            }
        }

        guiGraphics.pose().popPose();
    }

    protected boolean shouldRenderBypassAndHeadshotTooltip() {
        return data.get(GunProp.BYPASSES_ARMOR) > 0 || data.get(GunProp.HEADSHOT) > 0;
    }

    protected boolean shouldRenderPerks() {
        return data.perk.get(Perk.Type.AMMO) != null
                || data.perk.get(Perk.Type.DAMAGE) != null
                || data.perk.get(Perk.Type.FUNCTIONAL) != null;
    }

    protected boolean shouldRenderEnergyTooltip() {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.getMaxEnergyStored() > 0;
    }

    protected boolean shouldRenderEditTooltip() {
        if (this.stack.getItem() instanceof GunItem gunItem) {
            return gunItem.canEditAttachments(GunData.from(stack));
        }
        return false;
    }

    /**
     * 渲染武器伤害和射速
     */
    protected void renderDamageAndRpmTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getDamageComponent(), x, y, 0xFFFFFF);
        int xo = font.width(getDamageComponent().getVisualOrderText());
        guiGraphics.drawString(font, getRpmComponent(), x + xo + 16, y, 0xFFFFFF);
    }

    /**
     * 获取武器伤害的文本组件
     */
    protected Component getDamageComponent() {
        double damage = data.get(GunProp.DAMAGE);
        double explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE);

        String dmgStr = FormatTool.format1D(damage);
        if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
            dmgStr = dmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT);
        }

        var component = Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(dmgStr).withStyle(ChatFormatting.GREEN));

        if (explosionDamage > 0) {
            String expDmgStr = FormatTool.format1D(explosionDamage);
            if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                expDmgStr = expDmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT);
            }
            component = component
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" + " + expDmgStr).withStyle(ChatFormatting.GOLD));
        }

        return component;
    }

    /**
     * 获取武器射速的文本组件
     */
    protected Component getRpmComponent() {
        if (!(this.stack.getItem() instanceof GunItem)) return Component.empty();
        var data = GunData.from(this.stack);
        var info = data.selectedFireModeInfo();

        if (info.mode == FireMode.AUTO || info.mode == FireMode.BURST) {
            return Component.translatable("des.superbwarfare.guns.rpm").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format0D(data.get(GunProp.RPM)))
                            .withStyle(ChatFormatting.GREEN));
        }
        return Component.empty();
    }

    /**
     * 渲染武器等级和强化点数
     */
    protected void renderLevelAndUpgradePointTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getLevelComponent(), x, y, 0xFFFFFF);
        int xo = font.width(getLevelComponent().getVisualOrderText());
        guiGraphics.drawString(font, getUpgradePointComponent(), x + xo + 16, y, 0xFFFFFF);
    }

    /**
     * 获取武器等级文本组件
     */
    protected Component getLevelComponent() {
        int level = data.level.get();
        double rate = data.exp.get() / (20 * Math.pow(level, 2) + 160 * level + 20);

        ChatFormatting formatting;
        if (level < 10) {
            formatting = ChatFormatting.WHITE;
        } else if (level < 20) {
            formatting = ChatFormatting.AQUA;
        } else if (level < 30) {
            formatting = ChatFormatting.LIGHT_PURPLE;
        } else if (level < 40) {
            formatting = ChatFormatting.GOLD;
        } else {
            formatting = ChatFormatting.RED;
        }

        return Component.translatable("des.superbwarfare.guns.level").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(level + "").withStyle(formatting).withStyle(ChatFormatting.BOLD))
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(" (" + FormatTool.DECIMAL_FORMAT_2ZZZ.format(rate * 100) + "%)").withStyle(ChatFormatting.GRAY));
    }

    /**
     * 获取武器强化点数文本组件
     */
    protected Component getUpgradePointComponent() {
        int upgradePoint = Mth.floor(data.upgradePoint.get());
        return Component.translatable("des.superbwarfare.guns.upgrade_point").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(String.valueOf(upgradePoint)).withStyle(ChatFormatting.WHITE).withStyle(ChatFormatting.BOLD));
    }

    /**
     * 渲染武器穿甲比例和爆头倍率
     */
    protected void renderBypassAndHeadshotTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getBypassComponent(), x, y, 0xFFFFFF);
        int xo = font.width(getBypassComponent().getVisualOrderText());
        guiGraphics.drawString(font, getHeadshotComponent(), x + xo + 16, y, 0xFFFFFF);
    }

    /**
     * 获取武器穿甲比例文本组件
     */
    protected Component getBypassComponent() {
        double bypassRate = Math.max(data.get(GunProp.BYPASSES_ARMOR), 0);
        return Component.translatable("des.superbwarfare.guns.bypass").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format2D(bypassRate * 100, "%")).withStyle(ChatFormatting.GOLD));
    }

    /**
     * 获取武器爆头倍率文本组件
     */
    protected Component getHeadshotComponent() {
        double headshot = data.get(GunProp.HEADSHOT);
        return Component.translatable("des.superbwarfare.guns.headshot").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(headshot, "x")).withStyle(ChatFormatting.AQUA));
    }

    /**
     * 渲染武器能量信息
     */
    protected void renderEnergyTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getEnergyComponent(), x, y, 0xFFFFFF);
    }

    /**
     * 获取武器能量文本组件
     */
    protected Component getEnergyComponent() {
        var storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        assert storage != null;

        int energy = storage.getEnergyStored();
        int maxEnergy = storage.getMaxEnergyStored();
        float percentage = Mth.clamp((float) energy / maxEnergy, 0, 1);
        MutableComponent component = Component.empty();

        ChatFormatting format;
        if (percentage <= .2f) {
            format = ChatFormatting.RED;
        } else if (percentage <= .6f) {
            format = ChatFormatting.YELLOW;
        } else {
            format = ChatFormatting.GREEN;
        }

        int count = (int) (percentage * 50);
        for (int i = 0; i < count; i++) {
            component.append(Component.literal("|").withStyle(format));
        }
        component.append(Component.empty().withStyle(ChatFormatting.RESET));
        for (int i = 0; i < 50 - count; i++) {
            component.append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }

        component.append(Component.literal(" " + energy + "/" + maxEnergy + " FE").withStyle(ChatFormatting.GRAY));

        return component;
    }

    /**
     * 渲染武器改装信息
     */
    protected void renderWeaponEditTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getEditComponent(), x, y + 10, 0xFFFFFF);
    }

    /**
     * 获取武器改装信息文本组件
     */
    protected Component getEditComponent() {
        return Component.translatable("des.superbwarfare.guns.edit", "[" + ModKeyMappings.EDIT_MODE.getKey().getDisplayName().getString() + "]")
                .withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.ITALIC);
    }

    /**
     * 渲染武器模组缩略图
     */
    protected void renderPerksShortcut(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.pose().pushPose();

        int xOffset = -20;

        for (var type : Perk.Type.values()) {
            var perkInstance = data.perk.getInstance(type);
            if (perkInstance == null) continue;

            xOffset += 20;

            var ammoItem = perkInstance.perk().getItem().get();
            ItemStack perkStack = ammoItem.getDefaultInstance();

            int level = perkInstance.level();
            perkStack.setCount(level);
            guiGraphics.renderItem(perkStack, x + xOffset, y + 2);
            guiGraphics.renderItemDecorations(font, perkStack, x + xOffset, y + 2);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * 渲染武器模组详细信息
     */
    protected void renderPerks(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.pose().pushPose();

        guiGraphics.drawString(font, Component.translatable("perk.superbwarfare.tips").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.UNDERLINE), x, y + 10, 0xFFFFFF);

        int yOffset = -5;

        for (var type : Perk.Type.values()) {
            var perkInstance = data.perk.getInstance(type);
            if (perkInstance == null) continue;

            yOffset += 25;

            var ammoItem = perkInstance.perk().getItem().get();
            guiGraphics.renderItem(ammoItem.getDefaultInstance(), x, y + 4 + yOffset);

            var id = perkInstance.perk().descriptionId;

            var component = Component.translatable("item.superbwarfare." + id).withStyle(type.getColor())
                    .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" Lvl. " + perkInstance.level()).withStyle(ChatFormatting.WHITE));
            var descComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);

            guiGraphics.drawString(font, component, x + 20, y + yOffset + 2, 0xFFFFFF);
            guiGraphics.drawString(font, descComponent, x + 20, y + yOffset + 12, 0xFFFFFF);

        }

        guiGraphics.pose().popPose();
    }

    protected int getDefaultMaxWidth(Font font) {
        int width = font.width(getDamageComponent().getVisualOrderText()) + font.width(getRpmComponent().getVisualOrderText()) + 16;
        width = Math.max(width, font.width(getLevelComponent().getVisualOrderText()) + font.width(getUpgradePointComponent().getVisualOrderText()) + 16);
        if (shouldRenderBypassAndHeadshotTooltip()) {
            width = Math.max(width, font.width(getBypassComponent().getVisualOrderText()) + font.width(getHeadshotComponent().getVisualOrderText()) + 16);
        }
        if (shouldRenderEditTooltip()) {
            width = Math.max(width, font.width(getEditComponent().getVisualOrderText()) + 16);
        }

        return width + 4;
    }

    protected int getMaxPerkDesWidth(Font font) {
        if (!shouldRenderPerks()) return 0;

        int width = 0;

        for (var type : Perk.Type.values()) {
            var perkInstance = data.perk.getInstance(type);
            if (perkInstance == null) continue;

            var id = perkInstance.perk().descriptionId;

            var ammoDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);
            width = Math.max(width, font.width(ammoDesComponent));
        }

        return width + 25;
    }

    @Override
    public int getHeight() {
        int height = Math.max(20, this.height);

        if (shouldRenderBypassAndHeadshotTooltip()) height += 10;
        if (shouldRenderEnergyTooltip()) height += 20;
        if (shouldRenderEditTooltip()) height += 20;
        if (shouldRenderPerks()) {
            height += 16;

            if (Screen.hasShiftDown()) {
                for (var type : Perk.Type.values()) {
                    if (data.perk.has(type)) {
                        height += 25;
                    }
                }
            }
        }

        return height;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        int width = getMaxPerkDesWidth(font);

        if (Screen.hasShiftDown()) {
            if (width == 0) {
                width = Math.max(this.width, getDefaultMaxWidth(font));
            } else {
                width = Math.max(width, getDefaultMaxWidth(font));
            }
        } else {
            width = getDefaultMaxWidth(font);
        }

        if (shouldRenderEnergyTooltip()) {
            width = Math.max(width, font.width(getEnergyComponent().getVisualOrderText()) + 10);
        }

        return width;
    }
}
