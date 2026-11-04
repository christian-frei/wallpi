package ch.survalit.wallpi.wallpapers.control;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import jakarta.ws.rs.ServerErrorException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import module java.net.http;

/// HTTP plumbing shared by the provider adapters; every failure becomes a rejection naming the source.
interface ImageApi {

    HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    Duration TIMEOUT = Duration.ofSeconds(20);
    int NOT_FOUND = 404;

    static HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
    }

    static HttpResponse<String> send(HttpRequest request, String sourceTitle) {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw badGateway("source '%s' is unreachable: %s".formatted(sourceTitle, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw badGateway("source '%s' request was interrupted".formatted(sourceTitle));
        }
    }

    static JsonValue json(HttpResponse<String> response, String sourceTitle) {
        if (!isSuccessful(response)) {
            throw badGateway("source '%s' answered HTTP %d".formatted(sourceTitle, response.statusCode()));
        }
        try (var reader = Json.createReader(new StringReader(response.body()))) {
            return reader.readValue();
        } catch (JsonException e) {
            throw badGateway("source '%s' answered with invalid JSON: %s".formatted(sourceTitle, e.getMessage()));
        }
    }

    static boolean isSuccessful(HttpResponse<String> response) {
        return response.statusCode() / 100 == 2;
    }

    static boolean isNotFound(HttpResponse<String> response) {
        return response.statusCode() == NOT_FOUND;
    }

    /// Keeps the source url's query and replaces the given parameters, so paging and filters stay in our hands.
    static String query(URI url, String... replaced) {
        var kept = Stream.ofNullable(url.getRawQuery())
                .flatMap(query -> Arrays.stream(query.split("&")))
                .filter(parameter -> !parameter.isBlank())
                .filter(parameter -> !isOneOf(parameter, replaced));
        return kept.collect(Collectors.joining("&"));
    }

    static boolean isOneOf(String parameter, String... names) {
        var name = parameter.split("=", 2)[0];
        return Stream.of(names).anyMatch(name::equals);
    }

    static String append(String query, String parameters) {
        return query.isEmpty() ? parameters : query + "&" + parameters;
    }

    static ServerErrorException badGateway(String reason) {
        return new ServerErrorException(reason, Reasons.of(Reasons.BAD_GATEWAY, reason));
    }
}
