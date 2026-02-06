package padej;

import java.nio.file.*;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("java -jar app.jar <mods_folder>");
            System.out.println("java -jar app.jar --limit");
            return;
        }

        ModrinthClient client = new ModrinthClient();

        if (args[0].equalsIgnoreCase("--limit")) {
            client.printLimit();
            return;
        }

        Path dir = Paths.get(args[0]);

        if (!Files.isDirectory(dir)) {
            System.out.println(Utils.red("Это не папка."));
            return;
        }

        System.out.println(Utils.cyan("Проверка модов: " + dir));

        Stats stats = new Stats();

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    boolean ok = client.checkFile(path);
                    if (ok) stats.ok++;
                    else stats.fail++;
                } catch (Exception e) {
                    stats.fail++;
                }
            });
        }

        System.out.println();
        System.out.println(Utils.cyan("ИТОГ"));
        System.out.println(Utils.green("OK: " + stats.ok));
        System.out.println(Utils.yellow("NOT FOUND: " + stats.fail));

        client.printLastKnownLimit();
    }
}
