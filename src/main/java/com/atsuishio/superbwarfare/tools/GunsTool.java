package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.message.receive.GunsDataMessage;
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

@EventBusSubscriber(modid = Mod.MODID)
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
                Mod.LOGGER.error(e.getMessage());
            }
        }
    }

    public static void initGun(Level level, final CompoundTag tag, String location) {
        if (level.getServer() == null) return;
        gunsData.get(location).forEach((k, v) -> {
            CompoundTag data = tag.getCompound("GunData");
            data.putDouble(k, v);
            tag.put("GunData", data);
        });
    }

    public static void initCreativeGun(ItemStack stack, String location) {
        if (gunsData != null && gunsData.get(location) != null) {
            final var tag = NBTTool.getTag(stack);
            gunsData.get(location).forEach((k, v) -> {
                CompoundTag data = tag.getCompound("GunData");
                data.putDouble(k, v);
                tag.put("GunData", data);
            });
            var data = tag.getCompound("GunData");
            data.putInt("Ammo", data.getInt("Magazine") + data.getInt("CustomMagazine"));
            NBTTool.saveTag(stack, tag);
        }
    }

    public static void generateAndSetUUID(final CompoundTag tag) {
        UUID uuid = UUID.randomUUID();
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

    public static void reload(Player player, ItemStack stack, final CompoundTag tag, AmmoType type) {
        reload(player, stack, tag, type, false);
    }

    public static void reload(Player player, ItemStack stack, final CompoundTag tag, AmmoType type, boolean extraOne) {
        final var data = tag.getCompound("GunData");

        int mag = data.getInt("Magazine") + data.getInt("CustomMagazine");
        int ammo = data.getInt("Ammo");
        int ammoToAdd = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && data.getInt("BoltActionTime") > 0 && !stack.is(ModTags.Items.REVOLVER)) {
            data.putBoolean("NeedBoltAction", false);
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

        data.putInt("Ammo", needToAdd);
        tag.putBoolean("is_normal_reloading", false);
        tag.putBoolean("is_empty_reloading", false);
    }

    /* PerkData */
    public static void setPerkIntTag(final CompoundTag rootTag, String name, int num) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putInt(name, num);
        rootTag.put("PerkData", tag);
    }

    public static int getPerkIntTag(final CompoundTag rootTag, String name) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        return tag.getInt(name);
    }

    public static void setPerkDoubleTag(final CompoundTag rootTag, String name, double num) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putDouble(name, num);
        rootTag.put("PerkData", tag);
    }

    public static double getPerkDoubleTag(final CompoundTag rootTag, String name) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        return tag.getDouble(name);
    }

    public static void setPerkBooleanTag(final CompoundTag rootTag, String name, boolean flag) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        tag.putBoolean(name, flag);
        rootTag.put("PerkData", tag);
    }

    public static boolean getPerkBooleanTag(final CompoundTag rootTag, String name) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        return tag.getBoolean(name);
    }

    /* Attachments */
    public static int getAttachmentType(final CompoundTag rootTag, AttachmentType type) {
        CompoundTag tag = rootTag.getCompound("Attachments");
        return tag.getInt(type.getName());
    }

    public static int getAttachmentType(ItemStack stack, AttachmentType type) {
        return getAttachmentType(NBTTool.getTag(stack), type);
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
    public static CompoundTag getGunData(final CompoundTag tag) {
        return tag.getCompound("GunData");
    }

    public static void setGunIntTag(final CompoundTag tag, String name, int num) {
        var data = tag.getCompound("GunData");
        data.putInt(name, num);
        tag.put("GunData", data);
    }

    public static int getGunIntTag(final CompoundTag tag, String name) {
        return getGunIntTag(tag, name, 0);
    }

    public static int getGunIntTag(final CompoundTag tag, String name, int defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getInt(name);
    }

    public static void setGunDoubleTag(final CompoundTag tag, String name, double num) {
        var data = tag.getCompound("GunData");
        data.putDouble(name, num);
        tag.put("GunData", data);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name) {
        return getGunDoubleTag(tag, name, 0);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name, double defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getDouble(name);
    }

    public static void setGunBooleanTag(final CompoundTag tag, String name, boolean flag) {
        var data = tag.getCompound("GunData");
        data.putBoolean(name, flag);
        tag.put("GunData", data);
    }

    public static boolean getGunBooleanTag(final CompoundTag tag, String name) {
        return getGunBooleanTag(tag, name, false);
    }

    public static boolean getGunBooleanTag(final CompoundTag tag, String name, boolean defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getBoolean(name);
    }

    @Nullable
    public static UUID getGunUUID(final CompoundTag tag) {
        if (!tag.contains("GunData")) return null;

        CompoundTag data = tag.getCompound("GunData");
        if (!data.hasUUID("UUID")) return null;
        return data.getUUID("UUID");
    }
}
