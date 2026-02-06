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
            System.out.println("java -jar app.jar --legacy <mods_folder>  (old single-file mode)");
            return;
        }

        if (args[0].equalsIgnoreCase("--limit")) {
            ModrinthClient client = new ModrinthClient();
            client.printLimit();
            return;
        }

        if (args[0].equalsIgnoreCase("--legacy")) {
            runLegacyMode(args);
            return;
        }

        runBatchMode(args[0]);
    }

    private static void runBatchMode(String dirPath) throws Exception {
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

        BatchModrinthClient client = new BatchModrinthClient(monitor);
        
        long startTime = System.currentTimeMillis();
        
        List<List<Path>> batches = BatchModrinthClient.partition(files, 100);
        Stats stats = new Stats();

        for (List<Path> batch : batches) {
            Map<String, Boolean> results = client.checkBatch(batch);
            
            for (Boolean result : results.values()) {
                if (result) stats.ok++;
                else stats.fail++;
            }
            
            monitor.setPending(files.size() - stats.ok - stats.fail);
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

    private static void runLegacyMode(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(Utils.red("Укажите папку для legacy режима"));
            return;
        }

        Path dir = Paths.get(args[1]);

        if (!Files.isDirectory(dir)) {
            System.out.println(Utils.red("Это не папка."));
            return;
        }

        System.out.println(Utils.cyan("LEGACY MODE - Single file processing"));
        System.out.println(Utils.cyan("Проверка модов: " + dir));

        ModrinthClient client = new ModrinthClient();
        Stats stats = new Stats();

        Files.list(dir).filter(Files::isRegularFile).forEach(path -> {
            try {
                boolean ok = client.checkFile(path);
                if (ok) stats.ok++;
                else stats.fail++;
            } catch (Exception e) {
                stats.fail++;
            }
        });

        System.out.println();
        System.out.println(Utils.cyan("ИТОГ"));
        System.out.println(Utils.green("OK: " + stats.ok));
        System.out.println(Utils.yellow("NOT FOUND: " + stats.fail));

        client.printLastKnownLimit();
    }
}
