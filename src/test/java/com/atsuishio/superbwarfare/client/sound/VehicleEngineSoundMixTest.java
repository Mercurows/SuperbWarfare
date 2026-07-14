package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.init.VehicleEngineSoundLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleEngineSoundMixTest {
    @Test
    void groundMixerPrioritizesResourcePackDistantStreams() {
        assertEquals(
                VehicleEngineSoundLayer.DISTANCE,
                VehicleEngineSoundLayer.Companion.getGROUND_LAYERS().getFirst()
        );
    }

    @Test
    void driveAndIdleCrossfadeInLessThanHalfASecond() {
        float driveMix = 0f;
        for (int tick = 0; tick < 5; tick++) {
            driveMix = VehicleEngineSoundMix.nextDriveMix(driveMix, true);
        }
        assertEquals(1f, driveMix, 0.0001f);

        for (int tick = 0; tick < 8; tick++) {
            driveMix = VehicleEngineSoundMix.nextDriveMix(driveMix, false);
        }
        assertEquals(0f, driveMix, 0.0001f);
    }

    @Test
    void releaseLayerBridgesDriveBackToIdle() {
        var mix = VehicleEngineSoundMix.groundMix(0.7f, false);
        assertEquals(0.3f, mix.getIdle(), 0.0001f);
        assertEquals(0f, mix.getDrive(), 0.0001f);
        assertEquals(0.7f, mix.getRelease(), 0.0001f);
    }

    @Test
    void distantLayerIsFullyMixedBeforeCloseLayerEnds() {
        assertEquals(0f, VehicleEngineSoundMix.distantBlend(20f), 0.0001f);
        assertTrue(VehicleEngineSoundMix.distantBlend(38f) > 0.45f);
        assertEquals(1f, VehicleEngineSoundMix.distantBlend(56f), 0.0001f);
    }

    @Test
    void staticDistantLayerBacksUpARejectedCloseStream() {
        assertEquals(0.8f, VehicleEngineSoundMix.missingCloseFallback(0f, 1f, 1f), 0.0001f);
        assertEquals(0.55f, VehicleEngineSoundMix.missingCloseFallback(1f, 0f, 1f), 0.0001f);
        assertEquals(0.6f, VehicleEngineSoundMix.missingCloseFallback(0f, 1f, 0.75f), 0.0001f);
        assertEquals(0f, VehicleEngineSoundMix.missingCloseFallback(0f, 1f, 0f), 0.0001f);
    }

    @Test
    void distantMixStaysAudibleWithoutHittingTheSoundEngineLimiter() {
        var ground = VehicleEngineSoundMix.distantVolume(0.7f * 1.06f, VehicleEngineSoundMix.groundDistantGain(1f));
        var rotor = VehicleEngineSoundMix.distantVolume(2f, VehicleEngineSoundMix.rotorDistantGain());
        var turbine = VehicleEngineSoundMix.distantVolume(2f * 1.1f, VehicleEngineSoundMix.turbineDistantGain());
        var aircraft = VehicleEngineSoundMix.distantVolume(1.3f, VehicleEngineSoundMix.aircraftDistantGain(1f));

        assertTrue(ground >= 0.45f && ground <= 0.6f);
        assertTrue(rotor >= 0.45f && rotor <= 0.6f);
        assertTrue(turbine >= 0.45f && turbine <= 0.6f);
        assertTrue(aircraft >= 0.45f && aircraft <= 0.6f);
        assertEquals(0.6f, VehicleEngineSoundMix.distantVolume(10f, 10f), 0.0001f);
    }
}
