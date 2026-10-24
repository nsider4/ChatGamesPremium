package org.nsider.chatgames.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Posts through a channel webhook. Waits out Discord's rate limits: the per-webhook bucket
 * Discord reports in each response, and a 429 (tried again up to three times).
 */
final class WebhookTarget implements DiscordTarget {

    /** discord.com, discordapp.com, ptb./canary. too; an optional ?thread_id= posts into a thread */
    static final Pattern URL = Pattern.compile(
            "https://(?:(?:ptb|canary)\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)/?(\\?thread_id=\\d+)?");
    private static final int MAX_TRIES = 4;
    private static final long MAX_WAIT_MILLIS = 60_000;

    private final String channel;
    private final String base;
    private final String query;
    private long nextAllowed;

    private WebhookTarget(String channel, String base, String query) {
        this.channel = channel;
        this.base = base;
        this.query = query;
    }

    /** @return null if this isn't a webhook URL */
    @Nullable
    static WebhookTarget of(@Nonnull String channel, @Nonnull String url) {
        Matcher matcher = URL.matcher(url.trim());
        if (!matcher.matches()) return null;
        String base = "https://discord.com/api/webhooks/" + matcher.group(1) + "/" + matcher.group(2);
        return new WebhookTarget(channel, base, matcher.group(3) == null ? "" : matcher.group(3).substring(1));
    }

    /** For tests: any http(s) base URL. */
    static WebhookTarget forTest(String channel, String base) {
        return new WebhookTarget(channel, base, "");
    }

    @Nullable
    @Override
    public String send(@Nonnull DiscordMessage message) throws IOException {
        // wait=true makes Discord answer with the message, whose id the edit needs
        HttpsRequest.Response response = request("POST", base + "?wait=true" + (query.isEmpty() ? "" : "&" + query),
                message.toJson(false).toString());
        try {
            JsonElement json = new JsonParser().parse(response.body);
            if (json.isJsonObject() && ((JsonObject) json).has("id")) return ((JsonObject) json).get("id").getAsString();
        } catch (RuntimeException e) {
            // Posted, but the answer can't be read: it just won't be edited
        }
        return null;
    }

    @Override
    public void edit(@Nonnull String messageId, @Nonnull DiscordMessage message) throws IOException {
        request("PATCH", base + "/messages/" + messageId + (query.isEmpty() ? "" : "?" + query), message.toJson(true).toString());
    }

    @Nonnull
    @Override
    public String describe() {
        return "the webhook of channel '" + channel + "'";
    }

    private HttpsRequest.Response request(String method, String url, String json) throws IOException {
        for (int attempt = 1; ; attempt++) {
            long wait = nextAllowed - System.currentTimeMillis();
            if (wait > 0) sleep(Math.min(wait, MAX_WAIT_MILLIS));
            HttpsRequest.Response response = HttpsRequest.send(method, url, json);
            remember(response);
            if (response.status == 429 && attempt < MAX_TRIES) {
                sleep(Math.min(retryAfterMillis(response), MAX_WAIT_MILLIS));
                continue;
            }
            if (response.status >= 500 && attempt < 2) {
                sleep(2000);
                continue;
            }
            if (response.status >= 200 && response.status < 300) return response;
            throw new IOException(problem(response));
        }
    }

    /** Discord says how many requests are left in this webhook's window and when it resets. */
    private void remember(HttpsRequest.Response response) {
        if ("0".equals(response.header("x-ratelimit-remaining"))) {
            double reset = number(response.header("x-ratelimit-reset-after"), 1);
            nextAllowed = System.currentTimeMillis() + (long) Math.ceil(reset * 1000);
        }
    }

    static long retryAfterMillis(HttpsRequest.Response response) {
        double seconds = number(response.header("retry-after"), -1);
        try {
            JsonElement json = new JsonParser().parse(response.body);
            if (json.isJsonObject() && ((JsonObject) json).has("retry_after")) seconds = ((JsonObject) json).get("retry_after").getAsDouble();
        } catch (RuntimeException e) {
            // keep the header's value
        }
        return seconds < 0 ? 1000 : (long) Math.ceil(seconds * 1000) + 50;
    }

    private static String problem(HttpsRequest.Response response) {
        switch (response.status) {
            case 401:
            case 403:
            case 404:
                return "Discord says the webhook doesn't exist any more (HTTP " + response.status + "): make a new one and put its URL in discord.yml";
            case 400:
                return "Discord refused the message (HTTP 400): " + shorten(response.body)
                        + ". Check that discord.yml's texts and links in this message are valid";
            case 429:
                return "Discord's rate limit was hit too often in a row (HTTP 429)";
            default:
                return "HTTP " + response.status + ": " + shorten(response.body);
        }
    }

    private static String shorten(String body) {
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() > 300 ? flat.substring(0, 300) + "…" : flat;
    }

    private static double number(@Nullable String text, double fallback) {
        if (text == null) return fallback;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void sleep(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }
}
