import com.piratetok.live.PirateTokClient;
import com.piratetok.live.Errors.PirateTokException;
import com.piratetok.live.Errors.SessionRequiredException;
import java.time.Duration;

/**
 * Fetches the full viewer roster of a live room, then exits.
 * TikTok gates this endpoint behind a login, so session cookies are required:
 * {@code Audience <username> "sessionid=abc; sid_tt=abc"}
 */
public class Audience {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) { System.out.println("usage: Audience <username> \"sessionid=xxx; sid_tt=xxx\""); return; }
        Duration t = Duration.ofSeconds(10);
        try {
            var room = PirateTokClient.checkOnline(args[0], t);
            var audience = PirateTokClient.fetchRoomAudience(room.roomId(), room.anchorId(), t, args[1]);
            System.out.println("@" + args[0] + " — " + audience.total() + " in room (" + audience.anonymous()
                + " anonymous), " + audience.viewers().size() + " listed");
            for (var v : audience.viewers()) {
                System.out.println("#" + v.rank() + " @" + v.username() + " (" + v.nickname() + ") score=" + v.score()
                    + " followers=" + v.followerCount() + (v.isSubscriber() ? " [sub]" : "")
                    + (v.isFollower() ? " [follower]" : ""));
            }
        } catch (SessionRequiredException e) {
            System.out.println(e.getMessage());
            System.out.println("hint: copy sessionid + sid_tt from browser DevTools while logged in");
        } catch (PirateTokException e) {
            System.out.println("error: " + e.getMessage());
        }
    }
}
