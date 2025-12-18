//package com.atsuishio.superbwarfare.compat.netmusic;
//
//import com.atsuishio.superbwarfare.Mod;
//import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
//import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
//import com.github.tartaricacid.netmusic.init.InitSounds;
//import net.minecraft.Util;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
//import net.minecraft.client.resources.sounds.Sound;
//import net.minecraft.client.sounds.AudioStream;
//import net.minecraft.client.sounds.SoundBufferLibrary;
//import net.minecraft.sounds.SoundSource;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.api.distmarker.OnlyIn;
//
//import javax.sound.sampled.UnsupportedAudioFileException;
//import java.io.IOException;
//import java.net.URL;
//import java.util.concurrent.CompletableFuture;
//
//@OnlyIn(Dist.CLIENT)
//public class SBWCompatSoundInstance extends AbstractTickableSoundInstance {
//
//    private final VehicleEntity entity;
//    private double lastDistance;
//    private int fade = 0;
//    private boolean die = false;
//
//    private final URL songUrl;
//
//    public SBWCompatSoundInstance(URL songUrl, VehicleEntity entity) {
//        super(InitSounds.NET_MUSIC.get(), SoundSource.RECORDS, entity.getCommandSenderWorld().getRandom());
//        this.entity = entity;
//        this.looping = true;
//        this.delay = 0;
//        this.songUrl = songUrl;
//    }
//
//    @Override
//    public void tick() {
//        var mc = Minecraft.getInstance();
//        var player = mc.player;
//        if (entity.isRemoved() || player == null) {
//            this.stop();
//            return;
//        } else if (!this.canPlay(entity)) {
//            this.die = true;
//        }
//
//        if (this.die) {
//            if (this.fade > 0) this.fade--;
//            else if (this.fade == 0) {
//                this.stop();
//                return;
//            }
//        } else if (this.fade < 3) {
//            this.fade++;
//        }
//
//        this.volume = this.getVolume(this.entity) * fade;
//
//        this.x = this.entity.getX();
//        this.y = this.entity.getY();
//        this.z = this.entity.getZ();
//
//        this.pitch = this.getPitch(this.entity);
//
//        if (player.getVehicle() != this.entity) {
//            double distance = this.entity.position().subtract(player.position()).length();
//            this.pitch += (float) (0.08 * Math.atan(lastDistance - distance));
//
//            this.lastDistance = distance;
//        } else {
//            this.lastDistance = 0;
//        }
//    }
//
//    protected boolean canPlay(VehicleEntity entity) {
//        return entity.inCarMusicPlaying();
//    }
//
//    protected float getPitch(VehicleEntity entity) {
//        return 1;
//    }
//
//    protected float getVolume(VehicleEntity entity) {
//        return 1;
//    }
//
//    @Override
//    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                return new NetMusicAudioStream(this.songUrl);
//            } catch (IOException | UnsupportedAudioFileException e) {
//                Mod.LOGGER.warn(e);
//            }
//            return null;
//        }, Util.backgroundExecutor());
//    }
//}
