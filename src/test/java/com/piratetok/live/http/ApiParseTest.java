package com.piratetok.live.http;

import com.piratetok.live.Errors.HostNotOnlineException;
import com.piratetok.live.Errors.InvalidResponseException;
import com.piratetok.live.Errors.SessionRequiredException;
import com.piratetok.live.Errors.TikTokApiException;
import com.piratetok.live.Errors.TikTokBlockedException;
import com.piratetok.live.Errors.UserNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiParseTest {

    private static final String AUDIENCE_OK = """
        {"status_code": 0, "data": {"total": 1234, "anonymous": 56, "ranks": [
          {"rank": 1, "score": 900, "user": {
            "id": 111, "id_str": "7000000000000000111", "display_id": "viewer_one",
            "nickname": "Viewer One", "sec_uid": "MS4w-one",
            "avatar_thumb": {"url_list": ["https://p16.example/a.webp", "https://p19.example/a.webp"]},
            "follow_info": {"follower_count": 42},
            "verified": true, "is_follower": true, "is_following": false, "is_subscribe": true}},
          {"rank": 2, "score": 10},
          {"rank": 3, "score": 5, "user": {"id": 333, "display_id": "viewer_three", "nickname": "V3"}}
        ]}}""";

    @Test
    void parsesAudienceSkippingEntriesWithoutUser() {
        var a = Api.parseRoomAudience(AUDIENCE_OK, 200);
        assertEquals(1234, a.total());
        assertEquals(56, a.anonymous());
        assertEquals(2, a.viewers().size());
        assertEquals(AUDIENCE_OK, a.rawJson());
        assertEquals(new Api.AudienceViewer(1, 900, "7000000000000000111", "viewer_one", "Viewer One",
            "MS4w-one", "https://p16.example/a.webp", 42, true, true, false, true), a.viewers().get(0));
        var three = a.viewers().get(1);
        assertEquals("333", three.userId());
        assertNull(three.avatarUrl());
        assertEquals(0, three.followerCount());
    }

    @Test
    void audienceStatus20003IsSessionRequired() {
        var ex = assertThrows(SessionRequiredException.class,
            () -> Api.parseRoomAudience("{\"status_code\":20003,\"data\":{\"message\":\"login\"}}", 200));
        assertTrue(ex.getMessage().contains("session cookies"));
    }

    @Test
    void audienceOtherStatusIsInvalidResponse() {
        var ex = assertThrows(InvalidResponseException.class,
            () -> Api.parseRoomAudience("{\"status_code\":10011,\"data\":{\"message\":\"room gone\"}}", 200));
        assertTrue(ex.getMessage().contains("status_code=10011 room gone"), ex.getMessage());
    }

    @Test
    void audienceMissingStatusOrEmptyBodyIsInvalid() {
        assertThrows(InvalidResponseException.class, () -> Api.parseRoomAudience("{\"data\":{}}", 200));
        var ex = assertThrows(InvalidResponseException.class, () -> Api.parseRoomAudience("", 403));
        assertTrue(ex.getMessage().contains("HTTP 403"));
    }

    @Test
    void checkOnlineExposesAnchorId() {
        var r = Api.parseCheckOnline("someone",
            "{\"statusCode\":0,\"data\":{\"user\":{\"id\":\"6900000000000000001\",\"roomId\":\"7300000000000000001\",\"status\":2},\"liveRoom\":{\"status\":2}}}",
            200);
        assertEquals("7300000000000000001", r.roomId());
        assertEquals("6900000000000000001", r.anchorId());
    }

    @Test
    void checkOnlineErrorMapping() {
        assertThrows(UserNotFoundException.class,
            () -> Api.parseCheckOnline("x", "{\"statusCode\":19881007}", 200));
        var api = assertThrows(TikTokApiException.class,
            () -> Api.parseCheckOnline("x", "{\"statusCode\":4242}", 200));
        assertEquals(4242, api.code);
        assertThrows(HostNotOnlineException.class,
            () -> Api.parseCheckOnline("x", "{\"statusCode\":0,\"data\":{\"user\":{\"roomId\":\"0\"}}}", 200));
        assertThrows(HostNotOnlineException.class,
            () -> Api.parseCheckOnline("x", "{\"statusCode\":0,\"data\":{\"user\":{\"roomId\":\"7\",\"status\":4}}}", 200));
        var blocked = assertThrows(TikTokBlockedException.class, () -> Api.parseCheckOnline("x", "<html>", 200));
        assertEquals(200, blocked.statusCode);
        assertThrows(TikTokBlockedException.class, () -> Api.parseCheckOnline("x", "", 200));
    }
}
