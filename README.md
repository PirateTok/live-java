<p align="center">
  <img src="https://raw.githubusercontent.com/PirateTok/.github/main/profile/assets/og-banner-v2.png" alt="PirateTok" width="640" />
</p>

# com.piratetok:live

Connect to any TikTok Live stream and receive real-time events in Java. No signing server, no API keys, no authentication required.

```java
import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;

var client = new PirateTokClient("username_here");

client.on(EventType.CHAT, e -> {
    var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
    System.out.println("[chat] " + user.getOrDefault("uniqueId", "?") + ": " + e.data().get("content"));
});

client.on(EventType.GIFT, e -> {
    var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
    var gift = (Map<String,Object>) e.data().getOrDefault("gift", Map.of());
    System.out.println("[gift] " + user.getOrDefault("uniqueId", "?") + " sent " + gift.getOrDefault("name", "?") + " (" + gift.getOrDefault("diamondCount", 0) + " diamonds)");
});

client.on(EventType.LIKE, e -> {
    var user = (Map<String,Object>) e.data().getOrDefault("user", Map.of());
    System.out.println("[like] " + user.getOrDefault("uniqueId", "?") + " (" + e.data().get("total") + " total)");
});

// Connect — blocks, handles auth, room resolution, WSS, heartbeat, reconnection
client.connect();
```

## Install

Requires Java >= 25. Single runtime dependency: Jackson (`jackson-databind`).

**Maven**:

```
mvn -q package dependency:copy-dependencies
```

The JAR is `target/live-0.1.3.jar`, dependencies are in `target/dependency/`.

## Other languages

