package com.piratetok.live.http;

/**
 * TikTok web / API literals shared across HTTP clients (not protobuf wire tags).
 */
public final class TikTokWeb {

    public static final String PARAM_AID = "1988";
    public static final String PARAM_APP_NAME = "tiktok_web";
    public static final String PARAM_DEVICE_PLATFORM_WEB_PC = "web_pc";

    /** {@code api-live/user/room} query: {@code sourceType}. */
    public static final String PARAM_USER_ROOM_SOURCE_TYPE = "54";
    /** {@code api-live/user/room} query: {@code staleTime} in ms. */
    public static final String PARAM_USER_ROOM_STALE_TIME_MS = "600000";

    /** JSON {@code statusCode} when the username does not exist. */
    public static final long JSON_STATUS_CODE_USER_NOT_FOUND = 19881007L;
    /** Webcast JSON {@code status_code} for 18+ / age-gated room without cookies. */
    public static final long WEBCAST_STATUS_CODE_AGE_RESTRICTED = 4003110L;

    public static final int HTTP_STATUS_FORBIDDEN = 403;
    public static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    /** User / liveRoom {@code status} when the host is live (web API). */
    public static final long LIVE_STATUS_ON_AIR = 2L;

    public static final String SCREEN_WIDTH = "1920";
    public static final String SCREEN_HEIGHT = "1080";

    private TikTokWeb() {}
}
