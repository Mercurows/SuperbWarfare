package com.atsuishio.superbwarfare.client.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleEngineSoundMixTest {
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
}
