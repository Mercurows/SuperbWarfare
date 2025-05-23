package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
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
        if (shouldRenderBypassAndHeadshotTooltip(stack)) {
            renderBypassAndHeadshotTooltip(font, guiGraphics, x, y + yo);
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

    protected boolean shouldRenderBypassAndHeadshotTooltip(ItemStack stack) {
        return !stack.is(ModTags.Items.LAUNCHER);
    }

    protected boolean shouldRenderEditTooltip() {
        if (this.stack.getItem() instanceof GunItem gunItem) {
            return gunItem.isCustomizable(stack);
        }
        return false;
    }

    protected boolean shouldRenderPerks() {
        return data.perk.get(Perk.Type.AMMO) != null
                || data.perk.get(Perk.Type.DAMAGE) != null
                || data.perk.get(Perk.Type.FUNCTIONAL) != null;
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
        double damage = data.damage();
        double extraDamage = -1;
        for (var type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                damage = instance.perk().getDisplayDamage(damage, data, instance);
                if (instance.perk().getExtraDisplayDamage(damage, data, instance) >= 0) {
                    extraDamage = instance.perk().getExtraDisplayDamage(damage, data, instance);
                }
            }
        }
        String dmgStr = FormatTool.format1D(damage) + (extraDamage >= 0 ? " + " + FormatTool.format1D(extraDamage) : "");
        if (data.projectileAmount() > 1) {
            if (extraDamage >= 0) {
                dmgStr = "(" + dmgStr + ") * " + data.projectileAmount();
            } else {
                dmgStr = dmgStr + " * " + data.projectileAmount();
            }
        }

        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(dmgStr)
                        .withStyle(ChatFormatting.GREEN));
    }

    /**
     * 获取武器射速的文本组件
     */
    protected Component getRpmComponent() {
        if (this.stack.getItem() instanceof GunItem &&
                (GunData.from(this.stack).getAvailableFireModes().contains(FireMode.AUTO)
                        || GunData.from(this.stack).getAvailableFireModes().contains(FireMode.BURST))) {
            return Component.translatable("des.superbwarfare.guns.rpm").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format0D(data.rpm()))
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
        double perkBypassArmorRate = 0;
        var perk = data.perk.get(Perk.Type.AMMO);

        if (perk instanceof AmmoPerk ammoPerk) {
            var perkInstance = data.perk.getInstance(perk);
            int level = perkInstance == null ? 0 : perkInstance.level();
            perkBypassArmorRate = ammoPerk.bypassArmorRate + (perk == ModPerks.AP_BULLET.get() ? 0.05f * (level - 1) : 0);
        }
        double bypassRate = Math.max(data.bypassArmor() + perkBypassArmorRate, 0);

        return Component.translatable("des.superbwarfare.guns.bypass").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format2D(bypassRate * 100, "%")).withStyle(ChatFormatting.GOLD));
    }

    /**
     * 获取武器爆头倍率文本组件
     */
    protected Component getHeadshotComponent() {
        double headshot = data.headshot();
        return Component.translatable("des.superbwarfare.guns.headshot").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(headshot, "x")).withStyle(ChatFormatting.AQUA));
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
        if (shouldRenderBypassAndHeadshotTooltip(stack)) {
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

        if (shouldRenderBypassAndHeadshotTooltip(stack)) height += 10;
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
        if (Screen.hasShiftDown()) {
            int width = getMaxPerkDesWidth(font);
            return width == 0 ? Math.max(this.width, getDefaultMaxWidth(font)) : Math.max(width, getDefaultMaxWidth(font));
        } else {
            return getDefaultMaxWidth(font);
        }
    }

}
