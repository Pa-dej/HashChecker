package padej;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TaskQueue {

    private final ExecutorService executor;
    private final List<CompletableFuture<Boolean>> futures = new ArrayList<>();

    public TaskQueue(int parallelism) {
        this.executor = Executors.newFixedThreadPool(parallelism);
    }

    public void submit(Callable<Boolean> task) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                return false;
            }
        }, executor);
        
        futures.add(future);
    }

    public Stats awaitAll() {
        Stats stats = new Stats();
        
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (future.get()) {
                    stats.ok++;
                } else {
                    stats.fail++;
                }
            } catch (Exception e) {
                stats.fail++;
            }
        }
        
        return stats;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
