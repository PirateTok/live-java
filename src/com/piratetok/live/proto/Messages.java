package com.piratetok.live.proto;

import com.piratetok.live.proto.Proto.FieldDef;

import java.util.Map;

import static com.piratetok.live.proto.Proto.FieldType.BOOL;
import static com.piratetok.live.proto.Proto.FieldType.BYTES;
import static com.piratetok.live.proto.Proto.FieldType.INT32;
import static com.piratetok.live.proto.Proto.FieldType.MESSAGE;
import static com.piratetok.live.proto.Proto.FieldType.REPEATED_MESSAGE;
import static com.piratetok.live.proto.Proto.FieldType.STRING;
import static com.piratetok.live.proto.Proto.FieldType.VARINT;
import static com.piratetok.live.proto.Schema.COMMON;
import static com.piratetok.live.proto.Schema.EMOTE;
import static com.piratetok.live.proto.Schema.TEXT;
import static com.piratetok.live.proto.Schema.USER;

public final class Messages {

    // Common-only (many messages just have common + a few fields)
    static final Map<Integer, FieldDef> COMMON_ONLY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON)
    );

    // === Niche + extended events ===

    public static final Map<Integer, FieldDef> RANK_UPDATE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 3, new FieldDef("groupType", VARINT));

    public static final Map<Integer, FieldDef> POLL = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 3, new FieldDef("pollId", VARINT),
        7, new FieldDef("pollKind", INT32));

    public static final Map<Integer, FieldDef> ENVELOPE = COMMON_ONLY;

    public static final Map<Integer, FieldDef> ROOM_PIN = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("pinnedMessage", BYTES),
        30, new FieldDef("originalMsgType", STRING), 31, new FieldDef("timestamp", VARINT));

    public static final Map<Integer, FieldDef> UNAUTHORIZED_MEMBER = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("action", INT32),
        4, new FieldDef("nickName", STRING));

    public static final Map<Integer, FieldDef> LINK_MIC_METHOD = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 3, new FieldDef("accessKey", STRING),
        4, new FieldDef("anchorLinkmicId", VARINT), 8, new FieldDef("channelId", VARINT));

    public static final Map<Integer, FieldDef> LINK_MIC_BATTLE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("id", VARINT));

    public static final Map<Integer, FieldDef> LINK_MIC_ARMIES = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("id", VARINT),
        5, new FieldDef("timeStamp1", VARINT), 6, new FieldDef("timeStamp2", VARINT));

    public static final Map<Integer, FieldDef> LINK_MESSAGE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 3, new FieldDef("linkerId", VARINT));

    public static final Map<Integer, FieldDef> LINK_LAYER = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 3, new FieldDef("channelId", VARINT));

    public static final Map<Integer, FieldDef> LINK_MIC_LAYOUT_STATE = Map.of(
        1, new FieldDef("commonRaw", BYTES), 2, new FieldDef("roomId", VARINT),
        3, new FieldDef("layoutState", INT32), 6, new FieldDef("layoutKey", STRING));

    public static final Map<Integer, FieldDef> GIFT_PANEL_UPDATE = Map.of(
        1, new FieldDef("commonRaw", BYTES), 2, new FieldDef("roomId", VARINT),
        3, new FieldDef("panelTsOrVersion", VARINT));

    public static final Map<Integer, FieldDef> IN_ROOM_BANNER = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("json", STRING));

    public static final Map<Integer, FieldDef> GUIDE = Map.of(
        1, new FieldDef("commonRaw", BYTES), 2, new FieldDef("guideType", INT32),
        5, new FieldDef("durationMs", VARINT), 7, new FieldDef("scene", STRING));

    public static final Map<Integer, FieldDef> EMOTE_CHAT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("user", MESSAGE, USER),
        3, new FieldDef("emoteList", REPEATED_MESSAGE, EMOTE));

    public static final Map<Integer, FieldDef> QUESTION_NEW = COMMON_ONLY;

    public static final Map<Integer, FieldDef> SUB_NOTIFY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("user", MESSAGE, USER),
        4, new FieldDef("subMonth", VARINT));

    public static final Map<Integer, FieldDef> BARRAGE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 5, new FieldDef("content", MESSAGE, TEXT),
        6, new FieldDef("duration", INT32));

    public static final Map<Integer, FieldDef> HOURLY_RANK = COMMON_ONLY;

    public static final Map<Integer, FieldDef> MSG_DETECT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("detectType", INT32),
        6, new FieldDef("fromRegion", STRING));

    public static final Map<Integer, FieldDef> LINK_MIC_FAN_TICKET = COMMON_ONLY;

    public static final Map<Integer, FieldDef> ROOM_VERIFY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("action", INT32),
        3, new FieldDef("content", STRING), 5, new FieldDef("closeRoom", BOOL));

    public static final Map<Integer, FieldDef> OEC_LIVE_SHOPPING = COMMON_ONLY;

    public static final Map<Integer, FieldDef> GIFT_BROADCAST = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("broadcastDataBlob", BYTES));

    public static final Map<Integer, FieldDef> RANK_TEXT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("scene", INT32));

    public static final Map<Integer, FieldDef> GIFT_DYNAMIC_RESTRICTION = Map.of(
        1, new FieldDef("commonRaw", BYTES), 2, new FieldDef("restrictionBlob", BYTES));

    public static final Map<Integer, FieldDef> VIEWER_PICKS_UPDATE = Map.of(
        1, new FieldDef("commonRaw", BYTES), 2, new FieldDef("updateType", INT32));

    // === Secondary events ===

    public static final Map<Integer, FieldDef> ACCESS_CONTROL = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("captchaBlob", BYTES));

    public static final Map<Integer, FieldDef> ACCESS_RECALL = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("status", INT32),
        3, new FieldDef("duration", VARINT), 4, new FieldDef("endTime", VARINT));

    public static final Map<Integer, FieldDef> ALERT_BOX_AUDIT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("userId", VARINT),
        5, new FieldDef("scene", INT32));

    public static final Map<Integer, FieldDef> BINDING_GIFT = Map.of(
        1, new FieldDef("giftMessageBlob", BYTES), 2, new FieldDef("common", MESSAGE, COMMON));

    public static final Map<Integer, FieldDef> BOOST_CARD = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("cardsBlob", BYTES));

    public static final Map<Integer, FieldDef> BOTTOM = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("content", STRING),
        3, new FieldDef("showType", INT32), 5, new FieldDef("duration", VARINT));

    public static final Map<Integer, FieldDef> GAME_RANK_NOTIFY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("msgType", INT32),
        3, new FieldDef("notifyText", STRING));

    public static final Map<Integer, FieldDef> GIFT_PROMPT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("title", STRING),
        3, new FieldDef("body", STRING));

    public static final Map<Integer, FieldDef> LINK_STATE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("channelId", VARINT),
        3, new FieldDef("scene", INT32), 4, new FieldDef("version", INT32));

    public static final Map<Integer, FieldDef> LINK_MIC_BATTLE_PUNISH = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("id1", VARINT),
        3, new FieldDef("timestamp", VARINT));

    public static final Map<Integer, FieldDef> LINKMIC_BATTLE_TASK = COMMON_ONLY;

    public static final Map<Integer, FieldDef> MARQUEE_ANNOUNCEMENT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("messageScene", INT32));

    public static final Map<Integer, FieldDef> NOTICE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("content", STRING),
        3, new FieldDef("noticeType", INT32));

    public static final Map<Integer, FieldDef> NOTIFY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("schema", STRING),
        3, new FieldDef("notifyType", INT32), 4, new FieldDef("contentStr", STRING));

    public static final Map<Integer, FieldDef> PARTNERSHIP_DROPS = COMMON_ONLY;
    public static final Map<Integer, FieldDef> PARTNERSHIP_OFFLINE = COMMON_ONLY;
    public static final Map<Integer, FieldDef> PARTNERSHIP_PUNISH = COMMON_ONLY;

    public static final Map<Integer, FieldDef> PERCEPTION = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("dialogBlob", BYTES),
        4, new FieldDef("endTime", VARINT));

    public static final Map<Integer, FieldDef> SPEAKER = COMMON_ONLY;

    public static final Map<Integer, FieldDef> SUB_CAPSULE = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("description", STRING),
        3, new FieldDef("btnName", STRING), 4, new FieldDef("btnUrl", STRING));

    public static final Map<Integer, FieldDef> SUB_PIN_EVENT = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("actionType", INT32),
        4, new FieldDef("operatorUserId", VARINT));

    public static final Map<Integer, FieldDef> SUBSCRIPTION_NOTIFY = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("user", MESSAGE, USER),
        3, new FieldDef("exhibitionType", INT32));

    public static final Map<Integer, FieldDef> TOAST = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("displayDurationMs", VARINT),
        3, new FieldDef("delayDisplayDurationMs", VARINT));

    public static final Map<Integer, FieldDef> SYSTEM_MSG = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("message", STRING));

    public static final Map<Integer, FieldDef> LIVE_GAME_INTRO = Map.of(
        1, new FieldDef("common", MESSAGE, COMMON), 2, new FieldDef("gameText", MESSAGE, TEXT));

    private Messages() {}
}
