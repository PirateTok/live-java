import com.piratetok.live.http.Api;
import com.piratetok.live.Errors.HostNotOnlineException;
import com.piratetok.live.Errors.UserNotFoundException;
import com.piratetok.live.Errors.TikTokBlockedException;
import java.time.Duration;

public class OnlineCheck {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: OnlineCheck <username>"); return; }
        try {
            var r = Api.checkOnline(args[0], Duration.ofSeconds(10));
            System.out.println("LIVE  " + args[0] + "  room_id=" + r.roomId());
        } catch (HostNotOnlineException e) {
            System.out.println("OFF   " + args[0]);
        } catch (UserNotFoundException e) {
            System.out.println("404   " + args[0] + " does not exist");
        } catch (TikTokBlockedException e) {
            System.out.println("BLOCKED  HTTP " + e.statusCode);
        }
    }
}
