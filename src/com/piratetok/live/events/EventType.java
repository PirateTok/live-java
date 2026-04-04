package com.piratetok.live.events;

public final class EventType {
    public static final String CONNECTED = "connected";
    public static final String RECONNECTING = "reconnecting";
    public static final String DISCONNECTED = "disconnected";
    public static final String UNKNOWN = "unknown";
    public static final String ERROR = "error";

    // core
    public static final String CHAT = "chat";
    public static final String GIFT = "gift";
    public static final String LIKE = "like";
    public static final String MEMBER = "member";
    public static final String SOCIAL = "social";
    public static final String ROOM_USER_SEQ = "roomUserSeq";
    public static final String CONTROL = "control";

    // Sub-routed convenience
    public static final String FOLLOW = "follow";
    public static final String SHARE = "share";
    public static final String JOIN = "join";
    public static final String LIVE_ENDED = "liveEnded";

    // useful
    public static final String LIVE_INTRO = "liveIntro";
    public static final String ROOM_MESSAGE = "roomMessage";
    public static final String CAPTION = "caption";
    public static final String GOAL_UPDATE = "goalUpdate";
    public static final String IM_DELETE = "imDelete";

    // niche + extended
    public static final String RANK_UPDATE = "rankUpdate";
    public static final String POLL = "poll";
    public static final String ENVELOPE = "envelope";
    public static final String ROOM_PIN = "roomPin";
    public static final String UNAUTHORIZED_MEMBER = "unauthorizedMember";
    public static final String LINK_MIC_METHOD = "linkMicMethod";
    public static final String LINK_MIC_BATTLE = "linkMicBattle";
    public static final String LINK_MIC_ARMIES = "linkMicArmies";
    public static final String LINK_MESSAGE = "linkMessage";
    public static final String LINK_LAYER = "linkLayer";
    public static final String LINK_MIC_LAYOUT_STATE = "linkMicLayoutState";
    public static final String GIFT_PANEL_UPDATE = "giftPanelUpdate";
    public static final String IN_ROOM_BANNER = "inRoomBanner";
    public static final String GUIDE = "guide";
    public static final String EMOTE_CHAT = "emoteChat";
    public static final String QUESTION_NEW = "questionNew";
    public static final String SUB_NOTIFY = "subNotify";
    public static final String BARRAGE = "barrage";
    public static final String HOURLY_RANK = "hourlyRank";
    public static final String MSG_DETECT = "msgDetect";
    public static final String LINK_MIC_FAN_TICKET = "linkMicFanTicket";
    public static final String ROOM_VERIFY = "roomVerify";
    public static final String OEC_LIVE_SHOPPING = "oecLiveShopping";
    public static final String GIFT_BROADCAST = "giftBroadcast";
    public static final String RANK_TEXT = "rankText";
    public static final String GIFT_DYNAMIC_RESTRICTION = "giftDynamicRestriction";
    public static final String VIEWER_PICKS_UPDATE = "viewerPicksUpdate";

    // secondary
    public static final String ACCESS_CONTROL = "accessControl";
    public static final String ACCESS_RECALL = "accessRecall";
    public static final String ALERT_BOX_AUDIT_RESULT = "alertBoxAuditResult";
    public static final String BINDING_GIFT = "bindingGift";
    public static final String BOOST_CARD = "boostCard";
    public static final String BOTTOM = "bottom";
    public static final String GAME_RANK_NOTIFY = "gameRankNotify";
    public static final String GIFT_PROMPT = "giftPrompt";
    public static final String LINK_STATE = "linkState";
    public static final String LINK_MIC_BATTLE_PUNISH_FINISH = "linkMicBattlePunishFinish";
    public static final String LINKMIC_BATTLE_TASK = "linkmicBattleTask";
    public static final String MARQUEE_ANNOUNCEMENT = "marqueeAnnouncement";
    public static final String NOTICE = "notice";
    public static final String NOTIFY = "notify";
    public static final String PARTNERSHIP_DROPS_UPDATE = "partnershipDropsUpdate";
    public static final String PARTNERSHIP_GAME_OFFLINE = "partnershipGameOffline";
    public static final String PARTNERSHIP_PUNISH = "partnershipPunish";
    public static final String PERCEPTION = "perception";
    public static final String SPEAKER = "speaker";
    public static final String SUB_CAPSULE = "subCapsule";
    public static final String SUB_PIN_EVENT = "subPinEvent";
    public static final String SUBSCRIPTION_NOTIFY = "subscriptionNotify";
    public static final String TOAST = "toast";
    public static final String SYSTEM = "system";
    public static final String LIVE_GAME_INTRO = "liveGameIntro";

    private EventType() {}
}
