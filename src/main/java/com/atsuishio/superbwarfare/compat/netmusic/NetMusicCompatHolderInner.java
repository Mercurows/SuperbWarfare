//package com.atsuishio.superbwarfare.compat.netmusic;
//
//import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
//import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
//import com.github.tartaricacid.netmusic.item.ItemMusicCD;
//import net.minecraft.Util;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//
//import java.util.concurrent.CompletableFuture;
//
//public class NetMusicCompatHolderInner {
//
//    public static boolean canPlayMusic(VehicleEntity vehicle) {
//        if (!(vehicle.getFirstPassenger() instanceof Player player)) return false;
//        var stack = player.getOffhandItem();
//        return canPlayMusic(stack);
//    }
//
//    public static boolean canPlayMusic(ItemStack stack) {
//        return ItemMusicCD.getSongInfo(stack) != null;
//    }
//
//    public static void playMusic(VehicleEntity vehicle) {
//        if (!(vehicle.getFirstPassenger() instanceof Player player)) return;
//        var stack = player.getOffhandItem();
//
//        var info = ItemMusicCD.getSongInfo(stack);
//        if (info == null) return;
//
//        CompletableFuture.runAsync(
//                () -> MusicPlayManager.play(info.songUrl, info.songName, url -> new SBWCompatSoundInstance(url, vehicle)),
//                Util.backgroundExecutor()
//        );
//    }
//}
