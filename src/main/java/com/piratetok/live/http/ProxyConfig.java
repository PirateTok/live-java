package com.piratetok.live.http;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Applies a proxy URL ({@code http://[user:pass@]host[:port]}) to an {@link HttpClient.Builder}, for HTTP calls
 * and WebSockets alike.
 *
 * <p>Limits of {@code java.net.http}: only HTTP CONNECT proxies (no SOCKS — rejected loudly instead of silently
 * misused), and the JDK disables Basic proxy auth for HTTPS tunnels by default. To use {@code user:pass@} with
 * TikTok's HTTPS/WSS endpoints, start the JVM with {@code -Djdk.http.auth.tunneling.disabledSchemes=}.</p>
 */
public final class ProxyConfig {

    public static HttpClient.Builder apply(HttpClient.Builder builder, String proxyUrl) {
        URI uri = URI.create(proxyUrl);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("unsupported proxy scheme '" + scheme
                + "': java.net.http supports HTTP CONNECT proxies only (no SOCKS)");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("proxy URL has no host: " + proxyUrl);
        }
        int port = uri.getPort() != -1 ? uri.getPort() : scheme.equals("https") ? 443 : 80;
        builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(), port)));

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
            char[] pass = decode(colon < 0 ? "" : userInfo.substring(colon + 1)).toCharArray();
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return getRequestorType() == RequestorType.PROXY ? new PasswordAuthentication(user, pass) : null;
                }
            });
        }
        return builder;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private ProxyConfig() {}
}
