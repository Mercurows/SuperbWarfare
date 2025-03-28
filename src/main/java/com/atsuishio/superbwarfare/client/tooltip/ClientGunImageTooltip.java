package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ClientGunImageTooltip implements ClientTooltipComponent {

    protected final int width;
    protected final int height;
    protected final ItemStack stack;

    public ClientGunImageTooltip(GunImageComponent tooltip) {
        this.width = tooltip.width;
        this.height = tooltip.height;
        this.stack = tooltip.stack;
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
        return GunsTool.getGunDoubleTag(stack, "BypassesArmor", 0) > 0 || GunsTool.getGunDoubleTag(stack, "Headshot", 0) > 0;
    }

    protected boolean shouldRenderEditTooltip() {
        if (this.stack.getItem() instanceof GunItem gunItem) {
            return gunItem.isCustomizable(stack);
        }
        return false;
    }

    protected boolean shouldRenderPerks() {
        return PerkHelper.getPerkByType(stack, Perk.Type.AMMO) != null || PerkHelper.getPerkByType(stack, Perk.Type.DAMAGE) != null || PerkHelper.getPerkByType(stack, Perk.Type.FUNCTIONAL) != null;
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
        double damage = GunsTool.getGunDoubleTag(stack, "Damage", 0) * TooltipTool.perkDamage(stack);
        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(damage) + (TooltipTool.heBullet(stack) ? " + "
                        + FormatTool.format1D(0.8 * damage * (1 + 0.1 * TooltipTool.heBulletLevel(stack))) : "")).withStyle(ChatFormatting.GREEN));
    }

    /**
     * 获取武器射速的文本组件
     */
    protected Component getRpmComponent() {
        if (this.stack.getItem() instanceof GunItem gunItem && gunItem.isAutoWeapon(this.stack)) {
            return Component.translatable("des.superbwarfare.guns.rpm").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format0D(GunsTool.getGunIntTag(stack, "RPM", 0)))
                            .withStyle(ChatFormatting.GREEN));
        }
        return Component.literal("");
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
        int level = GunsTool.getGunIntTag(stack, "Level", 0);
        double rate = GunsTool.getGunDoubleTag(stack, "Exp", 0) / (20 * Math.pow(level, 2) + 160 * level + 20);

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
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
                .append(Component.literal(level + "").withStyle(formatting).withStyle(ChatFormatting.BOLD))
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
                .append(Component.literal(" (" + FormatTool.DECIMAL_FORMAT_2ZZZ.format(rate * 100) + "%)").withStyle(ChatFormatting.GRAY));
    }

    /**
     * 获取武器强化点数文本组件
     */
    protected Component getUpgradePointComponent() {
        int upgradePoint = Mth.floor(GunsTool.getGunDoubleTag(stack, "UpgradePoint", 0));
        return Component.translatable("des.superbwarfare.guns.upgrade_point").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
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
        var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);

        if (perk instanceof AmmoPerk ammoPerk) {
            int level = PerkHelper.getItemPerkLevel(perk, stack);
            perkBypassArmorRate = ammoPerk.bypassArmorRate + (perk == ModPerks.AP_BULLET.get() ? 0.05f * (level - 1) : 0);
        }
        double bypassRate = Math.max(GunsTool.getGunDoubleTag(stack, "BypassesArmor", 0) + perkBypassArmorRate, 0);

        return Component.translatable("des.superbwarfare.guns.bypass").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format2D(bypassRate * 100, "%")).withStyle(ChatFormatting.GOLD));
    }

    /**
     * 获取武器爆头倍率文本组件
     */
    protected Component getHeadshotComponent() {
        double headshot = GunsTool.getGunDoubleTag(stack, "Headshot", 0);
        return Component.translatable("des.superbwarfare.guns.headshot").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
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

        Perk ammoPerk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
        if (ammoPerk != null && PerkHelper.getPerkItem(ammoPerk).isPresent()) {
            xOffset += 20;

            var ammoItem = PerkHelper.getPerkItem(ammoPerk).get().get();
            ItemStack perkStack = ammoItem.getDefaultInstance();

            CompoundTag ammoTag = PerkHelper.getPerkTag(stack, Perk.Type.AMMO);
            if (!ammoTag.isEmpty()) {
                int level = PerkHelper.getItemPerkLevel(ammoPerk, stack);
                perkStack.setCount(level);
            }
            guiGraphics.renderItem(perkStack, x + xOffset, y + 2);
            guiGraphics.renderItemDecorations(font, perkStack, x + xOffset, y + 2);
        }

        Perk funcPerk = PerkHelper.getPerkByType(stack, Perk.Type.FUNCTIONAL);
        if (funcPerk != null && PerkHelper.getPerkItem(funcPerk).isPresent()) {
            xOffset += 20;

            var funcItem = PerkHelper.getPerkItem(funcPerk).get().get();
            ItemStack perkStack = funcItem.getDefaultInstance();

            CompoundTag funcTag = PerkHelper.getPerkTag(stack, Perk.Type.FUNCTIONAL);
            if (!funcTag.isEmpty()) {
                int level = PerkHelper.getItemPerkLevel(funcPerk, stack);
                perkStack.setCount(level);
            }

            guiGraphics.renderItem(perkStack, x + xOffset, y + 2);
            guiGraphics.renderItemDecorations(font, perkStack, x + xOffset, y + 2);
        }

        Perk damagePerk = PerkHelper.getPerkByType(stack, Perk.Type.DAMAGE);
        if (damagePerk != null && PerkHelper.getPerkItem(damagePerk).isPresent()) {
            xOffset += 20;

            var damageItem = PerkHelper.getPerkItem(damagePerk).get().get();
            ItemStack perkStack = damageItem.getDefaultInstance();

            CompoundTag damageTag = PerkHelper.getPerkTag(stack, Perk.Type.DAMAGE);
            if (!damageTag.isEmpty()) {
                int level = PerkHelper.getItemPerkLevel(damagePerk, stack);
                perkStack.setCount(level);
            }

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

        Perk ammoPerk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
        if (ammoPerk != null && PerkHelper.getPerkItem(ammoPerk).isPresent()) {
            yOffset += 25;
            var ammoItem = PerkHelper.getPerkItem(ammoPerk).get().get();
            guiGraphics.renderItem(ammoItem.getDefaultInstance(), x, y + 4 + yOffset);

            CompoundTag ammoTag = PerkHelper.getPerkTag(stack, Perk.Type.AMMO);
            if (!ammoTag.isEmpty()) {
                var ids = ammoTag.getString("id").split(":");
                if (ids.length > 1) {
                    String id = ids[1];
                    var ammoComponent = Component.translatable("item.superbwarfare." + id).withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                            .append(Component.literal(" Lvl. " + ammoTag.getInt("level")).withStyle(ChatFormatting.WHITE));
                    var ammoDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);

                    guiGraphics.drawString(font, ammoComponent, x + 20, y + yOffset + 2, 0xFFFFFF);
                    guiGraphics.drawString(font, ammoDesComponent, x + 20, y + yOffset + 12, 0xFFFFFF);
                }
            }
        }

        Perk funcPerk = PerkHelper.getPerkByType(stack, Perk.Type.FUNCTIONAL);
        if (funcPerk != null && PerkHelper.getPerkItem(funcPerk).isPresent()) {
            yOffset += 25;
            var funcItem = PerkHelper.getPerkItem(funcPerk).get().get();
            guiGraphics.renderItem(funcItem.getDefaultInstance(), x, y + 4 + yOffset);

            CompoundTag funcTag = PerkHelper.getPerkTag(stack, Perk.Type.FUNCTIONAL);
            if (!funcTag.isEmpty()) {
                var ids = funcTag.getString("id").split(":");
                if (ids.length > 1) {
                    String id = ids[1];
                    var funcComponent = Component.translatable("item.superbwarfare." + id).withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                            .append(Component.literal(" Lvl. " + funcTag.getInt("level")).withStyle(ChatFormatting.WHITE));
                    var funcDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);

                    guiGraphics.drawString(font, funcComponent, x + 20, y + yOffset + 2, 0xFFFFFF);
                    guiGraphics.drawString(font, funcDesComponent, x + 20, y + yOffset + 12, 0xFFFFFF);
                }
            }
        }

        Perk damagePerk = PerkHelper.getPerkByType(stack, Perk.Type.DAMAGE);
        if (damagePerk != null && PerkHelper.getPerkItem(damagePerk).isPresent()) {
            yOffset += 25;
            var damageItem = PerkHelper.getPerkItem(damagePerk).get().get();
            guiGraphics.renderItem(damageItem.getDefaultInstance(), x, y + 4 + yOffset);

            CompoundTag damageTag = PerkHelper.getPerkTag(stack, Perk.Type.DAMAGE);
            if (!damageTag.isEmpty()) {
                var ids = damageTag.getString("id").split(":");
                if (ids.length > 1) {
                    String id = ids[1];
                    var damageComponent = Component.translatable("item.superbwarfare." + id).withStyle(ChatFormatting.RED)
                            .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                            .append(Component.literal(" Lvl. " + damageTag.getInt("level")).withStyle(ChatFormatting.WHITE));
                    var damageDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);

                    guiGraphics.drawString(font, damageComponent, x + 20, y + yOffset + 2, 0xFFFFFF);
                    guiGraphics.drawString(font, damageDesComponent, x + 20, y + yOffset + 12, 0xFFFFFF);
                }
            }
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

        CompoundTag ammoTag = PerkHelper.getPerkTag(stack, Perk.Type.AMMO);
        if (!ammoTag.isEmpty()) {
            var ids = ammoTag.getString("id").split(":");
            if (ids.length > 1) {
                String id = ids[1];
                var ammoDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);
                width = Math.max(width, font.width(ammoDesComponent));
            }
        }

        CompoundTag funcTag = PerkHelper.getPerkTag(stack, Perk.Type.FUNCTIONAL);
        if (!funcTag.isEmpty()) {
            var ids = funcTag.getString("id").split(":");
            if (ids.length > 1) {
                String id = ids[1];
                var funcDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);
                width = Math.max(width, font.width(funcDesComponent));
            }
        }

        CompoundTag damageTag = PerkHelper.getPerkTag(stack, Perk.Type.DAMAGE);
        if (!damageTag.isEmpty()) {
            var ids = damageTag.getString("id").split(":");
            if (ids.length > 1) {
                String id = ids[1];
                var damageDesComponent = Component.translatable("des.superbwarfare." + id).withStyle(ChatFormatting.GRAY);
                width = Math.max(width, font.width(damageDesComponent));
            }
        }

        return width + 25;
    }

    @Override
    public int getHeight() {
        int height = Math.max(20, this.height);

        if (shouldRenderBypassAndHeadshotTooltip()) height += 10;
        if (shouldRenderEditTooltip()) height += 20;
        if (shouldRenderPerks()) {
            if (!Screen.hasShiftDown()) {
                height += 16;
            } else {
                height += 16;
                if (PerkHelper.getPerkByType(stack, Perk.Type.AMMO) != null) {
                    height += 25;
                }
                if (PerkHelper.getPerkByType(stack, Perk.Type.FUNCTIONAL) != null) {
                    height += 25;
                }
                if (PerkHelper.getPerkByType(stack, Perk.Type.DAMAGE) != null) {
                    height += 25;
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
