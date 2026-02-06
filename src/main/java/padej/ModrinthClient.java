package padej;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;

public class ModrinthClient {

    private static final String API =
            "https://api.modrinth.com/v2/version_file/";

    private final HttpClient http = HttpClient.newHttpClient();
    private final RateLimiter limiter = new RateLimiter();

    private int lastLimit = -1;
    private int lastRemaining = -1;
    private String lastReset = "?";

    public boolean checkFile(Path file) throws Exception {

        limiter.acquire();

        String hash = Utils.sha1(file);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API + hash))
                .header("User-Agent", "padej/hashchecker/2.0")
                .GET()
                .build();

        HttpResponse<String> resp =
                http.send(req, HttpResponse.BodyHandlers.ofString());

        updateRateInfo(resp);

        if (resp.statusCode() == 200) {
            System.out.println(Utils.green("[OK] ") + file.getFileName());
            return true;
        }

        if (resp.statusCode() == 404) {
            System.out.println(Utils.yellow("[NOT FOUND] ") + file.getFileName());
            return false;
        }

        if (resp.statusCode() == 429) {
            limiter.penalty();
            System.out.println(Utils.red("[429 RATE LIMIT]"));
            return false;
        }

        System.out.println(Utils.red("[HTTP " + resp.statusCode() + "]"));
        return false;
    }

    private void updateRateInfo(HttpResponse<?> resp) {

        resp.headers().firstValue("X-Ratelimit-Limit")
                .ifPresent(v -> lastLimit = Integer.parseInt(v));

        resp.headers().firstValue("X-Ratelimit-Remaining")
                .ifPresent(v -> {
                    lastRemaining = Integer.parseInt(v);
                    limiter.updateFromRemaining(lastLimit, lastRemaining);
                });

        resp.headers().firstValue("X-Ratelimit-Reset")
                .ifPresent(v -> lastReset = v);
    }

    public void printLimit() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/sodium"))
                .header("User-Agent", "padej/hashchecker/2.0")
                .GET()
                .build();

        HttpResponse<String> resp =
                http.send(req, HttpResponse.BodyHandlers.ofString());

        updateRateInfo(resp);
        printLastKnownLimit();
    }

    public void printLastKnownLimit() {

        System.out.println();
        System.out.println(Utils.cyan("LIMIT STATUS"));
        System.out.println("Limit: " + lastLimit +
                " | Remaining: " + lastRemaining +
                " | Reset: " + lastReset);
    }
}
