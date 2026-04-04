package com.piratetok.live.proto;

import com.piratetok.live.proto.Proto.FieldDef;
import com.piratetok.live.proto.Proto.FieldType;

import java.util.Map;

import static com.piratetok.live.proto.Proto.FieldType.BOOL;
import static com.piratetok.live.proto.Proto.FieldType.BYTES;
import static com.piratetok.live.proto.Proto.FieldType.INT32;
import static com.piratetok.live.proto.Proto.FieldType.MESSAGE;
import static com.piratetok.live.proto.Proto.FieldType.REPEATED_MESSAGE;
import static com.piratetok.live.proto.Proto.FieldType.STRING;
import static com.piratetok.live.proto.Proto.FieldType.STRING_MAP;
import static com.piratetok.live.proto.Proto.FieldType.VARINT;

/**
 * TikTok webcast protobuf field layouts; map keys are wire tags from upstream payloads.
 */
public final class Schema {

    // === Common types ===

    public static final Map<Integer, FieldDef> IMAGE = Map.of(
        1, new FieldDef("urlList", STRING),
        2, new FieldDef("uri", STRING),
        3, new FieldDef("width", INT32),
        4, new FieldDef("height", INT32)
    );

    public static final Map<Integer, FieldDef> TEXT = Map.of(
        1, new FieldDef("key", STRING),
        2, new FieldDef("defaultPattern", STRING)
    );

    public static final Map<Integer, FieldDef> COMMON = Map.of(
        1, new FieldDef("method", STRING),
        2, new FieldDef("msgId", VARINT),
        3, new FieldDef("roomId", VARINT),
        4, new FieldDef("createTime", VARINT),
        7, new FieldDef("describe", STRING)
    );

    public static final Map<Integer, FieldDef> PRIVILEGE_LOG_EXTRA = Map.of(
        1, new FieldDef("dataVersion", STRING),
        2, new FieldDef("privilegeId", STRING),
        5, new FieldDef("level", STRING)
    );

    public static final Map<Integer, FieldDef> BADGE_IMAGE = Map.of(
        2, new FieldDef("image", MESSAGE, IMAGE)
    );

    public static final Map<Integer, FieldDef> BADGE_TEXT = Map.of(
        2, new FieldDef("key", STRING),
        3, new FieldDef("defaultPattern", STRING)
    );

    public static final Map<Integer, FieldDef> BADGE_STRING = Map.of(
        2, new FieldDef("contentStr", STRING)
    );

    // badge_scene: ADMIN=1, SUBSCRIBER=4, RANK_LIST=6, USER_GRADE=8, FANS=10
    public static final Map<Integer, FieldDef> BADGE_STRUCT = Map.of(
        1, new FieldDef("displayType", INT32),
        3, new FieldDef("badgeScene", INT32),
        11, new FieldDef("display", BOOL),
        12, new FieldDef("logExtra", MESSAGE, PRIVILEGE_LOG_EXTRA),
        20, new FieldDef("imageBadge", MESSAGE, BADGE_IMAGE),
        21, new FieldDef("textBadge", MESSAGE, BADGE_TEXT),
        22, new FieldDef("stringBadge", MESSAGE, BADGE_STRING)
    );

    public static final Map<Integer, FieldDef> FOLLOW_INFO = Map.of(
        1, new FieldDef("followingCount", VARINT),
        2, new FieldDef("followerCount", VARINT),
        3, new FieldDef("followStatus", VARINT)
    );

    public static final Map<Integer, FieldDef> FANS_CLUB_DATA = Map.of(
        1, new FieldDef("clubName", STRING),
        2, new FieldDef("level", INT32)
    );

    public static final Map<Integer, FieldDef> FANS_CLUB_MEMBER = Map.of(
        1, new FieldDef("data", MESSAGE, FANS_CLUB_DATA)
    );

    public static final Map<Integer, FieldDef> USER = Map.ofEntries(
        Map.entry(1, new FieldDef("id", VARINT)),
        Map.entry(3, new FieldDef("nickname", STRING)),
        Map.entry(5, new FieldDef("bioDescription", STRING)),
        Map.entry(9, new FieldDef("avatarThumb", MESSAGE, IMAGE)),
        Map.entry(12, new FieldDef("verified", BOOL)),
        Map.entry(22, new FieldDef("followInfo", MESSAGE, FOLLOW_INFO)),
        Map.entry(24, new FieldDef("fansClub", MESSAGE, FANS_CLUB_MEMBER)),
        Map.entry(31, new FieldDef("topVipNo", INT32)),
        Map.entry(34, new FieldDef("payScore", VARINT)),
        Map.entry(35, new FieldDef("fanTicketCount", VARINT)),
        Map.entry(38, new FieldDef("uniqueId", STRING)),
        Map.entry(46, new FieldDef("displayId", STRING)),
        Map.entry(64, new FieldDef("badgeList", REPEATED_MESSAGE, BADGE_STRUCT)),
        Map.entry(1024, new FieldDef("followStatus", VARINT)),
        Map.entry(1029, new FieldDef("isFollower", BOOL)),
        Map.entry(1030, new FieldDef("isFollowing", BOOL)),
        Map.entry(1090, new FieldDef("isSubscribe", BOOL))
    );

