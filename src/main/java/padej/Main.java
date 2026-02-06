package padej;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("java -jar app.jar <mods_folder>");
            System.out.println("java -jar app.jar --limit");
            return;
        }

        if (args[0].equalsIgnoreCase("--limit")) {
            ModrinthClient client = new ModrinthClient();
            client.printLimit();
            return;
        }

        runSingleFileMode(args[0]);
    }

    private static void runSingleFileMode(String dirPath) throws Exception {
        Path dir = Paths.get(dirPath);

        if (!Files.isDirectory(dir)) {
            System.out.println(Utils.red("Это не папка."));
            return;
        }

        System.out.println(Utils.cyan("Проверка модов: " + dir));
        System.out.println();

        List<Path> files = Files.list(dir)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.out.println(Utils.yellow("Нет файлов для проверки."));
            return;
        }

        TPSMonitor monitor = new TPSMonitor();
        monitor.start();
        monitor.setPending(files.size());

        ModrinthClient client = new ModrinthClient(monitor);
        
        long startTime = System.currentTimeMillis();
        Stats stats = new Stats();

        for (Path file : files) {
            try {
                boolean ok = client.checkFile(file);
                if (ok) stats.ok++;
                else stats.fail++;
                monitor.incrementCompleted();
                monitor.setPending(files.size() - stats.ok - stats.fail);
            } catch (Exception e) {
                stats.fail++;
                monitor.incrementCompleted();
                monitor.setPending(files.size() - stats.ok - stats.fail);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        monitor.stop();
        monitor.clearLine();

        System.out.println();
        System.out.println(Utils.green("OK: " + stats.ok));
        System.out.println(Utils.yellow("NOT FOUND: " + stats.fail));
        System.out.println(Utils.cyan("Время: " + (elapsed / 1000.0) + " сек"));
        System.out.println(Utils.cyan("Средний TPS: " + String.format("%.2f", files.size() / (elapsed / 1000.0))));

        client.printLastKnownLimit();
    }
}
