import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;
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

        client.on(EventType.CONNECTED, e -> System.out.println("tracking gifts in room " + e.data().get("roomId")));

        client.on(EventType.GIFT, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            @SuppressWarnings("unchecked") var gift = (Map<String,Object>) e.data().getOrDefault("gift", Map.of());
            String nick = String.valueOf(user.getOrDefault("uniqueId", "?"));
            long diamonds = ((Number) gift.getOrDefault("diamondCount", 0)).longValue();
            long count = ((Number) e.data().getOrDefault("repeatCount", 1)).longValue();
            var stats = gifts.computeIfAbsent(nick, k -> new GiftStats());
            stats.count().addAndGet(count);
            stats.diamonds().addAndGet(diamonds * count);
            System.out.println("  " + nick + " -> " + gift.getOrDefault("name", "?") + " x" + count);
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
