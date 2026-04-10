import com.piratetok.live.http.Api;
import com.piratetok.live.Errors.HostNotOnlineException;
import com.piratetok.live.Errors.UserNotFoundException;
import com.piratetok.live.Errors.AgeRestrictedException;
import java.time.Duration;

public class StreamInfo {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("usage: StreamInfo <username> [cookies]"); return; }
        String cookies = args.length > 1 ? args[1] : "";
        Duration t = Duration.ofSeconds(10);

        try {
            var room = Api.checkOnline(args[0], t);
            System.out.println("room_id: " + room.roomId());
            var info = Api.fetchRoomInfo(room.roomId(), t, cookies);
            System.out.println("title:      " + info.title());
            System.out.println("viewers:    " + info.viewers());
            System.out.println("likes:      " + info.likes());
            System.out.println("total_user: " + info.totalUser());
            if (info.streamUrl() != null) {
                System.out.println("flv_origin: " + (info.streamUrl().flvOrigin().isEmpty() ? "(none)" : info.streamUrl().flvOrigin().substring(0, Math.min(80, info.streamUrl().flvOrigin().length())) + "..."));
                System.out.println("flv_hd:     " + (info.streamUrl().flvHd().isEmpty() ? "(none)" : "present"));
            } else {
                System.out.println("no stream URLs");
            }
        } catch (HostNotOnlineException e) { System.out.println(args[0] + " is not live"); }
        catch (UserNotFoundException e) { System.out.println(args[0] + " does not exist"); }
        catch (AgeRestrictedException e) { System.out.println("18+ room — pass cookies: sessionid=xxx;sid_tt=xxx"); }
    }
}
