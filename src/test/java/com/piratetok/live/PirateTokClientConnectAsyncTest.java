package com.piratetok.live;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PirateTokClientConnectAsyncTest {

    @Test
    void connectAsync_nullExecutor_throws() {
        var client = new PirateTokClient("user");
        assertThrows(NullPointerException.class, () -> client.connectAsync(null));
    }
}
