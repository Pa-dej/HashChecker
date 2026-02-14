package padej;

public class RateLimiter {

    private double tokens = 5;
    private double maxTokens = 5;
    private double refillRate = 5;
    private long lastRefill = System.nanoTime();

    public synchronized void acquire() {

        refill();

        while (tokens < 1) {
            refill();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        tokens -= 1;
    }

    private void refill() {

        long now = System.nanoTime();
        double delta = (now - lastRefill) / 1_000_000_000.0;
        lastRefill = now;

        tokens = Math.min(maxTokens, tokens + delta * refillRate);
    }

    public synchronized void updateFromRemaining(int limit, int remaining) {

        if (limit <= 0) return;

        double ratio = (double) remaining / limit;

        if (ratio < 0.2) refillRate = 1.5;
        else if (ratio < 0.5) refillRate = 3;
        else refillRate = 6;
    }

    public void penalty() {
        refillRate = 0.5;
    }
}
