package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.google.gson.JsonObject;
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

import java.util.Locale;
import java.util.regex.Pattern;

public class AmmoConsumer implements DeserializeFromString, GunPropertyModifier {
    @SerializedName("Ammo")
    public String ammo;

    @SerializedName("AmmoSlot")
    public String ammoSlot = "Default";

    @ServerOnly
    @SerializedName("Projectile")
    public StringToObject<ProjectileInfo> projectile = null;

    @SerializedName("Override")
    public JsonObject override = null;

    // TODO 这玩意有点抽象，看看能不能优化掉
    @SerializedName("Icon")
    public String icon = Mod.loc("textures/screens/vehicle_weapon/empty.png").toString();

    public transient AmmoConsumeType type = AmmoConsumeType.EMPTY;
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

    // TODO 整合弹药处理
    public enum AmmoConsumeType {
        INVALID,
        EMPTY,
        INFINITE,

        PLAYER_AMMO,
        ITEM,
        ENERGY,
    }

    public boolean isAmmoItem(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, this.stack);
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    public int consume(@NotNull GunData data, @NotNull Entity shooter, int count) {
        if (!initialized) init();
        if (count <= 0
                || this.type == AmmoConsumeType.INFINITE
                || shooter instanceof Player player && player.isCreative()
        ) return 0;

        if (type == AmmoConsumeType.INVALID) {
            Mod.LOGGER.warn("consume ammo failed: invalid AmmoConsumeType");
            return 0;
        }

        int consumed = 0;
        if (type == AmmoConsumeType.PLAYER_AMMO) {
            if (shooter instanceof Player player) {
                if (playerAmmoType != null) {
                    var current = playerAmmoType.get(player);
                    consumed = Math.min(current, count);
                    count -= consumed;
                    playerAmmoType.add(player, -consumed);
                } else {
                    Mod.LOGGER.warn("consume player ammo failed: invalid player ammo type");
                }
            } else {
                Mod.LOGGER.warn("consume player ammo failed: invalid shooter");
            }
        }

        if (type == AmmoConsumeType.ENERGY) {
            var energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyStorage == null) {
                return 0;
            }
            return energyStorage.extractEnergy(count, false);
        }

