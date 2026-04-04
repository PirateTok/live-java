<p align="center">
  <img src="https://raw.githubusercontent.com/PirateTok/.github/main/profile/assets/og-banner-v2.png" alt="PirateTok" width="640" />
</p>

# com.piratetok:live

Connect to any TikTok Live stream and receive real-time events in Java. No signing server, no API keys, no authentication required.

```java
import com.piratetok.live.PirateTokClient;
import com.piratetok.live.events.EventType;

// Create client — zero external dependencies, hand-written protobuf codec
var client = new PirateTokClient("username_here");

// Register event handlers — data arrives as decoded Maps
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
    System.out.println("[like] " + user.getOrDefault("uniqueId", "?") + " (" + e.data().get("totalLikes") + " total)");
});

// Connect — blocks, handles auth, room resolution, WSS, heartbeat, reconnection
client.connect();
```

## Install

Requires Java >= 25. No external runtime dependencies.

**Maven** (recommended):

```
mvn -q package
```

The JAR is `target/live-0.1.0-SNAPSHOT.jar`. Compiled classes (including examples) are under `target/classes`.

**Make** (plain `javac`, no Maven):

```
make build
```

Output directory is `out/`.

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
- **Zero external dependencies** -- hand-written protobuf codec, raw `java.net.http` WebSocket
- **64 decoded event types** -- chat, gifts, likes, joins, follows, shares, battles, polls, and more
- **Auto-reconnection** -- stale detection, exponential backoff, self-healing auth
- **Enriched User data** -- badges, gifter level, moderator status, follow info, fan club
- **Sub-routed convenience events** -- `follow`, `share`, `join`, `liveEnded`

## Configuration

```java
var client = new PirateTokClient("username_here")
    .cdnEU()
    .timeout(Duration.ofSeconds(15))
    .maxRetries(10)
    .staleTimeout(Duration.ofSeconds(90));
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
2. Authenticates and opens a direct WSS connection via `java.net.http.WebSocket`
3. Sends protobuf heartbeats every 10s to keep alive
4. Decodes protobuf event stream via hand-written codec
5. Auto-reconnects on stale/dropped connections with fresh credentials

All protobuf encoding/decoding is hand-written -- no `.proto` files, no codegen, no protobuf-java dependency.

## Examples

```bash
mvn -q package
java -cp target/classes BasicChat <username>       # connect + print chat events
java -cp target/classes OnlineCheck <username>     # check if user is live
java -cp target/classes StreamInfo <username>      # fetch room metadata + stream URLs
java -cp target/classes GiftTracker <username>     # track gifts with diamond totals
```

With **Make** instead of Maven, use `make build` and `-cp out`.

## Known gaps

- Proxy transport support is not wired yet.
- Explicit `DEVICE_BLOCKED` handshake handling is not implemented yet.

## License

0BSD
