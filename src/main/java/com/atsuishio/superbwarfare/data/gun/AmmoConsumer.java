package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class AmmoConsumer implements DeserializeFromString {
    @SerializedName("Ammo")
    public String ammo;

    @ServerOnly
    @SerializedName("Projectile")
    public StringToObject<ProjectileInfo> projectile = null;

    public transient AmmoConsumeType type = AmmoConsumeType.INVALID;
    public transient int loadAmount = 1;

    public static final AmmoConsumer INVALID = new AmmoConsumer();

    private transient boolean initialized = false;
    private transient Ammo playerAmmoType;
    private transient ItemStack stack = ItemStack.EMPTY;

    public ItemStack stack() {
        return this.stack;
    }

    public boolean initialized() {
        return this.initialized;
    }

    public enum AmmoConsumeType {
        PLAYER_AMMO, ITEM, INVALID,
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    public int consume(@NotNull Entity shooter, int count) {
        if (count <= 0 || shooter instanceof Player player && player.isCreative()) return 0;
        if (!initialized) init();

        if (type == AmmoConsumeType.INVALID) {
            Mod.LOGGER.warn("consume ammo failed: invalid AmmoConsumeType");
            return 0;
        }

        if (type == AmmoConsumeType.PLAYER_AMMO) {
            if (shooter instanceof Player player) {
                if (playerAmmoType != null) {
                    playerAmmoType.add(player, -count);
                    return count;
                } else {
                    Mod.LOGGER.warn("consume player ammo failed: invalid player ammo type");
                }
            } else {
                Mod.LOGGER.warn("consume player ammo failed: invalid shooter");
            }
        }

        var handler = shooter.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler != null) {
            return consume(handler, count);
        } else {
            Mod.LOGGER.warn("consume ammo failed: invalid item handler for entity {}", shooter);
            return 0;
        }
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    public int consume(@NotNull IItemHandler handler, int count) {
        if (type == AmmoConsumeType.PLAYER_AMMO || type == AmmoConsumeType.INVALID || count <= 0) return 0;
        if (!initialized) init();

        return InventoryTool.consumeItem(handler, stack -> ItemStack.isSameItemSameComponents(stack, this.stack), count);
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    public int count(@Nullable Entity entity) {
        if (entity == null) return 0;
        if (!initialized) init();

        if (type == AmmoConsumeType.PLAYER_AMMO && entity instanceof Player player) {
            return playerAmmoType.get(player);
        }

        return count(entity.getCapability(Capabilities.ItemHandler.ENTITY));
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    public int count(@Nullable IItemHandler handler) {
        if (handler == null) return 0;
        if (!initialized) init();

        if (type == AmmoConsumeType.ITEM) {
            return InventoryTool.countItem(handler, stack -> ItemStack.isSameItemSameComponents(stack, this.stack));
        }

        return 0;
    }

    /**
     * 返还指定数量的弹药
     * <br>
     * 注：不会实际消耗枪内弹药
     * @return 成功返还的弹药数量
     */
    // TODO 正确处理多发弹药装填情况下的退弹
    public int withdraw(@NotNull Entity shooter, int count) {
        if (type == AmmoConsumeType.INVALID) {
            Mod.LOGGER.warn("withdraw ammo failed: invalid type");
            return 0;
        }
        if (!initialized) init();

        if (type == AmmoConsumeType.PLAYER_AMMO) {
            if (shooter instanceof Player player) {
                if (playerAmmoType != null) {
                    playerAmmoType.add(player, count);
                    return count;
                } else {
                    Mod.LOGGER.warn("withdraw player ammo failed: invalid player ammo type");
                }
            } else {
                Mod.LOGGER.warn("withdraw player ammo failed: invalid shooter");
            }
        } else {
            if (shooter instanceof Player player) {
                ItemHandlerHelper.giveItemToPlayer(player, this.stack.copyWithCount(count));
                return count;
            } else {
                var itemHandler = shooter.getCapability(Capabilities.ItemHandler.ENTITY);
                if (itemHandler != null) {
                    return withdraw(itemHandler, count);
                } else {
                    Mod.LOGGER.warn("withdraw ammo failed: invalid item handler");
                }
            }
        }
        return 0;
    }

    public int withdraw(@NotNull IItemHandler handler, int count) {
        if (!initialized) init();

        var copiedStack = this.stack.copyWithCount(count);
        var result = ItemHandlerHelper.insertItemStacked(handler, copiedStack, false);

        int inserted = count - result.getCount();
        if (!result.isEmpty()) {
            Mod.LOGGER.warn("trying to withdraw ammo {} with count {}, but only {} is inserted", copiedStack, count, inserted);
        }
        return inserted;
    }

    private static final Pattern AMMO_PATTERN = Pattern.compile("^(?<count>(\\d+ )?)(?<prefix>[@#]?)(?<id>\\w+(:\\w+)?)(?<data>(\\{.*})?)$");

    public void init() {
        this.type = AmmoConsumeType.INVALID;
        if (ammo == null) {
            Mod.LOGGER.warn("ammo value should not be null!");
            return;
        }

        var matcher = AMMO_PATTERN.matcher(ammo);
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid ammo value: {}", ammo);
            return;
        }

        var numStr = matcher.group("count").trim();
        this.loadAmount = Mth.clamp(numStr.isEmpty() ? 1 : Integer.parseInt(numStr), 1, Integer.MAX_VALUE);

        var prefix = matcher.group("prefix");
        var id = matcher.group("id");
        var data = matcher.group("data");

        if ("@".equals(prefix)) {
            this.playerAmmoType = Ammo.getType(id);
            if (this.playerAmmoType == null) {
                Mod.LOGGER.warn("invalid player ammo type: {}", id);
                return;
            }
            this.type = AmmoConsumeType.PLAYER_AMMO;
        } else {
            var location = ResourceLocation.tryParse(id);
            if (location == null) {
                Mod.LOGGER.warn("invalid item id: {}", id);
                return;
            }
            var item = BuiltInRegistries.ITEM.get(location);
            if (item == Items.AIR) {
                Mod.LOGGER.warn("invalid item: {}", id);
                return;
            }

            this.stack = new ItemStack(item);
            if (!data.isEmpty()) {
                try {
                    var tag = NbtUtils.snbtToStructure(data);
                    tag.putString("id", location.toString());
                    tag.putInt("count", 1);
                    ItemStack.parse(RegistryAccess.EMPTY, tag).ifPresent(stack -> this.stack = stack);
                } catch (CommandSyntaxException exception) {
                    Mod.LOGGER.warn("invalid item data {}: {}", data, exception.getMessage());
                    return;
                }
            }

            this.type = AmmoConsumeType.ITEM;
        }

        this.initialized = true;
    }

    @Override
    public void deserializeFromString(String str) {
        this.ammo = str;
        init();
    }

    public Ammo getPlayerAmmoType() {
        return playerAmmoType;
    }
}
