package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
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

    @SerializedName("Icon")
    public String icon = Mod.loc("textures/overlay/vehicle/weapon/icons/empty.png").toString();

    @SerializedName("ShouldUnload")
    public boolean shouldUnload = true;

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
        return ItemStack.isSameItemSameTags(stack, this.stack);
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
            }
        }

        if (type == AmmoConsumeType.ENERGY) {
            int finalCount = count;
            return data.getEnergyProvider(shooter)
                    .map(cap -> cap.extractEnergy(finalCount, false))
                    .orElse(0);
        }

        var handler = shooter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
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
            return data.stack.getCapability(ForgeCapabilities.ENERGY)
                    .map(cap -> cap.extractEnergy(count, false))
                    .orElse(0);
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
        } else if (type == AmmoConsumeType.ENERGY) {
            return data.getEnergyProvider(entity)
                    .map(IEnergyStorage::getEnergyStored)
                    .orElse(0);
        }

        return playerAmmoCount + count(data, entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null));
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
            return data.stack.getCapability(ForgeCapabilities.ENERGY)
                    .map(IEnergyStorage::getEnergyStored)
                    .orElse(0);
        }

        return InventoryTool.countAmmoItem(handler, this.playerAmmoType);
    }

    /**
     * 返还指定数量的弹药
     * <br>
     * 注：不会实际消耗枪内弹药
     *
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
                    var countToWithdraw = Math.min(count, playerAmmoType.getLimit() - playerAmmoType.get(player));
                    playerAmmoType.add(player, countToWithdraw);

                    var restItemCount = count - countToWithdraw;
                    if (restItemCount > 0) {
                        InventoryTool.insertItem(player, playerAmmoType.getItemStack(), restItemCount);
                    }

                    return count;
                } else {
                    Mod.LOGGER.warn("withdraw player ammo failed: invalid player ammo type");
                }
            } else {
                var itemHandler = ammoSupplier.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
                if (itemHandler != null) {
                    return withdraw(itemHandler, count);
                } else {
                    Mod.LOGGER.warn("withdraw ammo failed: invalid item handler");
                }
            }
        } else {
            if (ammoSupplier instanceof Player player) {
                InventoryTool.insertItem(player, this.stack, count);
                return count;
            } else {
                var itemHandler = ammoSupplier.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
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

        return InventoryTool.insertItem(handler, stackToInsert, count);
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
            this.stack = this.playerAmmoType.getItemStack();
        } else {
            // Item
            var location = ResourceLocation.tryParse(id);
            if (location == null) {
                Mod.LOGGER.warn("invalid item id: {}", id);
                return;
            }
            var item = ForgeRegistries.ITEMS.getValue(location);
            if (item == null || item == Items.AIR) {
                Mod.LOGGER.warn("invalid item: {}", id);
                return;
            }

            this.stack = new ItemStack(item);
            if (!data.isEmpty()) {
                try {
                    var tag = new CompoundTag();
                    tag.put("tag", TagParser.parseTag(data));
                    tag.putString("id", location.toString());
                    tag.putInt("Count", 1);
                    this.stack = ItemStack.of(tag);
                } catch (Exception exception) {
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
