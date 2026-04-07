import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LikeDebug {
    static final long START = System.currentTimeMillis();
    static final AtomicLong accumulated = new AtomicLong();
    static final AtomicInteger maxTotal = new AtomicInteger();
    static final AtomicLong eventCount = new AtomicLong();
    static final AtomicLong backwardsCount = new AtomicLong();
    static final AtomicInteger lastTotal = new AtomicInteger();
    static final ConcurrentHashMap<String, AtomicLong> perUser = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: LikeDebug <username>"); return; }

        var client = new PirateTokClient(args[0]).cdnEU();

        System.out.println("=== LIKE DEBUG (java) — @" + args[0] + " ===");
        System.out.printf("%-6s %-8s %-10s %-12s %-12s %-12s %-5s %s%n",
            "#", "dt(s)", "delta", "wire_total", "max_total", "accum", "BACK", "user");
        System.out.println("-".repeat(90));

        client.on(EventType.CONNECTED, e ->
            System.out.println("--- connected room=" + e.data().get("roomId") + " ---"));

        client.on(EventType.LIKE, e -> {
            long n = eventCount.incrementAndGet();
            int delta = ((Number) e.data().getOrDefault("count", 0)).intValue();
            int wireTotal = ((Number) e.data().getOrDefault("total", 0)).intValue();
            long accum = accumulated.addAndGet(delta);

            int prev = lastTotal.getAndSet(wireTotal);
            boolean back = wireTotal < prev;
            if (back) backwardsCount.incrementAndGet();
            maxTotal.updateAndGet(cur -> Math.max(cur, wireTotal));

            @SuppressWarnings("unchecked")
            var user = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            String nick = String.valueOf(user.getOrDefault("nickname", "?"));
            perUser.computeIfAbsent(nick, k -> new AtomicLong()).addAndGet(delta);

            double dt = (System.currentTimeMillis() - START) / 1000.0;
            String flag = back ? "<<<" : "";

            System.out.printf("%-6d %-8.1f %-10d %-12d %-12d %-12d %-5s %s%n",
                n, dt, delta, wireTotal, maxTotal.get(), accum, flag, nick);
        });

        client.on(EventType.DISCONNECTED, e -> printSummary());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> printSummary()));
        client.connect();
    }

    static void printSummary() {
        System.out.println("\n=== SUMMARY ===");
        System.out.println("events:           " + eventCount.get());
        System.out.println("backwards jumps:  " + backwardsCount.get());
        System.out.println("max wire total:   " + maxTotal.get());
        System.out.println("accumulated:      " + accumulated.get());
        System.out.println("drift:            " + (maxTotal.get() - accumulated.get()));
        System.out.println("\n--- per-user deltas ---");
        perUser.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
            .forEach(en -> System.out.println("  " + en.getKey() + ": " + en.getValue().get()));
    }
}
