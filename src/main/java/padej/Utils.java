package padej;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Utils {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    public static String green(String s) { return GREEN + s + RESET; }
    public static String red(String s) { return RED + s + RESET; }
    public static String yellow(String s) { return YELLOW + s + RESET; }
    public static String cyan(String s) { return CYAN + s + RESET; }

    public static String sha512(Path file) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String sha1(Path file) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