    public static final Map<Integer, FieldDef> GIFT_STRUCT = Map.ofEntries(
        Map.entry(1, new FieldDef("image", MESSAGE, IMAGE)),
        Map.entry(2, new FieldDef("describe", STRING)),
        Map.entry(4, new FieldDef("duration", VARINT)),
        Map.entry(5, new FieldDef("id", VARINT)),
        Map.entry(10, new FieldDef("combo", BOOL)),
        Map.entry(11, new FieldDef("type", INT32)),
        Map.entry(12, new FieldDef("diamondCount", INT32)),
        Map.entry(16, new FieldDef("name", STRING))
    );

    public static final Map<Integer, FieldDef> EMOTE = Map.of(
        1, new FieldDef("emoteId", STRING),
        2, new FieldDef("image", MESSAGE, IMAGE)
    );

    // === Frame types ===

    public static final Map<Integer, FieldDef> PUSH_FRAME = Map.ofEntries(
        Map.entry(1, new FieldDef("seqId", VARINT)),
        Map.entry(2, new FieldDef("logId", VARINT)),
        Map.entry(5, new FieldDef("headers", STRING_MAP)),
        Map.entry(6, new FieldDef("payloadEncoding", STRING)),
        Map.entry(7, new FieldDef("payloadType", STRING)),
        Map.entry(8, new FieldDef("payload", BYTES))
    );

    public static final Map<Integer, FieldDef> RESPONSE_MESSAGE = Map.of(
        1, new FieldDef("method", STRING),
        2, new FieldDef("payload", BYTES),
        3, new FieldDef("msgId", VARINT),
        4, new FieldDef("msgType", INT32),
        5, new FieldDef("offset", VARINT),
        6, new FieldDef("isHistory", BOOL)
    );

    public static final Map<Integer, FieldDef> WEBCAST_RESPONSE = Map.ofEntries(
        Map.entry(1, new FieldDef("messages", REPEATED_MESSAGE, RESPONSE_MESSAGE)),
        Map.entry(2, new FieldDef("cursor", STRING)),
        Map.entry(5, new FieldDef("internalExt", BYTES)),
        Map.entry(8, new FieldDef("heartBeatDuration", VARINT)),
        Map.entry(9, new FieldDef("needsAck", BOOL))
    );

    // === Core messages ===

    public static final Map<Integer, FieldDef> CHAT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("user", MESSAGE, USER),
        3, new FieldDef("content", STRING),
        14, new FieldDef("contentLanguage", STRING)
    );

    public static final Map<Integer, FieldDef> GIFT = Map.ofEntries(
        Map.entry(1, new FieldDef("common", MESSAGE, COMMON)),
        Map.entry(2, new FieldDef("giftId", VARINT)),
        Map.entry(3, new FieldDef("fanTicketCount", VARINT)),
        Map.entry(4, new FieldDef("groupCount", INT32)),
        Map.entry(5, new FieldDef("repeatCount", INT32)),
        Map.entry(6, new FieldDef("comboCount", INT32)),
        Map.entry(7, new FieldDef("user", MESSAGE, USER)),
        Map.entry(8, new FieldDef("toUser", MESSAGE, USER)),
        Map.entry(9, new FieldDef("repeatEnd", INT32)),
        Map.entry(11, new FieldDef("groupId", VARINT)),
        Map.entry(13, new FieldDef("roomFanTicketCount", VARINT)),
        Map.entry(15, new FieldDef("gift", MESSAGE, GIFT_STRUCT)),
        Map.entry(17, new FieldDef("sendType", VARINT))
    );

    public static final Map<Integer, FieldDef> LIKE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("count", INT32),
        3, new FieldDef("total", INT32),
        5, new FieldDef("user", MESSAGE, USER)
    );

    public static final Map<Integer, FieldDef> MEMBER = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("user", MESSAGE, USER),
        3, new FieldDef("memberCount", INT32),
        10, new FieldDef("action", INT32),
        11, new FieldDef("actionDescription", STRING)
    );

    public static final Map<Integer, FieldDef> SOCIAL = Map.ofEntries(
        Map.entry(1, new FieldDef("common", MESSAGE, COMMON)),
        Map.entry(2, new FieldDef("user", MESSAGE, USER)),
        Map.entry(3, new FieldDef("shareType", VARINT)),
        Map.entry(4, new FieldDef("action", VARINT)),
        Map.entry(5, new FieldDef("shareTarget", STRING)),
        Map.entry(6, new FieldDef("followCount", INT32)),
        Map.entry(8, new FieldDef("shareCount", INT32))
    );

    public static final Map<Integer, FieldDef> ROOM_USER_SEQ = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        3, new FieldDef("total", VARINT),
        4, new FieldDef("popStr", STRING),
        6, new FieldDef("popularity", VARINT),
        7, new FieldDef("totalUser", INT32)
    );

    public static final Map<Integer, FieldDef> CONTROL = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("action", INT32),
        3, new FieldDef("tips", STRING)
    );

    public static final Map<Integer, FieldDef> LIVE_INTRO = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("roomId", VARINT),
        4, new FieldDef("content", STRING),
        5, new FieldDef("host", MESSAGE, USER),
        8, new FieldDef("language", STRING)
    );

    public static final Map<Integer, FieldDef> ROOM_MESSAGE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("content", STRING)
    );

    public static final Map<Integer, FieldDef> CAPTION = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        2, new FieldDef("timeStamp", VARINT)
    );

    public static final Map<Integer, FieldDef> GOAL_UPDATE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON),
        4, new FieldDef("contributorId", VARINT),
        9, new FieldDef("contributeCount", VARINT),
        10, new FieldDef("contributeScore", VARINT)
    );

    public static final Map<Integer, FieldDef> IM_DELETE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON)
    );

    private Schema() {}
}
