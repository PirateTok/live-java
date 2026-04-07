import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;
import com.piratetok.live.helpers.GiftStreakTracker;

public class GiftStreak {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: GiftStreak <username>"); return; }
        var client = new PirateTokClient(args[0]).cdnEU();
        var tracker = new GiftStreakTracker();

        client.on(EventType.CONNECTED, e ->
            System.out.println("tracking gift streaks in room " + e.data().get("roomId")));

        client.on(EventType.GIFT, e -> {
            var enriched = tracker.process(e.data());
            String status = enriched.isFinal() ? "FINAL" : "active";
            System.out.printf("  streak %d [%s] +%d gifts (+%d diamonds) | total: %d gifts (%d diamonds)%n",
                enriched.streakId(), status,
                enriched.eventGiftCount(), enriched.eventDiamondCount(),
                enriched.totalGiftCount(), enriched.totalDiamondCount());
        });

        client.on(EventType.DISCONNECTED, e ->
            System.out.println("disconnected — " + tracker.activeStreaks() + " streaks were still active"));

        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        client.connect();
    }
}
