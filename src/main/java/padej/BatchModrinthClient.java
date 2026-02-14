package padej;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class BatchModrinthClient {

    private static final String BATCH_API = "https://api.modrinth.com/v2/version_files";
    private static final int BATCH_SIZE = 100;

    private final HttpClient http;
    private final RateLimiter limiter = new RateLimiter();
    private final Gson gson = new Gson();
    private final TPSMonitor monitor;

    private int lastLimit = -1;
    private int lastRemaining = -1;
    private int lastResetSeconds = -1;
    private int totalApiCalls = 0;
    
    private static final int MAX_RETRIES = 5;

    public BatchModrinthClient(TPSMonitor monitor) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.monitor = monitor;
    }

    public Map<String, Boolean> checkBatch(List<Path> files) throws Exception {
        return checkBatch(files, 0);
    }

    private Map<String, Boolean> checkBatch(List<Path> files, int retryCount) throws Exception {
        
        if (retryCount > MAX_RETRIES) {
            throw new RuntimeException("Too many retries (429 rate limit)");
        }
        
        List<String> hashes = new ArrayList<>();
        Map<String, Path> hashToFile = new HashMap<>();

        for (Path file : files) {
            String hash = Utils.sha512(file);
            hashes.add(hash);
            hashToFile.put(hash, file);
        }

        limiter.acquire();

        JsonObject body = new JsonObject();
        JsonArray hashArray = new JsonArray();
        for (String hash : hashes) {
            hashArray.add(hash);
        }
        body.add("hashes", hashArray);
        body.addProperty("algorithm", "sha512");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BATCH_API))
                .header("User-Agent", "Pa-dej/HashChecker/1.0.0 (github.com/Pa-dej/HashChecker2)")
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        totalApiCalls++;
        updateRateInfo(resp);

        Map<String, Boolean> results = new HashMap<>();

        if (resp.statusCode() == 200) {
            JsonObject response = gson.fromJson(resp.body(), JsonObject.class);
            
            for (String hash : hashes) {
                Path file = hashToFile.get(hash);
                boolean found = response.has(hash);
                results.put(file.toString(), found);
                
                if (found) {
                    monitor.clearLine();
                    System.out.println(Utils.green("[OK] ") + file.getFileName());
                } else {
                    monitor.clearLine();
                    System.out.println(Utils.yellow("[NOT FOUND] ") + file.getFileName());
                }
                
                monitor.incrementCompleted();
            }
        } else if (resp.statusCode() == 429) {
            limiter.penalty();
            monitor.clearLine();
            System.out.println(Utils.red("[429 RATE LIMIT] Retry " + (retryCount + 1) + "/" + MAX_RETRIES));
            Thread.sleep(2000);
            return checkBatch(files, retryCount + 1);
        } else {
            monitor.clearLine();
            System.out.println(Utils.red("[HTTP " + resp.statusCode() + "]"));
            for (Path file : files) {
                results.put(file.toString(), false);
            }
        }

        return results;
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

    public static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
