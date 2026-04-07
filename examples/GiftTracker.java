import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;
import com.piratetok.live.helpers.GiftStreakTracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GiftTracker {
    record GiftStats(AtomicLong count, AtomicLong diamonds) {
        GiftStats() { this(new AtomicLong(), new AtomicLong()); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: GiftTracker <username>"); return; }
        var client = new PirateTokClient(args[0]).cdnEU();
        var gifts = new ConcurrentHashMap<String, GiftStats>();
        var tracker = new GiftStreakTracker();

        client.on(EventType.CONNECTED, e -> System.out.println("tracking gifts in room " + e.data().get("roomId")));

        client.on(EventType.GIFT, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            String nick = String.valueOf(user.getOrDefault("uniqueId", "?"));
            var enriched = tracker.process(e.data());
            var stats = gifts.computeIfAbsent(nick, k -> new GiftStats());
            stats.count().addAndGet(enriched.eventGiftCount());
            stats.diamonds().addAndGet(enriched.eventDiamondCount());
            if (enriched.eventGiftCount() > 0) {
                System.out.println("  " + nick + " -> +" + enriched.eventGiftCount()
                    + " gifts (+" + enriched.eventDiamondCount() + " diamonds)");
            }
        });

        client.on(EventType.DISCONNECTED, e -> {
            System.out.println("\n--- gift summary (" + gifts.size() + " gifters) ---");
            gifts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().diamonds().get(), a.getValue().diamonds().get()))
                .forEach(en -> System.out.println("  " + en.getKey() + ": " + en.getValue().count() + " gifts, " + en.getValue().diamonds() + " diamonds"));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        client.connect();
    }
}
