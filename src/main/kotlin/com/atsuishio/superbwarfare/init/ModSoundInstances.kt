package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.sound.FastProjectileSoundInstance
import com.atsuishio.superbwarfare.client.sound.HornSoundInstance
import com.atsuishio.superbwarfare.client.sound.StagedVehicleEngineSound
import com.atsuishio.superbwarfare.client.sound.SteelCoilMoveSoundInstance
import com.atsuishio.superbwarfare.client.sound.VehicleFireSoundInstance
import com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance
import com.atsuishio.superbwarfare.entity.living.SteelCoilEntity
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.mc
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.function.Consumer

@OnlyIn(Dist.CLIENT)
object ModSoundInstances {
    fun init() {
        VehicleEntity.playTrackSound =
            Consumer { mc.soundManager.play(VehicleSoundInstance.TrackSound(it)) }
        VehicleEntity.playEngineSound =
            Consumer { vehicle ->
                vehicle ?: return@Consumer
                if (!StagedVehicleEngineSound.play(vehicle)) {
                    mc.soundManager.play(VehicleSoundInstance.EngineSound(vehicle))
                }
            }
        VehicleEntity.playEngineStartSound =
            Consumer { vehicle -> vehicle?.let(StagedVehicleEngineSound::playStart) }
        VehicleEntity.playEngineStopSound =
            Consumer { vehicle -> vehicle?.let(StagedVehicleEngineSound::playStop) }
        VehicleEntity.playSwimSound =
            Consumer { mc.soundManager.play(VehicleSoundInstance.SwimSound(it)) }
        VehicleEntity.playHornSound =
            Consumer { mc.soundManager.play(HornSoundInstance.VehicleHornSound(it)) }
        VehicleEntity.playStukaSound =
            Consumer { mc.soundManager.play(VehicleSoundInstance.StukaSound(it)) }
        VehicleEntity.playHeliCrashSound =
            Consumer { mc.soundManager.play(VehicleSoundInstance.HeliCrashSound(it)) }
        VehicleEntity.playVehicleSkipSound =
            Consumer { mc.soundManager.play(VehicleSoundInstance.SkipSound(it)) }
//        VehicleEntity.playInCarMusic = vehicle -> {
//            if (NetMusicCompatHolder.canPlayMusic(vehicle)) {
//                NetMusicCompatHolder.playMusic(vehicle);
//            } else {
//                mc.soundManager.play(new InCarMusicInstance.InCarMusicSound(vehicle));
//            }
//        };
        VehicleEntity.playFireSound =
            Consumer(VehicleFireSoundInstance.VehicleFireSound::play)
        FastThrowableProjectile.playFlySound =
            Consumer { mc.soundManager.play(FastProjectileSoundInstance.FlySound(it)) }
        SteelCoilEntity.playMoveSound =
            Consumer { mc.soundManager.play(SteelCoilMoveSoundInstance.SteelCoilMoveSound(it)) }
    }
}
