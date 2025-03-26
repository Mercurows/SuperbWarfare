package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.GunsDataMessage;
import com.google.gson.stream.JsonReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid = ModUtils.MODID)
public class GunsTool {

    public static HashMap<String, HashMap<String, Double>> gunsData = new HashMap<>();

    /**
     * 初始化数据，从data中读取数据json文件
     */
    public static void initJsonData(ResourceManager manager) {
        for (var entry : manager.listResources("guns", file -> file.getPath().endsWith(".json")).entrySet()) {
            var id = entry.getKey();
            var attribute = entry.getValue();
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(attribute.open()));

                reader.beginObject();
                var map = new HashMap<String, Double>();
                while (reader.hasNext()) {
                    map.put(reader.nextName(), reader.nextDouble());
                }
                var path = id.getPath();
                gunsData.put(path.substring(5, path.length() - 5), map);

                reader.endObject();
                reader.close();
            } catch (Exception e) {
                ModUtils.LOGGER.error(e.getMessage());
            }
        }
    }

    public static void initGun(Level level, ItemStack stack, String location) {
        if (level.getServer() == null) return;
        gunsData.get(location).forEach((k, v) -> {
            CompoundTag tag = NBTTool.getOrCreateTag(stack);
            CompoundTag data = tag.getCompound("GunData");
            data.putDouble(k, v);
            tag.put("GunData", data);
        });
    }

    public static void initCreativeGun(ItemStack stack, String location) {
        if (gunsData != null && gunsData.get(location) != null) {
            gunsData.get(location).forEach((k, v) -> {
                CompoundTag tag = NBTTool.getOrCreateTag(stack);
                CompoundTag data = tag.getCompound("GunData");
                data.putDouble(k, v);
                tag.put("GunData", data);
            });
            GunsTool.setGunIntTag(stack, "Ammo", GunsTool.getGunIntTag(stack, "Magazine", 0)
                    + GunsTool.getGunIntTag(stack, "CustomMagazine", 0));
        }
    }

    public static void generateAndSetUUID(ItemStack stack) {
        UUID uuid = UUID.randomUUID();
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        data.putUUID("UUID", uuid);
        tag.put("GunData", data);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new GunsDataMessage(GunsTool.gunsData));
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        initJsonData(event.getServer().getResourceManager());
    }

    public static void reload(Player player, ItemStack stack, AmmoType type) {
        reload(player, stack, type, false);
    }

    public static void reload(Player player, ItemStack stack, AmmoType type, boolean extraOne) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        if (!(stack.getItem() instanceof GunItem)) return;

        int mag = GunsTool.getGunIntTag(stack, "Magazine", 0) + GunsTool.getGunIntTag(stack, "CustomMagazine", 0);
        int ammo = GunsTool.getGunIntTag(stack, "Ammo", 0);
        int ammoToAdd = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && GunsTool.getGunIntTag(stack, "BoltActionTime", 0) > 0 && !stack.is(ModTags.Items.REVOLVER)) {
            GunsTool.setGunBooleanTag(stack, "NeedBoltAction", false);
        }

        var capability = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        var playerAmmo = 0;

        if (capability != null) {
            playerAmmo = type.get(capability);
            var newAmmoCount = Math.max(0, playerAmmo - ammoToAdd);
            type.set(capability, newAmmoCount);
            capability.syncPlayerVariables(player);
        }

        int needToAdd = ammo + Math.min(ammoToAdd, playerAmmo);

        GunsTool.setGunIntTag(stack, "Ammo", needToAdd);
        tag.putBoolean("is_normal_reloading", false);
        tag.putBoolean("is_empty_reloading", false);
    }

    /* PerkData */
    public static void setPerkIntTag(ItemStack stack, String name, int num) {
        var rootTag = NBTTool.getOrCreateTag(stack);
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putInt(name, num);
        rootTag.put("PerkData", tag);
    }

    public static int getPerkIntTag(ItemStack stack, String name) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack).getCompound("PerkData");
        return tag.getInt(name);
    }

    public static void setPerkDoubleTag(ItemStack stack, String name, double num) {
        var rootTag = NBTTool.getOrCreateTag(stack);
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putDouble(name, num);
        rootTag.put("PerkData", tag);
    }

    public static double getPerkDoubleTag(ItemStack stack, String name) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack).getCompound("PerkData");
        return tag.getDouble(name);
    }

    public static void setPerkBooleanTag(ItemStack stack, String name, boolean flag) {
        var rootTag = NBTTool.getOrCreateTag(stack);
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putBoolean(name, flag);
        rootTag.put("PerkData", tag);
    }

    public static boolean getPerkBooleanTag(ItemStack stack, String name) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack).getCompound("PerkData");
        return tag.getBoolean(name);
    }

    /* Attachments */
    public static int getAttachmentType(ItemStack stack, AttachmentType type) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack).getCompound("Attachments");
        return tag.getInt(type.getName());
    }

    public enum AttachmentType {
        SCOPE("Scope"),
        MAGAZINE("Magazine"),
        BARREL("Barrel"),
        STOCK("Stock"),
        GRIP("Grip");

        private final String name;

        AttachmentType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /* GunData */
    public static CompoundTag getGunData(ItemStack stack) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        return tag.getCompound("GunData");
    }

    public static void setGunIntTag(ItemStack stack, String name, int num) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        data.putInt(name, num);
        tag.put("GunData", data);
    }

    public static int getGunIntTag(ItemStack stack, String name) {
        return getGunIntTag(stack, name, 0);
    }

    public static int getGunIntTag(ItemStack stack, String name, int defaultValue) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getInt(name);
    }

    public static void setGunDoubleTag(ItemStack stack, String name, double num) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        data.putDouble(name, num);
        tag.put("GunData", data);
    }

    public static double getGunDoubleTag(ItemStack stack, String name) {
        return getGunDoubleTag(stack, name, 0);
    }

    public static double getGunDoubleTag(ItemStack stack, String name, double defaultValue) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getDouble(name);
    }

    public static void setGunBooleanTag(ItemStack stack, String name, boolean flag) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        data.putBoolean(name, flag);
        tag.put("GunData", data);
    }

    public static boolean getGunBooleanTag(ItemStack stack, String name) {
        return getGunBooleanTag(stack, name, false);
    }

    public static boolean getGunBooleanTag(ItemStack stack, String name, boolean defaultValue) {
        CompoundTag tag = NBTTool.getOrCreateTag(stack);
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getBoolean(name);
    }

    @Nullable
    public static UUID getGunUUID(ItemStack stack) {
        CompoundTag tag = stack.get(ModDataComponents.GUN_DATA);
        if (tag == null || !tag.contains("GunData")) return null;

        CompoundTag data = tag.getCompound("GunData");
        if (!data.hasUUID("UUID")) return null;
        return data.getUUID("UUID");
    }
}
