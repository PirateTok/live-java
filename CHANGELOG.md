# Changelog

## 0.2.0

- ttwid fetch retries up to 8× (750 ms apart) when TikTok omits the cookie; transport errors still propagate.
- Reconnect loop: a ttwid or WSS failure is a failed attempt (`reconnecting`, backoff) instead of aborting `connect()`.
- ttwid + UA are reused across reconnects and rotated only on DEVICE_BLOCKED or a connection that died within 30 s.
- `maxRetries` counts consecutive failures; a 30 s healthy session resets the count.
- `connect()` now drives the same loop as `connectAsync()` (one implementation).
- `heartbeatInterval(Duration)` builder (default 10 s), also fed into the `heartbeat_duration` WSS URL param.
  `WssUrl.build` / `Wss.connect[Async]` take the interval.
- `RoomIdResult.anchorId()`; `RoomInfo.rawJson()`; check_online maps empty / non-JSON responses to `TikTokBlockedException`.
- RoomUserSeq decodes `ranksList`, `seatsList`, `anonymous`; fields renamed `viewerCount` / `totalUser`.
  New `RoomUserSeq.topViewers(data)`.
- `fetchRoomAudience` (full viewer roster, login-gated) + `SessionRequiredException` / `InvalidResponseException`; `Audience` example.
- `unknown` events carry the raw `payload` bytes; `WebcastRoomVerifyMessage` routed.
- Replay tests fail on missing testdata instead of passing silently; `make build` delegates to Maven.
