package com.runalitycs.normalizer;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FitSdkTest {

    @Test
    void shouldLoadFitSdkClasses() {
        // Given & When
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

        // Then
        assertNotNull(decode);
        assertNotNull(broadcaster);
    }
}