        var handler = shooter.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler != null) {
            return consumed + consume(data, handler, count);
        } else {
            Mod.LOGGER.warn("consume ammo failed: invalid item handler for entity {}", shooter);
            return consumed;
        }
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    public int consume(@NotNull GunData data, @NotNull IItemHandler handler, int count) {
        if (!initialized) init();
        if (type == AmmoConsumeType.INVALID
                || type == AmmoConsumeType.INFINITE
                || type == AmmoConsumeType.EMPTY
                || count <= 0
        ) return 0;

        if (type == AmmoConsumeType.PLAYER_AMMO) {
            var consumed = InventoryTool.consumeAmmoItem(handler, this.playerAmmoType, count);
            var rest = consumed - count;
            data.virtualAmmo.add(rest);
            return count;
        } else if (type == AmmoConsumeType.ENERGY) {
            var energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyStorage == null) {
                return 0;
            }
            return energyStorage.extractEnergy(count, false);
        } else {
            return InventoryTool.consumeItem(handler, this::isAmmoItem, count);
        }
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    public int count(@NotNull GunData data, @Nullable Entity entity) {
        if (!initialized) init();
        if (this.type == AmmoConsumeType.INFINITE) return Integer.MAX_VALUE;
        if (entity == null || type == AmmoConsumeType.EMPTY) return 0;

        int playerAmmoCount = 0;
        if (type == AmmoConsumeType.PLAYER_AMMO && entity instanceof Player player) {
            playerAmmoCount = playerAmmoType.get(player);
        }

        return playerAmmoCount + count(data, entity.getCapability(Capabilities.ItemHandler.ENTITY));
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    public int count(@NotNull GunData data, @Nullable IItemHandler handler) {
        if (!initialized) init();
        if (this.type == AmmoConsumeType.INFINITE) return Integer.MAX_VALUE;
        if (handler == null || type == AmmoConsumeType.EMPTY) return 0;

        if (type == AmmoConsumeType.ITEM) {
            return InventoryTool.countItem(handler, this::isAmmoItem);
        } else if (type == AmmoConsumeType.ENERGY) {
            var energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyStorage == null) {
                return 0;
            }
            return energyStorage.getEnergyStored();
        }

        return InventoryTool.countAmmoItem(handler, this.playerAmmoType);
    }

    /**
     * 返还指定数量的弹药
     * <br>
     * 注：不会实际消耗枪内弹药
     * @return 成功返还的弹药数量
     */
    public int withdraw(@NotNull Entity ammoSupplier, int count) {
        if (!initialized) init();
        if (type == AmmoConsumeType.INVALID
                || type == AmmoConsumeType.INFINITE
                || type == AmmoConsumeType.EMPTY
                || type == AmmoConsumeType.ENERGY
                || count <= 0
        ) {
            return 0;
        }

        if (type == AmmoConsumeType.PLAYER_AMMO) {
            if (ammoSupplier instanceof Player player) {
                if (playerAmmoType != null) {
                    playerAmmoType.add(player, count);
                    return count;
                } else {
                    Mod.LOGGER.warn("withdraw player ammo failed: invalid player ammo type");
                }
            } else {
                var itemHandler = ammoSupplier.getCapability(Capabilities.ItemHandler.ENTITY);
                if (itemHandler != null) {
                    return withdraw(itemHandler, count);
                } else {
                    Mod.LOGGER.warn("withdraw ammo failed: invalid item handler");
                }
            }
        } else {
            if (ammoSupplier instanceof Player player) {
                var limit = this.stack.getMaxStackSize();
                while (count > 0) {
                    var toInsert = Math.min(limit, count);
                    ItemHandlerHelper.giveItemToPlayer(player, this.stack.copyWithCount(toInsert));
                    count -= toInsert;
                }
                return count;
            } else {
                var itemHandler = ammoSupplier.getCapability(Capabilities.ItemHandler.ENTITY);
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
        if (type == AmmoConsumeType.INVALID
                || type == AmmoConsumeType.INFINITE
                || type == AmmoConsumeType.EMPTY
                || type == AmmoConsumeType.ENERGY
                || count <= 0
        ) {
            return 0;
        }

        ItemStack stackToInsert;
        if (type == AmmoConsumeType.PLAYER_AMMO) {
            stackToInsert = getPlayerAmmoType().getItemStack();
        } else {
            stackToInsert = this.stack;
        }

        int inserted = 0;
        while (count > 0) {
            var limit = stackToInsert.getMaxStackSize();
            var toInsert = Math.min(limit, count);
            var result = ItemHandlerHelper.insertItemStacked(handler, stackToInsert.copyWithCount(toInsert), false);

            count -= toInsert - result.getCount();
            inserted += toInsert - result.getCount();

            if (!result.isEmpty()) {
                Mod.LOGGER.warn("trying to withdraw ammo {} with count {}, but only {} is inserted", stackToInsert, count, inserted);
            }
        }

        return inserted;
    }

    private static final Pattern AMMO_PATTERN = Pattern.compile("^(?<count>(\\d+)?)\\s*(?<prefix>[@#]?)(?<id>\\w+(:\\w+)?)\\s*(?<data>(\\{.*})?)$");

    private final transient JsonPropertyModifier<GunData, DefaultGunData> jsonPropModifier = new JsonPropertyModifier<>();

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        if (this.projectile != null) {
            rawData.projectile = projectile;
        }

        if (override != null) {
            jsonPropModifier.update(override);
            rawData = jsonPropModifier.computeProperties(gunData, rawData);
        }

        return rawData;
    }

    @SuppressWarnings("invalid")
    public void init() {

        if (ammo == null) return;

        var matcher = AMMO_PATTERN.matcher(ammo.trim());
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid ammo value: {}", ammo);
            return;
        }

        var numStr = matcher.group("count").trim();
        this.loadAmount = Mth.clamp(numStr.isEmpty() ? 1 : Integer.parseInt(numStr), 1, Integer.MAX_VALUE);

        var prefix = matcher.group("prefix");
        var id = matcher.group("id");
        var data = matcher.group("data");

        if (prefix.isBlank()) {
            this.type = switch (id.toLowerCase(Locale.ROOT)) {
                case "infinity", "infinite" -> AmmoConsumeType.INFINITE;
                case "empty" -> AmmoConsumeType.EMPTY;
                case "fe", "rf", "energy" -> AmmoConsumeType.ENERGY;
                default -> AmmoConsumeType.INVALID;
            };

            if (this.type != AmmoConsumeType.INVALID) return;
        }

        // Player Ammo
        if ("@".equals(prefix)) {
            this.playerAmmoType = Ammo.getType(id);
            if (this.playerAmmoType == null) {
                Mod.LOGGER.warn("invalid player ammo type: {}", id);
                return;
            }
            this.type = AmmoConsumeType.PLAYER_AMMO;
        } else {
            // Item
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
