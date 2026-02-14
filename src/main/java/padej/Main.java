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
            System.out.println("java -jar app.jar --single <mods_folder>  (single-file mode)");
            return;
        }

        if (args[0].equalsIgnoreCase("--limit")) {
            ModrinthClient client = new ModrinthClient();
            client.printLimit();
            return;
        }

        if (args[0].equalsIgnoreCase("--single")) {
            if (args.length < 2) {
                System.out.println(Utils.red("Please specify mods folder for single mode"));
                return;
            }
            runSingleFileMode(args[1]);
            return;
        }

        runBatchMode(args[0]);
    }

    private static void runBatchMode(String dirPath) throws Exception {
        Path dir = Paths.get(dirPath);

        if (!Files.isDirectory(dir)) {
            System.out.println(Utils.red("Not a directory."));
            return;
        }

        // Canonical path check - защита от symlink attacks
        Path canonicalDir = dir.toRealPath();
        System.out.println(Utils.cyan("Checking mods: " + canonicalDir));
        System.out.println();

        // Фильтруем только .jar файлы и проверяем symlinks
        List<Path> files = Files.list(canonicalDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                .filter(p -> {
                    try {
                        // Защита от symlink атак - файл должен быть внутри директории
                        return p.toRealPath().startsWith(canonicalDir);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.out.println(Utils.yellow("No .jar files found."));
            return;
        }

        System.out.println(Utils.cyan("Found .jar files: " + files.size()));
        System.out.println();

        TPSMonitor monitor = new TPSMonitor();
        monitor.start();
        monitor.setPending(files.size());

        BatchModrinthClient client = new BatchModrinthClient(monitor);
        
        long startTime = System.currentTimeMillis();
        
        List<List<Path>> batches = BatchModrinthClient.partition(files, 100);
        Stats stats = new Stats();
        int unknownFiles = 0;

        for (List<Path> batch : batches) {
            Map<String, Boolean> results = client.checkBatch(batch);
            
            for (Boolean result : results.values()) {
                if (result) stats.ok++;
                else {
                    stats.fail++;
                    unknownFiles++;
                }
            }
            
            monitor.setPending(files.size() - stats.ok - stats.fail);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        monitor.stop();
        monitor.clearLine();

        System.out.println();
        System.out.println(Utils.green("VERIFIED: " + stats.ok));
        System.out.println(Utils.yellow("UNKNOWN: " + stats.fail));
        
        if (unknownFiles > 0) {
            System.out.println();
            System.out.println(Utils.red("WARNING: " + unknownFiles + " unknown/modified files detected!"));
            System.out.println(Utils.yellow("These files are NOT verified by Modrinth:"));
            System.out.println(Utils.yellow("- Private mods"));
            System.out.println(Utils.yellow("- Modified jars"));
            System.out.println(Utils.yellow("- Potential cheats"));
        }
        
        System.out.println();
        System.out.println(Utils.cyan("Time: " + (elapsed / 1000.0) + " sec"));
        System.out.println(Utils.cyan("Average TPS: " + String.format("%.2f", files.size() / (elapsed / 1000.0))));

        client.printLastKnownLimit();
        
        // Security summary
        System.out.println();
        System.out.println(Utils.cyan("SECURITY SUMMARY"));
        System.out.println("Hash Algorithm: SHA-512");
        System.out.println("Files Checked: " + files.size());
        System.out.println("Verification Rate: " + String.format("%.1f%%", (stats.ok * 100.0) / files.size()));
    }

    private static void runSingleFileMode(String dirPath) throws Exception {
        Path dir = Paths.get(dirPath);

        if (!Files.isDirectory(dir)) {
            System.out.println(Utils.red("Not a directory."));
            return;
        }

        Path canonicalDir = dir.toRealPath();
        System.out.println(Utils.cyan("Checking mods: " + canonicalDir));
        System.out.println();

        List<Path> files = Files.list(canonicalDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                .filter(p -> {
                    try {
                        // Защита от symlink атак - файл должен быть внутри директории
                        return p.toRealPath().startsWith(canonicalDir);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.out.println(Utils.yellow("No .jar files found."));
            return;
        }

        System.out.println(Utils.cyan("Found .jar files: " + files.size()));
        System.out.println();

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
        System.out.println(Utils.green("VERIFIED: " + stats.ok));
        System.out.println(Utils.yellow("UNKNOWN: " + stats.fail));
        
        if (stats.fail > 0) {
            System.out.println();
            System.out.println(Utils.red("WARNING: " + stats.fail + " unknown/modified files detected!"));
        }
        
        System.out.println();
        System.out.println(Utils.cyan("Time: " + (elapsed / 1000.0) + " sec"));
        System.out.println(Utils.cyan("Average TPS: " + String.format("%.2f", files.size() / (elapsed / 1000.0))));

        client.printLastKnownLimit();
        
        System.out.println();
        System.out.println(Utils.cyan("SECURITY SUMMARY"));
        System.out.println("Hash Algorithm: SHA-512");
        System.out.println("Files Checked: " + files.size());
        System.out.println("Verification Rate: " + String.format("%.1f%%", (stats.ok * 100.0) / files.size()));
    }
}
