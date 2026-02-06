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
    private String lastReset = "?";

    public BatchModrinthClient(TPSMonitor monitor) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.monitor = monitor;
    }

    public Map<String, Boolean> checkBatch(List<Path> files) throws Exception {
        
        List<String> hashes = new ArrayList<>();
        Map<String, Path> hashToFile = new HashMap<>();

        for (Path file : files) {
            String hash = Utils.sha1(file);
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
        body.addProperty("algorithm", "sha1");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BATCH_API))
                .header("User-Agent", "padej/hashchecker/3.0")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

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
            System.out.println(Utils.red("[429 RATE LIMIT] Retrying..."));
            Thread.sleep(2000);
            return checkBatch(files);
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

    public void printLastKnownLimit() {
        System.out.println();
        System.out.println(Utils.cyan("LIMIT STATUS"));
        System.out.println("Limit: " + lastLimit +
                " | Remaining: " + lastRemaining +
                " | Reset: " + lastReset);
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
