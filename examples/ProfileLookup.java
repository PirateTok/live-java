import com.piratetok.live.Errors.ProfilePrivateException;
import com.piratetok.live.Errors.ProfileNotFoundException;
import com.piratetok.live.helpers.ProfileCache;

public class ProfileLookup {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: ProfileLookup <username> [username2] ...");
            return;
        }

        var cache = new ProfileCache();

        for (String username : args) {
            System.out.println("Fetching profile for @" + username + "...");
            try {
                var p = cache.fetch(username);
                String room = p.roomId().isEmpty() ? "(offline)" : p.roomId();
                String link = p.bioLink() != null ? p.bioLink() : "(none)";
                System.out.println("  User ID:    " + p.userId());
                System.out.println("  Nickname:   " + p.nickname());
                System.out.println("  Verified:   " + p.verified());
                System.out.println("  Followers:  " + p.followerCount());
                System.out.println("  Videos:     " + p.videoCount());
                System.out.println("  Avatar (thumb):  " + p.avatarThumb());
                System.out.println("  Avatar (720):    " + p.avatarMedium());
                System.out.println("  Avatar (1080):   " + p.avatarLarge());
                System.out.println("  Bio link:   " + link);
                System.out.println("  Room ID:    " + room);
            } catch (ProfilePrivateException e) {
                System.out.println("  [PRIVATE] " + e.getMessage());
            } catch (ProfileNotFoundException e) {
                System.out.println("  [NOT FOUND] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  [ERROR] " + e.getMessage());
            }
            System.out.println();
        }

        String first = args[0];
        System.out.println("Fetching @" + first + " again (should be cached)...");
        try {
            var p = cache.fetch(first);
            System.out.println("  [cached] " + p.nickname() + " — " + p.followerCount() + " followers");
        } catch (Exception e) {
            System.out.println("  [cached error] " + e.getMessage());
        }
    }
}
