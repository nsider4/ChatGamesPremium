package org.nsider.chatgames.discord;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Against a local server standing in for Discord. */
class WebhookTargetTest {

    private HttpServer server;
    private final List<String> requests = Collections.synchronizedList(new ArrayList<String>());
    private final AtomicInteger rateLimited = new AtomicInteger();
    private volatile int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = read(exchange.getRequestBody());
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body);
            int code = status;
            byte[] answer = "{\"id\": \"987654321\"}".getBytes(StandardCharsets.UTF_8);
            if (rateLimited.getAndDecrement() > 0) {
                code = 429;
                answer = "{\"retry_after\": 0.05}".getBytes(StandardCharsets.UTF_8);
            }
            // Length 0 means chunked, as Discord answers
            exchange.sendResponseHeaders(code, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(answer);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private WebhookTarget target() {
        return WebhookTarget.forTest("main", "http://127.0.0.1:" + server.getAddress().getPort() + "/hook");
    }

    @Test
    void postsAndEditsTheSameMessage() throws IOException {
        WebhookTarget target = target();
        String id = target.send(new DiscordMessage().content("Hello ünïcode"));
        assertEquals("987654321", id);
        target.edit(id, new DiscordMessage().content("Edited"));
        assertEquals(2, requests.size());
        assertTrue(requests.get(0).startsWith("POST /hook?wait=true "), requests.get(0));
        assertTrue(requests.get(0).contains("Hello ünïcode"), "UTF-8 body");
        assertTrue(requests.get(1).startsWith("PATCH /hook/messages/987654321 "), requests.get(1));
    }

    @Test
    void waitsOutTheRateLimit() throws IOException {
        rateLimited.set(2);
        assertEquals("987654321", target().send(new DiscordMessage().content("Hi")));
        assertEquals(3, requests.size());
    }

    @Test
    void aDeletedWebhookSaysSo() {
        status = 404;
        IOException error = assertThrows(IOException.class, () -> target().send(new DiscordMessage().content("Hi")));
        assertTrue(error.getMessage().contains("doesn't exist"), error.getMessage());
    }

    @Test
    void onlyDiscordWebhookUrlsAreAccepted() {
        assertNotNull(WebhookTarget.of("main", "https://discord.com/api/webhooks/123/abc-DEF_1"));
        assertNotNull(WebhookTarget.of("main", "https://canary.discordapp.com/api/webhooks/123/abc?thread_id=456"));
        assertNull(WebhookTarget.of("main", "https://example.com/api/webhooks/123/abc"));
        assertNull(WebhookTarget.of("main", "paste your webhook here"));
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
