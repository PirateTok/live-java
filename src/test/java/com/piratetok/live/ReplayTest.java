package com.piratetok.live;

import com.piratetok.live.connection.Frames;
import com.piratetok.live.events.EventType;
import com.piratetok.live.events.Router;
import com.piratetok.live.events.TikTokEvent;
import com.piratetok.live.helpers.GiftStreakTracker;
import com.piratetok.live.helpers.LikeAccumulator;
import com.piratetok.live.proto.Proto;
import com.piratetok.live.proto.Schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Replay test — reads a capture file, processes it through the full decode
 * pipeline, and asserts every value matches the manifest JSON.
 *
 * <p>Skips if testdata is not available. Clone {@code live-testdata} into
 * {@code ../live-testdata/} or set {@code PIRATETOK_TESTDATA} env var.</p>
 */
class ReplayTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // --- event type name mapping (Java camelCase -> manifest PascalCase) ---

    private static final Map<String, String> EVENT_NAME_MAP = Map.ofEntries(
        Map.entry(EventType.CHAT, "Chat"),
        Map.entry(EventType.GIFT, "Gift"),
        Map.entry(EventType.LIKE, "Like"),
        Map.entry(EventType.MEMBER, "Member"),
        Map.entry(EventType.SOCIAL, "Social"),
        Map.entry(EventType.FOLLOW, "Follow"),
        Map.entry(EventType.SHARE, "Share"),
        Map.entry(EventType.JOIN, "Join"),
        Map.entry(EventType.ROOM_USER_SEQ, "RoomUserSeq"),
        Map.entry(EventType.CONTROL, "Control"),
        Map.entry(EventType.LIVE_ENDED, "LiveEnded"),
        Map.entry(EventType.LIVE_INTRO, "LiveIntro"),
        Map.entry(EventType.ROOM_MESSAGE, "RoomMessage"),
        Map.entry(EventType.CAPTION, "Caption"),
        Map.entry(EventType.GOAL_UPDATE, "GoalUpdate"),
        Map.entry(EventType.IM_DELETE, "ImDelete"),
        Map.entry(EventType.RANK_UPDATE, "RankUpdate"),
        Map.entry(EventType.POLL, "Poll"),
        Map.entry(EventType.ENVELOPE, "Envelope"),
        Map.entry(EventType.ROOM_PIN, "RoomPin"),
        Map.entry(EventType.UNAUTHORIZED_MEMBER, "UnauthorizedMember"),
        Map.entry(EventType.LINK_MIC_METHOD, "LinkMicMethod"),
        Map.entry(EventType.LINK_MIC_BATTLE, "LinkMicBattle"),
        Map.entry(EventType.LINK_MIC_ARMIES, "LinkMicArmies"),
        Map.entry(EventType.LINK_MESSAGE, "LinkMessage"),
        Map.entry(EventType.LINK_LAYER, "LinkLayer"),
        Map.entry(EventType.LINK_MIC_LAYOUT_STATE, "LinkMicLayoutState"),
        Map.entry(EventType.GIFT_PANEL_UPDATE, "GiftPanelUpdate"),
        Map.entry(EventType.IN_ROOM_BANNER, "InRoomBanner"),
        Map.entry(EventType.GUIDE, "Guide"),
        Map.entry(EventType.EMOTE_CHAT, "EmoteChat"),
        Map.entry(EventType.QUESTION_NEW, "QuestionNew"),
        Map.entry(EventType.SUB_NOTIFY, "SubNotify"),
        Map.entry(EventType.BARRAGE, "Barrage"),
        Map.entry(EventType.HOURLY_RANK, "HourlyRank"),
        Map.entry(EventType.MSG_DETECT, "MsgDetect"),
        Map.entry(EventType.LINK_MIC_FAN_TICKET, "LinkMicFanTicket"),
        Map.entry(EventType.ROOM_VERIFY, "RoomVerify"),
        Map.entry(EventType.OEC_LIVE_SHOPPING, "OecLiveShopping"),
        Map.entry(EventType.GIFT_BROADCAST, "GiftBroadcast"),
        Map.entry(EventType.RANK_TEXT, "RankText"),
        Map.entry(EventType.GIFT_DYNAMIC_RESTRICTION, "GiftDynamicRestriction"),
        Map.entry(EventType.VIEWER_PICKS_UPDATE, "ViewerPicksUpdate"),
        Map.entry(EventType.SYSTEM, "SystemMessage"),
        Map.entry(EventType.LIVE_GAME_INTRO, "LiveGameIntro"),
        Map.entry(EventType.ACCESS_CONTROL, "AccessControl"),
        Map.entry(EventType.ACCESS_RECALL, "AccessRecall"),
        Map.entry(EventType.ALERT_BOX_AUDIT_RESULT, "AlertBoxAuditResult"),
        Map.entry(EventType.BINDING_GIFT, "BindingGift"),
        Map.entry(EventType.BOOST_CARD, "BoostCard"),
        Map.entry(EventType.BOTTOM, "BottomMessage"),
        Map.entry(EventType.GAME_RANK_NOTIFY, "GameRankNotify"),
        Map.entry(EventType.GIFT_PROMPT, "GiftPrompt"),
        Map.entry(EventType.LINK_STATE, "LinkState"),
        Map.entry(EventType.LINK_MIC_BATTLE_PUNISH_FINISH, "LinkMicBattlePunishFinish"),
        Map.entry(EventType.LINKMIC_BATTLE_TASK, "LinkmicBattleTask"),
        Map.entry(EventType.MARQUEE_ANNOUNCEMENT, "MarqueeAnnouncement"),
        Map.entry(EventType.NOTICE, "Notice"),
        Map.entry(EventType.NOTIFY, "Notify"),
        Map.entry(EventType.PARTNERSHIP_DROPS_UPDATE, "PartnershipDropsUpdate"),
        Map.entry(EventType.PARTNERSHIP_GAME_OFFLINE, "PartnershipGameOffline"),
        Map.entry(EventType.PARTNERSHIP_PUNISH, "PartnershipPunish"),
        Map.entry(EventType.PERCEPTION, "Perception"),
        Map.entry(EventType.SPEAKER, "Speaker"),
        Map.entry(EventType.SUB_CAPSULE, "SubCapsule"),
        Map.entry(EventType.SUB_PIN_EVENT, "SubPinEvent"),
        Map.entry(EventType.SUBSCRIPTION_NOTIFY, "SubscriptionNotify"),
        Map.entry(EventType.TOAST, "Toast"),
        Map.entry(EventType.UNKNOWN, "Unknown")
    );

    // --- testdata location ---

    private static Path findTestdata() {
        // 1. PIRATETOK_TESTDATA env var
        String env = System.getenv("PIRATETOK_TESTDATA");
        if (env != null) {
            Path p = Path.of(env);
            if (Files.exists(p)) return p;
        }
        // 2. ../live-testdata/ (shared testdata repo)
        Path testdata = Path.of("../live-testdata");
        if (Files.exists(testdata.resolve("captures"))) return testdata;
        // 3. ../live-rs/captures/ (dev fallback)
        Path dev = Path.of("../live-rs/captures");
        if (Files.exists(dev)) return dev.getParent();
        return null;
    }

    private static Path capturePath(Path testdata, String name) {
        Path inCaptures = testdata.resolve("captures").resolve(name + ".bin");
        if (Files.exists(inCaptures)) return inCaptures;
        return testdata.resolve(name + ".bin");
    }

    private static Path manifestPath(Path testdata, String name) {
        Path inManifests = testdata.resolve("manifests").resolve(name + ".json");
        if (Files.exists(inManifests)) return inManifests;
        return testdata.resolve("captures").resolve("manifests").resolve(name + ".json");
    }

    // --- frame reader ---

    private static List<byte[]> readCapture(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        var frames = new ArrayList<byte[]>();
        int pos = 0;
        while (pos + 4 <= data.length) {
            int len = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            pos += 4;
            if (pos + len > data.length) {
                throw new IOException("truncated frame at offset " + (pos - 4));
            }
            byte[] frame = new byte[len];
            System.arraycopy(data, pos, frame, 0, len);
            frames.add(frame);
            pos += len;
        }
        return frames;
    }

    // --- replay engine ---

    private record LikeEventRecord(int wireCount, int wireTotal, int accTotal, long accumulated, boolean wentBackwards) {}
    private record GiftGroupRecord(int giftId, int repeatCount, int delta, boolean isFinal, long diamondTotal) {}

    private record ReplayResult(
        long frameCount, long messageCount, long eventCount,
        long decodeFailures, long decompressFailures,
        Map<String, Long> payloadTypes, Map<String, Long> messageTypes, Map<String, Long> eventTypes,
        long followCount, long shareCount, long joinCount, long liveEndedCount,
        Map<String, Long> unknownTypes,
        List<LikeEventRecord> likeEvents,
        Map<String, List<GiftGroupRecord>> giftGroups,
        long comboCount, long nonComboCount, long streakFinals, long negativeDeltaCount
    ) {}

    private static ReplayResult replay(List<byte[]> frames) {
        var payloadTypes = new TreeMap<String, Long>();
        var messageTypes = new TreeMap<String, Long>();
        var eventTypes = new TreeMap<String, Long>();
        var unknownTypes = new TreeMap<String, Long>();
        long messageCount = 0, eventCount = 0, decodeFailures = 0, decompressFailures = 0;
        long followCount = 0, shareCount = 0, joinCount = 0, liveEndedCount = 0;

        var likeAcc = new LikeAccumulator();
        var giftTracker = new GiftStreakTracker();
        var likeEvents = new ArrayList<LikeEventRecord>();
        var giftGroups = new TreeMap<String, List<GiftGroupRecord>>();
        long comboCount = 0, nonComboCount = 0, streakFinals = 0, negativeDeltaCount = 0;

        for (byte[] raw : frames) {
            Proto.ProtoMap frame;
            try {
                frame = Proto.decode(raw);
            } catch (RuntimeException ex) {
                decodeFailures++;
                continue;
            }

            String payloadType = frame.getString(7);
            payloadTypes.merge(payloadType, 1L, Long::sum);

            if (!"msg".equals(payloadType)) continue;

            byte[] payload;
            try {
                payload = Frames.decompressIfGzipped(frame.getRawBytes(8));
            } catch (IOException ex) {
                decompressFailures++;
                continue;
            }

            Proto.ProtoMap response;
            try {
                response = Proto.decode(payload);
            } catch (RuntimeException ex) {
                decodeFailures++;
                continue;
            }

            for (var msg : response.getRepeatedMessages(1)) {
                messageCount++;
                String method = msg.getString(1);
                byte[] msgPayload = msg.getRawBytes(2);
                messageTypes.merge(method, 1L, Long::sum);

                List<TikTokEvent> events = Router.decode(method, msgPayload, "0");
                for (var evt : events) {
                    eventCount++;
                    String manifestName = EVENT_NAME_MAP.getOrDefault(evt.type(), evt.type());
                    eventTypes.merge(manifestName, 1L, Long::sum);

                    switch (evt.type()) {
                        case EventType.FOLLOW -> followCount++;
                        case EventType.SHARE -> shareCount++;
                        case EventType.JOIN -> joinCount++;
                        case EventType.LIVE_ENDED -> liveEndedCount++;
                        case EventType.UNKNOWN -> {
                            String unknownMethod = (String) evt.data().getOrDefault("method", "");
                            unknownTypes.merge(unknownMethod, 1L, Long::sum);
                        }
                        default -> {}
                    }
                }

                // Like accumulator
                if ("WebcastLikeMessage".equals(method)) {
                    try {
                        var likeMap = Proto.decode(msgPayload).toMap(Schema.LIKE);
                        var stats = likeAcc.process(likeMap);
                        int wireCount = intVal(likeMap, "count");
                        int wireTotal = intVal(likeMap, "total");
                        likeEvents.add(new LikeEventRecord(
                            wireCount, wireTotal, stats.totalLikeCount(), stats.accumulatedCount(), stats.wentBackwards()
                        ));
                    } catch (RuntimeException ignored) {}
                }

                // Gift streak tracker
                if ("WebcastGiftMessage".equals(method)) {
                    try {
                        var giftMap = Proto.decode(msgPayload).toMap(Schema.GIFT);
                        @SuppressWarnings("unchecked")
                        var giftStruct = (Map<String, Object>) giftMap.getOrDefault("gift", Map.of());
                        int giftType = intVal(giftStruct, "type");
                        boolean isCombo = giftType == 1;
                        if (isCombo) comboCount++; else nonComboCount++;

                        var streak = giftTracker.process(giftMap);
                        if (streak.isFinal()) streakFinals++;
                        if (streak.eventGiftCount() < 0) negativeDeltaCount++;

                        String key = String.valueOf(longVal(giftMap, "groupId"));
                        giftGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(new GiftGroupRecord(
                            intVal(giftMap, "giftId"),
                            intVal(giftMap, "repeatCount"),
                            streak.eventGiftCount(),
                            streak.isFinal(),
                            streak.totalDiamondCount()
                        ));
                    } catch (RuntimeException ignored) {}
                }
            }
        }

        // need effectively final copies for record
        final long mc = messageCount, ec = eventCount, df = decodeFailures, dcf = decompressFailures;
        final long fc = followCount, sc = shareCount, jc = joinCount, lec = liveEndedCount;
        final long cc = comboCount, ncc = nonComboCount, sf = streakFinals, nd = negativeDeltaCount;

        return new ReplayResult(
            frames.size(), mc, ec, df, dcf,
            payloadTypes, messageTypes, eventTypes,
            fc, sc, jc, lec, unknownTypes,
            likeEvents, giftGroups, cc, ncc, sf, nd
        );
    }

    // --- assertion ---

    @SuppressWarnings("unchecked")
    private static void assertReplay(String name, ReplayResult r, Map<String, Object> manifest) {
        assertLong(name, "frame_count", r.frameCount(), manifest);
        assertLong(name, "message_count", r.messageCount(), manifest);
        assertLong(name, "event_count", r.eventCount(), manifest);
        assertLong(name, "decode_failures", r.decodeFailures(), manifest);
        assertLong(name, "decompress_failures", r.decompressFailures(), manifest);

        assertCountMap(name, "payload_types", r.payloadTypes(), manifest);
        assertCountMap(name, "message_types", r.messageTypes(), manifest);
        assertCountMap(name, "event_types", r.eventTypes(), manifest);

        var subRouted = (Map<String, Object>) manifest.get("sub_routed");
        assertEquals(r.followCount(), longFrom(subRouted, "follow"), name + ": sub_routed.follow");
        assertEquals(r.shareCount(), longFrom(subRouted, "share"), name + ": sub_routed.share");
        assertEquals(r.joinCount(), longFrom(subRouted, "join"), name + ": sub_routed.join");
        assertEquals(r.liveEndedCount(), longFrom(subRouted, "live_ended"), name + ": sub_routed.live_ended");

        assertCountMap(name, "unknown_types", r.unknownTypes(), manifest);

        // like accumulator
        var likeManifest = (Map<String, Object>) manifest.get("like_accumulator");
        assertEquals(r.likeEvents().size(), longFrom(likeManifest, "event_count"), name + ": like event_count");

        long backwardsJumps = r.likeEvents().stream().filter(LikeEventRecord::wentBackwards).count();
        assertEquals(backwardsJumps, longFrom(likeManifest, "backwards_jumps"), name + ": like backwards_jumps");

        if (!r.likeEvents().isEmpty()) {
            var last = r.likeEvents().getLast();
            assertEquals(last.accTotal(), intFrom(likeManifest, "final_max_total"), name + ": like final_max_total");
            assertEquals(last.accumulated(), longFrom(likeManifest, "final_accumulated"), name + ": like final_accumulated");
        }

        boolean accMono = true, accumMono = true;
        for (int i = 1; i < r.likeEvents().size(); i++) {
            if (r.likeEvents().get(i).accTotal() < r.likeEvents().get(i - 1).accTotal()) accMono = false;
            if (r.likeEvents().get(i).accumulated() < r.likeEvents().get(i - 1).accumulated()) accumMono = false;
        }
        assertEquals(accMono, boolFrom(likeManifest, "acc_total_monotonic"), name + ": like acc_total_monotonic");
        assertEquals(accumMono, boolFrom(likeManifest, "accumulated_monotonic"), name + ": like accumulated_monotonic");

        // like event-by-event
        var likeExpected = (List<Map<String, Object>>) likeManifest.get("events");
        assertEquals(r.likeEvents().size(), likeExpected.size(), name + ": like events length");
        for (int i = 0; i < r.likeEvents().size(); i++) {
            var got = r.likeEvents().get(i);
            var exp = likeExpected.get(i);
            assertEquals(got.wireCount(), intFrom(exp, "wire_count"), name + ": like[" + i + "].wire_count");
            assertEquals(got.wireTotal(), intFrom(exp, "wire_total"), name + ": like[" + i + "].wire_total");
            assertEquals(got.accTotal(), intFrom(exp, "acc_total"), name + ": like[" + i + "].acc_total");
            assertEquals(got.accumulated(), longFrom(exp, "accumulated"), name + ": like[" + i + "].accumulated");
            assertEquals(got.wentBackwards(), boolFrom(exp, "went_backwards"), name + ": like[" + i + "].went_backwards");
        }

        // gift streaks
        var giftManifest = (Map<String, Object>) manifest.get("gift_streaks");
        assertEquals(r.comboCount() + r.nonComboCount(), longFrom(giftManifest, "event_count"), name + ": gift event_count");
        assertEquals(r.comboCount(), longFrom(giftManifest, "combo_count"), name + ": gift combo_count");
        assertEquals(r.nonComboCount(), longFrom(giftManifest, "non_combo_count"), name + ": gift non_combo_count");
        assertEquals(r.streakFinals(), longFrom(giftManifest, "streak_finals"), name + ": gift streak_finals");
        assertEquals(r.negativeDeltaCount(), longFrom(giftManifest, "negative_deltas"), name + ": gift negative_deltas");

        // gift group-by-group
        var giftGroupsExpected = (Map<String, List<Map<String, Object>>>) giftManifest.get("groups");
        assertEquals(r.giftGroups().size(), giftGroupsExpected.size(), name + ": gift groups count");
        for (var entry : r.giftGroups().entrySet()) {
            String gid = entry.getKey();
            var gotEvts = entry.getValue();
            var expEvts = giftGroupsExpected.get(gid);
            if (expEvts == null) fail(name + ": missing gift group " + gid);
            assertEquals(gotEvts.size(), expEvts.size(), name + ": gift group " + gid + " length");
            for (int i = 0; i < gotEvts.size(); i++) {
                var g = gotEvts.get(i);
                var e = expEvts.get(i);
                assertEquals(g.giftId(), intFrom(e, "gift_id"), name + ": gift[" + gid + "][" + i + "].gift_id");
                assertEquals(g.repeatCount(), intFrom(e, "repeat_count"), name + ": gift[" + gid + "][" + i + "].repeat_count");
                assertEquals(g.delta(), intFrom(e, "delta"), name + ": gift[" + gid + "][" + i + "].delta");
                assertEquals(g.isFinal(), boolFrom(e, "is_final"), name + ": gift[" + gid + "][" + i + "].is_final");
                assertEquals(g.diamondTotal(), longFrom(e, "diamond_total"), name + ": gift[" + gid + "][" + i + "].diamond_total");
            }
        }
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private static void assertCountMap(String name, String field, Map<String, Long> got, Map<String, Object> manifest) {
        var expected = (Map<String, Object>) manifest.get(field);
        var expectedLong = new TreeMap<String, Long>();
        for (var e : expected.entrySet()) {
            expectedLong.put(e.getKey(), ((Number) e.getValue()).longValue());
        }
        assertEquals(expectedLong, new TreeMap<>(got), name + ": " + field);
    }

    private static void assertLong(String name, String field, long got, Map<String, Object> manifest) {
        assertEquals(got, ((Number) manifest.get(field)).longValue(), name + ": " + field);
    }

    private static long longFrom(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).longValue();
    }

    private static int intFrom(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).intValue();
    }

    private static boolean boolFrom(Map<String, Object> m, String key) {
        return (Boolean) m.get(key);
    }

    private static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0;
    }

    // --- test runner ---

    @SuppressWarnings("unchecked")
    private void runCaptureTest(String name) throws Exception {
        Path testdata = findTestdata();
        if (testdata == null) {
            System.err.println("SKIP " + name + ": no testdata (set PIRATETOK_TESTDATA or clone live-testdata)");
            return;
        }

        Path cap = capturePath(testdata, name);
        Path man = manifestPath(testdata, name);

        if (!Files.exists(cap)) {
            System.err.println("SKIP " + name + ": capture not found at " + cap);
            return;
        }
        if (!Files.exists(man)) {
            System.err.println("SKIP " + name + ": manifest not found at " + man);
            return;
        }

        Map<String, Object> manifest = JSON.readValue(man.toFile(), Map.class);
        List<byte[]> frames = readCapture(cap);
        ReplayResult result = replay(frames);
        assertReplay(name, result, manifest);
    }

    @Test
    void replay_calvinterest6() throws Exception {
        runCaptureTest("calvinterest6");
    }

    @Test
    void replay_happyhappygaltv() throws Exception {
        runCaptureTest("happyhappygaltv");
    }

    @Test
    void replay_fox4newsdallasfortworth() throws Exception {
        runCaptureTest("fox4newsdallasfortworth");
    }
}
