//package com.atsuishio.superbwarfare.compat.netmusic;
//
//import com.atsuishio.superbwarfare.compat.CompatHolder;
//import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
//import net.minecraft.world.item.ItemStack;
//import net.minecraftforge.fml.ModList;
//
//public class NetMusicCompatHolder {
//
//    public static boolean hasMod() {
//        return ModList.get().isLoaded(CompatHolder.NET_MUSIC);
//    }
//
//    public static boolean canPlayMusic(VehicleEntity vehicle) {
//        if (hasMod()) {
//            return NetMusicCompatHolderInner.canPlayMusic(vehicle);
//        } else {
//            return false;
//        }
//    }
//
//    public static boolean canPlayMusic(ItemStack stack) {
//        if (hasMod()) {
//            return NetMusicCompatHolderInner.canPlayMusic(stack);
//        } else {
//            return false;
//        }
//    }
//
//    public static void playMusic(VehicleEntity vehicle) {
//        NetMusicCompatHolderInner.playMusic(vehicle);
//    }
//}
