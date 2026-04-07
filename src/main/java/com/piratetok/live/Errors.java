package com.piratetok.live;

public final class Errors {

    public static class PirateTokException extends RuntimeException {
        public PirateTokException(String message) { super(message); }
        public PirateTokException(String message, Throwable cause) { super(message, cause); }
    }

    public static class UserNotFoundException extends PirateTokException {
        public final String username;
        public UserNotFoundException(String username) {
            super("user \"" + username + "\" does not exist");
            this.username = username;
        }
    }

    public static class HostNotOnlineException extends PirateTokException {
        public final String username;
        public HostNotOnlineException(String username) {
            super("user \"" + username + "\" is not currently live");
            this.username = username;
        }
    }

    public static class TikTokBlockedException extends PirateTokException {
        public final int statusCode;
        public TikTokBlockedException(int statusCode) {
            super("tiktok blocked (HTTP " + statusCode + ")");
            this.statusCode = statusCode;
        }
    }

    public static class TikTokApiException extends PirateTokException {
        public final long code;
        public TikTokApiException(long code) {
            super("tiktok API error: statusCode=" + code);
            this.code = code;
        }
    }

    public static class AgeRestrictedException extends PirateTokException {
        public AgeRestrictedException() {
            super("age-restricted stream: 18+ room — pass session cookies to fetchRoomInfo()");
        }
    }

    public static class DeviceBlockedException extends PirateTokException {
        public DeviceBlockedException() {
            super("device blocked — ttwid was flagged, fetch a fresh one");
        }
    }

    public static class ProfilePrivateException extends PirateTokException {
        public final String username;
        public ProfilePrivateException(String username) {
            super("profile is private: @" + username);
            this.username = username;
        }
    }

    public static class ProfileNotFoundException extends PirateTokException {
        public final String username;
        public ProfileNotFoundException(String username) {
            super("profile not found: @" + username);
            this.username = username;
        }
    }

    public static class ProfileScrapeException extends PirateTokException {
        public ProfileScrapeException(String reason) {
            super("failed to scrape profile: " + reason);
        }
    }

    public static class ProfileErrorException extends PirateTokException {
        public final long code;
        public ProfileErrorException(long code) {
            super("profile fetch error: statusCode=" + code);
            this.code = code;
        }
    }

    private Errors() {}
}
