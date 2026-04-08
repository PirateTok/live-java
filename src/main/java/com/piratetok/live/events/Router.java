package com.piratetok.live.events;

import com.piratetok.live.proto.Proto;
import com.piratetok.live.proto.Proto.FieldDef;
import com.piratetok.live.proto.Messages;
import com.piratetok.live.proto.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Router {

    private static final long SOCIAL_ACTION_FOLLOW = 1L;
    private static final long SOCIAL_ACTION_SHARE_MIN = 2L;
    private static final long SOCIAL_ACTION_SHARE_MAX = 5L;
    private static final long MEMBER_ACTION_JOIN = 1L;
    private static final long CONTROL_ACTION_LIVE_ENDED = 3L;

    private record SchemaEntry(String eventType, Map<Integer, FieldDef> schema) {}

    private static final Map<String, SchemaEntry> METHOD_MAP = Map.ofEntries(
        e("WebcastChatMessage", EventType.CHAT, Schema.CHAT),
        e("WebcastGiftMessage", EventType.GIFT, Schema.GIFT),
        e("WebcastLikeMessage", EventType.LIKE, Schema.LIKE),
        e("WebcastMemberMessage", EventType.MEMBER, Schema.MEMBER),
        e("WebcastSocialMessage", EventType.SOCIAL, Schema.SOCIAL),
        e("WebcastRoomUserSeqMessage", EventType.ROOM_USER_SEQ, Schema.ROOM_USER_SEQ),
        e("WebcastControlMessage", EventType.CONTROL, Schema.CONTROL),
        e("WebcastLiveIntroMessage", EventType.LIVE_INTRO, Schema.LIVE_INTRO),
        e("WebcastRoomMessage", EventType.ROOM_MESSAGE, Schema.ROOM_MESSAGE),
        e("WebcastCaptionMessage", EventType.CAPTION, Schema.CAPTION),
        e("WebcastGoalUpdateMessage", EventType.GOAL_UPDATE, Schema.GOAL_UPDATE),
        e("WebcastImDeleteMessage", EventType.IM_DELETE, Schema.IM_DELETE),
        e("WebcastRankUpdateMessage", EventType.RANK_UPDATE, Messages.RANK_UPDATE),
        e("WebcastPollMessage", EventType.POLL, Messages.POLL),
        e("WebcastEnvelopeMessage", EventType.ENVELOPE, Messages.ENVELOPE),
        e("WebcastRoomPinMessage", EventType.ROOM_PIN, Messages.ROOM_PIN),
        e("WebcastUnauthorizedMemberMessage", EventType.UNAUTHORIZED_MEMBER, Messages.UNAUTHORIZED_MEMBER),
        e("WebcastLinkMicMethod", EventType.LINK_MIC_METHOD, Messages.LINK_MIC_METHOD),
        e("WebcastLinkMicBattle", EventType.LINK_MIC_BATTLE, Messages.LINK_MIC_BATTLE),
        e("WebcastLinkMicArmies", EventType.LINK_MIC_ARMIES, Messages.LINK_MIC_ARMIES),
        e("WebcastLinkMessage", EventType.LINK_MESSAGE, Messages.LINK_MESSAGE),
        e("WebcastLinkLayerMessage", EventType.LINK_LAYER, Messages.LINK_LAYER),
        e("WebcastLinkMicLayoutStateMessage", EventType.LINK_MIC_LAYOUT_STATE, Messages.LINK_MIC_LAYOUT_STATE),
        e("WebcastGiftPanelUpdateMessage", EventType.GIFT_PANEL_UPDATE, Messages.GIFT_PANEL_UPDATE),
        e("WebcastInRoomBannerMessage", EventType.IN_ROOM_BANNER, Messages.IN_ROOM_BANNER),
        e("WebcastGuideMessage", EventType.GUIDE, Messages.GUIDE),
        e("WebcastEmoteChatMessage", EventType.EMOTE_CHAT, Messages.EMOTE_CHAT),
        e("WebcastQuestionNewMessage", EventType.QUESTION_NEW, Messages.QUESTION_NEW),
        e("WebcastSubNotifyMessage", EventType.SUB_NOTIFY, Messages.SUB_NOTIFY),
        e("WebcastBarrageMessage", EventType.BARRAGE, Messages.BARRAGE),
        e("WebcastHourlyRankMessage", EventType.HOURLY_RANK, Messages.HOURLY_RANK),
        e("WebcastMsgDetectMessage", EventType.MSG_DETECT, Messages.MSG_DETECT),
        e("WebcastLinkMicFanTicketMethod", EventType.LINK_MIC_FAN_TICKET, Messages.LINK_MIC_FAN_TICKET),
        e("RoomVerifyMessage", EventType.ROOM_VERIFY, Messages.ROOM_VERIFY),
        e("WebcastOecLiveShoppingMessage", EventType.OEC_LIVE_SHOPPING, Messages.OEC_LIVE_SHOPPING),
        e("WebcastGiftBroadcastMessage", EventType.GIFT_BROADCAST, Messages.GIFT_BROADCAST),
        e("WebcastRankTextMessage", EventType.RANK_TEXT, Messages.RANK_TEXT),
        e("WebcastGiftDynamicRestrictionMessage", EventType.GIFT_DYNAMIC_RESTRICTION, Messages.GIFT_DYNAMIC_RESTRICTION),
        e("WebcastViewerPicksUpdateMessage", EventType.VIEWER_PICKS_UPDATE, Messages.VIEWER_PICKS_UPDATE),
        e("WebcastAccessControlMessage", EventType.ACCESS_CONTROL, Messages.ACCESS_CONTROL),
        e("WebcastAccessRecallMessage", EventType.ACCESS_RECALL, Messages.ACCESS_RECALL),
        e("WebcastAlertBoxAuditResultMessage", EventType.ALERT_BOX_AUDIT_RESULT, Messages.ALERT_BOX_AUDIT),
        e("WebcastBindingGiftMessage", EventType.BINDING_GIFT, Messages.BINDING_GIFT),
        e("WebcastBoostCardMessage", EventType.BOOST_CARD, Messages.BOOST_CARD),
        e("WebcastBottomMessage", EventType.BOTTOM, Messages.BOTTOM),
        e("WebcastGameRankNotifyMessage", EventType.GAME_RANK_NOTIFY, Messages.GAME_RANK_NOTIFY),
        e("WebcastGiftPromptMessage", EventType.GIFT_PROMPT, Messages.GIFT_PROMPT),
        e("WebcastLinkStateMessage", EventType.LINK_STATE, Messages.LINK_STATE),
        e("WebcastLinkMicBattlePunishFinish", EventType.LINK_MIC_BATTLE_PUNISH_FINISH, Messages.LINK_MIC_BATTLE_PUNISH),
        e("WebcastLinkmicBattleTaskMessage", EventType.LINKMIC_BATTLE_TASK, Messages.LINKMIC_BATTLE_TASK),
        e("WebcastMarqueeAnnouncementMessage", EventType.MARQUEE_ANNOUNCEMENT, Messages.MARQUEE_ANNOUNCEMENT),
        e("WebcastNoticeMessage", EventType.NOTICE, Messages.NOTICE),
        e("WebcastNotifyMessage", EventType.NOTIFY, Messages.NOTIFY),
        e("WebcastPartnershipDropsUpdateMessage", EventType.PARTNERSHIP_DROPS_UPDATE, Messages.PARTNERSHIP_DROPS),
        e("WebcastPartnershipGameOfflineMessage", EventType.PARTNERSHIP_GAME_OFFLINE, Messages.PARTNERSHIP_OFFLINE),
        e("WebcastPartnershipPunishMessage", EventType.PARTNERSHIP_PUNISH, Messages.PARTNERSHIP_PUNISH),
        e("WebcastPerceptionMessage", EventType.PERCEPTION, Messages.PERCEPTION),
        e("WebcastSpeakerMessage", EventType.SPEAKER, Messages.SPEAKER),
        e("WebcastSubCapsuleMessage", EventType.SUB_CAPSULE, Messages.SUB_CAPSULE),
        e("WebcastSubPinEventMessage", EventType.SUB_PIN_EVENT, Messages.SUB_PIN_EVENT),
        e("WebcastSubscriptionNotifyMessage", EventType.SUBSCRIPTION_NOTIFY, Messages.SUBSCRIPTION_NOTIFY),
        e("WebcastToastMessage", EventType.TOAST, Messages.TOAST),
        e("WebcastSystemMessage", EventType.SYSTEM, Messages.SYSTEM_MSG),
        e("WebcastLiveGameIntroMessage", EventType.LIVE_GAME_INTRO, Messages.LIVE_GAME_INTRO)
    );

    public static List<TikTokEvent> decode(String method, byte[] payload, String roomId) {
        var entry = METHOD_MAP.get(method);
        if (entry == null) {
            return List.of(new TikTokEvent(EventType.UNKNOWN, Map.of("method", method), roomId));
        }

        Map<String, Object> data;
        try {
            var protoMap = Proto.decode(payload);
            data = protoMap.toMap(entry.schema());
        } catch (RuntimeException _ex) {
            return List.of(new TikTokEvent(EventType.UNKNOWN, Map.of("method", method), roomId));
        }

        var events = new ArrayList<TikTokEvent>();
        events.add(new TikTokEvent(entry.eventType(), data, roomId));

        // Sub-routing
        if ("WebcastSocialMessage".equals(method)) {
            long action = longFrom(data, "action");
            if (action == SOCIAL_ACTION_FOLLOW) {
                events.add(new TikTokEvent(EventType.FOLLOW, data, roomId));
            } else if (action >= SOCIAL_ACTION_SHARE_MIN && action <= SOCIAL_ACTION_SHARE_MAX) {
                events.add(new TikTokEvent(EventType.SHARE, data, roomId));
            }
        } else if ("WebcastMemberMessage".equals(method)) {
            long action = longFrom(data, "action");
            if (action == MEMBER_ACTION_JOIN) {
                events.add(new TikTokEvent(EventType.JOIN, data, roomId));
            }
        } else if ("WebcastControlMessage".equals(method)) {
            long action = longFrom(data, "action");
            if (action == CONTROL_ACTION_LIVE_ENDED) {
                events.add(new TikTokEvent(EventType.LIVE_ENDED, data, roomId));
            }
        }

        return events;
    }

    private static long longFrom(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0;
    }

    private static Map.Entry<String, SchemaEntry> e(String method, String eventType, Map<Integer, FieldDef> schema) {
        return Map.entry(method, new SchemaEntry(eventType, schema));
    }

    private Router() {}
}