| Language | Install | Repo |
|:---------|:--------|:-----|
| **Rust** | `cargo add piratetok-live-rs` | [live-rs](https://github.com/PirateTok/live-rs) |
| **Go** | `go get github.com/PirateTok/live-go` | [live-go](https://github.com/PirateTok/live-go) |
| **Python** | `pip install piratetok-live-py` | [live-py](https://github.com/PirateTok/live-py) |
| **JavaScript** | `npm install piratetok-live-js` | [live-js](https://github.com/PirateTok/live-js) |
| **C#** | `dotnet add package PirateTok.Live` | [live-cs](https://github.com/PirateTok/live-cs) |
| **Lua** | `luarocks install piratetok-live-lua` | [live-lua](https://github.com/PirateTok/live-lua) |
| **Elixir** | `{:piratetok_live, "~> 0.1"}` | [live-ex](https://github.com/PirateTok/live-ex) |
| **Dart** | `dart pub add piratetok_live` | [live-dart](https://github.com/PirateTok/live-dart) |
| **C** | `#include "piratetok.h"` | [live-c](https://github.com/PirateTok/live-c) |
| **PowerShell** | `Install-Module PirateTok.Live` | [live-ps1](https://github.com/PirateTok/live-ps1) |
| **Shell** | `bpkg install PirateTok/live-sh` | [live-sh](https://github.com/PirateTok/live-sh) |

## Features

- **Zero signing dependency** -- no API keys, no signing server, no external auth
- **Hand-written protobuf codec** -- no `.proto` files, no codegen, no protobuf-java
- **Jackson for JSON** -- TikTok API responses parsed via Jackson with input limits
- **64 decoded event types** -- chat, gifts, likes, joins, follows, shares, battles, polls, and more
- **Async API** -- `connectAsync()` for non-blocking multi-room connections
- **Auto-reconnection** -- stale detection, exponential backoff, self-healing auth
- **DEVICE_BLOCKED handling** -- detects handshake block, rotates ttwid + UA, retries
- **Enriched User data** -- badges, gifter level, moderator status, follow info, fan club
- **Sub-routed convenience events** -- `follow`, `share`, `join`, `liveEnded`
- **System locale detection** -- language/region auto-detected, threaded into all API calls
- **Input limits** -- gzip bomb protection, WSS frame caps, proto nesting/field limits
- **Helpers** -- `GiftStreakTracker`, `LikeAccumulator`, `ProfileCache` (stateful, optional)

## Configuration

```java
var client = new PirateTokClient("username_here")
    .cdnEU()                              // EU CDN (default: global)
    .cdnUS()                              // US CDN
    .cdn("webcast-ws.eu.tiktok.com")      // custom CDN host
    .timeout(Duration.ofSeconds(15))      // HTTP timeout
    .maxRetries(10)                       // reconnect attempts
    .staleTimeout(Duration.ofSeconds(90)) // no-data timeout
    .language("en")                       // override detected language
    .region("US")                         // override detected region
    .userAgent("custom UA")               // override random UA
    .cookies("sessionid=abc; sid_tt=abc") // session cookies (18+ room info only)
    .proxy("http://127.0.0.1:8080")      // HTTP/HTTPS/SOCKS5 proxy for all traffic
    .compress(false);                     // disable gzip compression for WSS payloads (trades bandwidth for CPU)

// Stop a running connection (from another thread)
client.disconnect();
```

## Async (non-blocking)

```java
// Single stream
CompletableFuture<String> session = client.connectAsync();

// Multiple streams on virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = usernames.stream()
        .map(u -> new PirateTokClient(u).connectAsync(executor))
        .toList();
    // ...
}
```

## Room info (optional, separate call)

```java
import com.piratetok.live.http.Api;

// Check if user is live
var result = Api.checkOnline("username_here", Duration.ofSeconds(10));

// Fetch room metadata
var info = Api.fetchRoomInfo(result.roomId(), Duration.ofSeconds(10), null);

// 18+ rooms -- pass session cookies from browser DevTools
var info = Api.fetchRoomInfo(result.roomId(), Duration.ofSeconds(10),
    "sessionid=abc; sid_tt=abc");
```

## How it works

1. Resolves username to room ID via TikTok JSON API
2. Fetches a fresh `ttwid` cookie (unauthenticated GET)
3. Opens a direct WSS connection via `java.net.http.WebSocket`
4. Sends protobuf heartbeats every 10s to keep alive
5. Decodes protobuf event stream via hand-written codec
6. Auto-reconnects on stale/dropped connections with fresh ttwid + rotated UA
7. On `DEVICE_BLOCKED` handshake response, retries with 2s delay

## Examples

```bash
mvn -q package dependency:copy-dependencies
java -cp target/classes:target/dependency/* BasicChat <username>
java -cp target/classes:target/dependency/* OnlineCheck <username>
java -cp target/classes:target/dependency/* StreamInfo <username>
java -cp target/classes:target/dependency/* GiftTracker <username>
java -cp target/classes:target/dependency/* GiftStreak <username>
java -cp target/classes:target/dependency/* LikeDebug <username>
java -cp target/classes:target/dependency/* ProfileLookup <username>
```

## Integration tests

Tests call TikTok over the network. **Skipped unless environment variables are set**, so `mvn test` stays green in CI.

| Variable | Required | Purpose |
|:---------|:---------|:--------|
| `PIRATETOK_LIVE_TEST_USER` | for HTTP + WSS tests | Username that is **live** for the whole run |
| `PIRATETOK_LIVE_TEST_OFFLINE_USER` | optional | Username that must **not** be live |
| `PIRATETOK_LIVE_TEST_COOKIES` | optional | Cookie header for `fetchRoomInfo` on age-restricted rooms |
| `PIRATETOK_LIVE_TEST_HTTP` | optional (`1`) | Enables HTTP probe with synthetic username |
| `PIRATETOK_LIVE_TEST_USERS` | optional | Comma-separated usernames for multi-stream load test |

```bash
PIRATETOK_LIVE_TEST_USER=some_live_creator mvn test
```

## Replay tests

Deterministic cross-lib validation against binary WSS captures. Requires testdata from a separate repo:

```bash
git clone https://github.com/PirateTok/live-testdata testdata
mvn test
```

Tests skip gracefully if testdata is not found. You can also set `PIRATETOK_TESTDATA` to point to a custom location.

## Scheduler tuning

The shared WSS scheduler handles heartbeats and stale checks for all connections. Default pool size is 4 threads.

```bash
export PIRATETOK_WSS_SCHEDULER_THREADS=64  # for high connection counts
```

## License

0BSD
