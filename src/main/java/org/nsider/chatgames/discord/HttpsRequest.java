package org.nsider.chatgames.discord;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A minimal HTTP/1.1 client: one request per connection. It exists because editing a webhook
 * message needs PATCH, which Java 8's HttpURLConnection refuses, and every server Java (8 to
 * 25) has sockets. Plain http:// is only there for tests.
 */
final class HttpsRequest {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_RESPONSE_BYTES = 1 << 20;

    private HttpsRequest() {}

    /** A response: its status, headers (names lower-case) and body. */
    static final class Response {
        final int status;
        final Map<String, String> headers;
        final String body;

        Response(int status, Map<String, String> headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        @Nullable
        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    @Nonnull
    static Response send(@Nonnull String method, @Nonnull String url, @Nullable String json) throws IOException {
        URI uri = URI.create(url);
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        if (!https && !"http".equalsIgnoreCase(uri.getScheme())) throw new IOException("not an http(s) URL");
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : https ? 443 : 80;
        String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        byte[] body = json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);

        try (Socket socket = open(host, port, https)) {
            StringBuilder request = new StringBuilder();
            request.append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
                    .append("Host: ").append(host).append(port == (https ? 443 : 80) ? "" : ":" + port).append("\r\n")
                    .append("User-Agent: ChatGames-Premium (https://www.spigotmc.org/resources/107781/)\r\n")
                    .append("Accept: application/json\r\n")
                    .append("Connection: close\r\n");
            if (json != null) request.append("Content-Type: application/json; charset=utf-8\r\n");
            request.append("Content-Length: ").append(body.length).append("\r\n\r\n");
            OutputStream out = socket.getOutputStream();
            out.write(request.toString().getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
            return read(socket.getInputStream());
        }
    }

    private static Socket open(String host, int port, boolean https) throws IOException {
        Socket plain = new Socket();
        try {
            plain.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            plain.setSoTimeout(READ_TIMEOUT_MILLIS);
            if (!https) return plain;
            // Layered over the connected socket so the connect timeout applies; the host name gives SNI
            SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(plain, host, port, true);
            // A layered socket checks the certificate chain but not that it is for this host; this adds that check
            SSLParameters params = tls.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            tls.setSSLParameters(params);
            tls.startHandshake();
            return tls;
        } catch (IOException | RuntimeException e) {
            plain.close();
            throw e;
        }
    }

    /** Reads to the end (the server closes the connection), then splits headers and body. */
    private static Response read(InputStream in) throws IOException {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            all.write(buffer, 0, n);
            if (all.size() > MAX_RESPONSE_BYTES) throw new IOException("response too large");
        }
        byte[] bytes = all.toByteArray();
        int split = indexOf(bytes, new byte[]{'\r', '\n', '\r', '\n'});
        if (split < 0) throw new IOException("incomplete response");
        String[] lines = new String(bytes, 0, split, StandardCharsets.ISO_8859_1).split("\r\n");
        String[] status = lines[0].split(" ", 3);
        if (status.length < 2 || !status[0].startsWith("HTTP/")) throw new IOException("not an HTTP response");
        int code;
        try {
            code = Integer.parseInt(status[1]);
        } catch (NumberFormatException e) {
            throw new IOException("bad status line: " + lines[0]);
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim());
        }
        byte[] body = Arrays.copyOfRange(bytes, split + 4, bytes.length);
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) body = unchunk(body);
        return new Response(code, headers, new String(body, StandardCharsets.UTF_8));
    }

    static byte[] unchunk(byte[] body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = 0;
        while (i < body.length) {
            int lineEnd = indexOf(body, new byte[]{'\r', '\n'}, i);
            if (lineEnd < 0) throw new IOException("bad chunked body");
            String sizeText = new String(body, i, lineEnd - i, StandardCharsets.US_ASCII);
            int semicolon = sizeText.indexOf(';');
            int size;
            try {
                size = Integer.parseInt((semicolon >= 0 ? sizeText.substring(0, semicolon) : sizeText).trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("bad chunk size");
            }
            if (size == 0) break;
            int start = lineEnd + 2;
            if (start + size > body.length) throw new IOException("truncated chunk");
            out.write(body, start, size);
            i = start + size + 2;
        }
        return out.toByteArray();
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        return indexOf(data, pattern, 0);
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
