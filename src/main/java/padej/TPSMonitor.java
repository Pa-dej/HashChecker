package padej;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TPSMonitor {

    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger pending = new AtomicInteger(0);
    private final AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
    private volatile double tps = 0.0;
    private volatile boolean running = true;

    public void start() {
        Thread monitor = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000);
                    updateTPS();
                    printStatus();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    public void stop() {
        running = false;
    }

    public void incrementCompleted() {
        completed.incrementAndGet();
    }

    public void setPending(int count) {
        pending.set(count);
    }

    private void updateTPS() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate.get();
        
        if (elapsed > 0) {
            int done = completed.getAndSet(0);
            tps = (done * 1000.0) / elapsed;
        }
        
        lastUpdate.set(now);
    }

    private void printStatus() {
        System.out.print("\r" + Utils.cyan("TPS: " + String.format("%.2f", tps) +
                         " | Pending: " + pending.get() + "    "));
    }

    public void clearLine() {
        System.out.print("\r" + " ".repeat(80) + "\r");
    }
}
