import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;
import com.piratetok.live.events.RoomUserSeq;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BasicChat {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: BasicChat <username>"); return; }

        var client = new PirateTokClient(args[0]).cdnEU();

        client.on(EventType.CONNECTED, e -> System.out.println("connected to room " + e.data().get("roomId")));

        client.on(EventType.CHAT, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[chat] " + user.getOrDefault("uniqueId", "?") + ": " + e.data().get("content"));
        });

        client.on(EventType.GIFT, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            @SuppressWarnings("unchecked") var gift = (Map<String,Object>) e.data().getOrDefault("gift", Map.of());
            System.out.println("[gift] " + user.getOrDefault("uniqueId", "?") + " sent " + gift.getOrDefault("name", "?"));
        });

        client.on(EventType.FOLLOW, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[follow] " + user.getOrDefault("uniqueId", "?"));
        });

        client.on(EventType.JOIN, e -> {
            @SuppressWarnings("unchecked") var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[join] " + user.getOrDefault("uniqueId", "?"));
        });

        // viewerCount = in the room right now; totalUser = unique viewers over the whole stream
        client.on(EventType.ROOM_USER_SEQ, e -> {
            System.out.println("[viewers] " + e.data().get("viewerCount") + " watching, "
                + e.data().get("totalUser") + " total unique");
            for (var c : RoomUserSeq.topViewers(e.data())) {
                var user = (Map<?, ?>) c.get("user");
                System.out.println("  #" + c.get("rank") + " " + user.get("nickname") + " (" + c.get("score") + ")");
            }
        });
        client.on(EventType.LIVE_ENDED, e -> System.out.println("[stream ended]"));
        client.on(EventType.DISCONNECTED, e -> System.out.println("disconnected"));

        client.connect();
    }
}
