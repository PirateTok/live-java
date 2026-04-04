package com.piratetok.live.http;

import com.piratetok.live.Errors.HostNotOnlineException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hits real TikTok HTTP endpoints. Disabled unless env vars are set — default {@code mvn test} stays green.
 *
 * <ul>
 *   <li>{@code PIRATETOK_LIVE_TEST_USER} — TikTok username that is <strong>live</strong> during the run</li>
 *   <li>{@code PIRATETOK_LIVE_TEST_OFFLINE_USER} — optional; must <strong>not</strong> be live</li>
 *   <li>{@code PIRATETOK_LIVE_TEST_COOKIES} — optional; browser cookie header for 18+ room info</li>
 * </ul>
 */
@Tag("integration")
class ApiIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void checkOnline_liveUser_returnsRoomId() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        var result = Api.checkOnline(user, TIMEOUT);
        assertNotNull(result.roomId());
        assertFalse(result.roomId().isEmpty());
        assertFalse("0".equals(result.roomId()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void fetchRoomInfo_liveRoom_returnsRoomInfo() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        var room = Api.checkOnline(user, TIMEOUT);
        String cookies = System.getenv("PIRATETOK_LIVE_TEST_COOKIES");
        if (cookies == null) {
            cookies = "";
        }
        var info = Api.fetchRoomInfo(room.roomId(), TIMEOUT, cookies);
        assertNotNull(info);
        assertTrue(info.viewers() >= 0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_OFFLINE_USER", matches = ".+")
    void checkOnline_offlineUser_throwsHostNotOnline() {
        String user = System.getenv("PIRATETOK_LIVE_TEST_OFFLINE_USER").strip();
        assertThrows(HostNotOnlineException.class, () -> Api.checkOnline(user, TIMEOUT));
    }
}
