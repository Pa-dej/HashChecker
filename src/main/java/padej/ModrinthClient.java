package padej;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;

public class ModrinthClient {

    private static final String API =
            "https://api.modrinth.com/v2/version_file/";

    private final HttpClient http = HttpClient.newHttpClient();
    private final RateLimiter limiter = new RateLimiter();
    private final TPSMonitor monitor;

    private int lastLimit = -1;
    private int lastRemaining = -1;
    private int lastResetSeconds = -1;
    private int totalApiCalls = 0;

    public ModrinthClient(TPSMonitor monitor) {
        this.monitor = monitor;
    }

    public ModrinthClient() {
        this.monitor = null;
    }

    public boolean checkFile(Path file) throws Exception {

        limiter.acquire();

        String hash = Utils.sha1(file);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API + hash))
                .header("User-Agent", "Pa-dej/HashChecker/1.0.0 (github.com/Pa-dej/HashChecker2)")
                .GET()
                .build();

        HttpResponse<String> resp =
                http.send(req, HttpResponse.BodyHandlers.ofString());

        totalApiCalls++;
        updateRateInfo(resp);

        if (resp.statusCode() == 200) {
            if (monitor != null) monitor.clearLine();
            System.out.println(Utils.green("[OK] ") + file.getFileName());
            return true;
        }

        if (resp.statusCode() == 404) {
            if (monitor != null) monitor.clearLine();
            System.out.println(Utils.yellow("[NOT FOUND] ") + file.getFileName());
            return false;
        }

        if (resp.statusCode() == 429) {
            limiter.penalty();
            if (monitor != null) monitor.clearLine();
            System.out.println(Utils.red("[429 RATE LIMIT] Retrying..."));
            Thread.sleep(2000);
            return checkFile(file);
        }

        if (monitor != null) monitor.clearLine();
        System.out.println(Utils.red("[HTTP " + resp.statusCode() + "]"));
        return false;
    }

    private void updateRateInfo(HttpResponse<?> resp) {

        resp.headers().firstValue("x-ratelimit-limit")
                .ifPresent(v -> lastLimit = Integer.parseInt(v));

        resp.headers().firstValue("x-ratelimit-remaining")
                .ifPresent(v -> {
                    lastRemaining = Integer.parseInt(v);
                    limiter.updateFromRemaining(lastLimit, lastRemaining);
                });

        resp.headers().firstValue("x-ratelimit-reset")
                .ifPresent(v -> {
                    try {
                        lastResetSeconds = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        lastResetSeconds = -1;
                    }
                });
    }

    public void printLimit() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/sodium"))
                .header("User-Agent", "Pa-dej/HashChecker/1.0.0 (github.com/Pa-dej/HashChecker2)")
                .GET()
                .build();

        HttpResponse<String> resp =
                http.send(req, HttpResponse.BodyHandlers.ofString());

        updateRateInfo(resp);
        printLastKnownLimit();
    }

    public void printLastKnownLimit() {

        System.out.println();
        System.out.println(Utils.cyan("RATE LIMIT STATUS"));
        
        if (lastLimit <= 0 || lastRemaining < 0) {
            System.out.println("No rate limit information available");
            return;
        }
        
        int used = lastLimit - lastRemaining;
        double usagePercent = (used * 100.0) / lastLimit;
        
        String resetStr;
        if (lastResetSeconds == 0) {
            resetStr = "now";
        } else if (lastResetSeconds > 0) {
            if (lastResetSeconds >= 60) {
                int minutes = lastResetSeconds / 60;
                int seconds = lastResetSeconds % 60;
                resetStr = String.format("%dm %ds", minutes, seconds);
            } else {
                resetStr = String.format("%ds", lastResetSeconds);
            }
        } else {
            resetStr = "unknown";
        }
        
        System.out.println(String.format("API calls made: %d | Used: %d/%d (%.1f%%) | Remaining: %d | Reset in: %s",
                totalApiCalls, used, lastLimit, usagePercent, lastRemaining, resetStr));
    }
}
