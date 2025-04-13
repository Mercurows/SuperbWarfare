package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.DefaultGunData;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.ReloadState;
import com.atsuishio.superbwarfare.network.message.receive.GunsDataMessage;
import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid = Mod.MODID)
public class GunsTool {

    public static HashMap<String, DefaultGunData> gunsData = new HashMap<>();

    /**
     * 初始化数据，从data中读取数据json文件
     */
    public static void initJsonData(ResourceManager manager) {
        for (var entry : manager.listResources("guns", file -> file.getPath().endsWith(".json")).entrySet()) {
            var id = entry.getKey();
            var attribute = entry.getValue();
            try {
                Gson gson = new Gson();
                var data = gson.fromJson(new InputStreamReader(attribute.open()), DefaultGunData.class);
                var path = id.getPath();

                gunsData.put(path.substring(5, path.length() - 5), data);
            } catch (Exception e) {
                Mod.LOGGER.error(e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, GunsDataMessage.create());
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        initJsonData(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        initJsonData(event.getPlayerList().getServer().getResourceManager());

        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, GunsDataMessage.create()));
    }

    public static void reload(Player player, ItemStack stack, GunData gunData, AmmoType type) {
        reload(player, stack, gunData, type, false);
    }

    public static void reload(Player player, ItemStack stack, GunData data, AmmoType type, boolean extraOne) {
        int mag = data.magazine();
        int ammo = data.ammo.get();
        int ammoToAdd = mag - ammo + (extraOne ? 1 : 0);

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && data.defaultActionTime() > 0 && !stack.is(ModTags.Items.REVOLVER)) {
            data.bolt.needed.set(false);
        }

        var capability = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
        var playerAmmo = 0;

        playerAmmo = type.get(capability);
        var newAmmoCount = Math.max(0, playerAmmo - ammoToAdd);
        type.set(capability, newAmmoCount);
        player.setData(ModAttachments.PLAYER_VARIABLE, capability);
        capability.sync(player);

        int needToAdd = ammo + Math.min(ammoToAdd, playerAmmo);

        data.ammo.set(needToAdd);
        data.reload.setState(ReloadState.NOT_RELOADING);
    }

    /* PerkData */
    public static void setPerkIntTag(final CompoundTag rootTag, String name, int num) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        if (!tag.contains(name) && num == 0) return;
        tag.putInt(name, num);
        rootTag.put("PerkData", tag);
    }

    public static int getPerkIntTag(final CompoundTag rootTag, String name) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        return tag.getInt(name);
    }

    public static void setPerkBooleanTag(final CompoundTag rootTag, String name, boolean flag) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        if (!tag.contains(name) && !flag) return;
        tag.putBoolean(name, flag);
        rootTag.put("PerkData", tag);
    }

    public static boolean getPerkBooleanTag(final CompoundTag rootTag, String name) {
        CompoundTag tag = rootTag.getCompound("PerkData");
        return tag.getBoolean(name);
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

    public static double getGunDoubleTag(final CompoundTag tag, String name) {
        return getGunDoubleTag(tag, name, 0);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name, double defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getDouble(name);
    }
    @Nullable
    public static UUID getGunUUID(final CompoundTag tag) {
        if (!tag.contains("GunData")) return null;

        CompoundTag data = tag.getCompound("GunData");
        if (!data.hasUUID("UUID")) return null;
        return data.getUUID("UUID");
    }
}
