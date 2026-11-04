package ch.survalit.wallpi.wallpapers.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import module jdk.httpserver;

/// Stands in for Wallhaven and Unsplash: canned replies per path, every request recorded.
class ImageApiStub implements AutoCloseable {

    record Reply(int status, String body, Map<String, String> headers) {}

    record Received(String path, String query, Headers headers) {

        boolean hasParameter(String parameter) {
            return List.of(query.split("&")).contains(parameter);
        }
    }

    static final Reply NOT_FOUND = new Reply(404, "{}", Map.of());

    HttpServer server;
    Map<String, Reply> replies = new ConcurrentHashMap<>();
    List<Received> received = new CopyOnWriteArrayList<>();

    ImageApiStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    void reply(String path, String body) {
        reply(path, 200, body, Map.of());
    }

    void reply(String path, int status, String body, Map<String, String> headers) {
        replies.put(path, new Reply(status, body, headers));
    }

    Received last(String path) {
        return received.stream()
                .filter(request -> request.path().equals(path))
                .reduce((_, later) -> later)
                .orElseThrow(() -> new AssertionError("no request to " + path + ", got " + received));
    }

    void handle(HttpExchange exchange) throws IOException {
        var uri = exchange.getRequestURI();
        received.add(new Received(uri.getPath(), String.valueOf(uri.getRawQuery()), exchange.getRequestHeaders()));
        var reply = replies.getOrDefault(uri.getPath(), NOT_FOUND);
        var body = reply.body().getBytes(StandardCharsets.UTF_8);
        reply.headers().forEach(exchange.getResponseHeaders()::add);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
