package discipline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Scanner {
    static int violations = 0;
    static final int MAX_LOC = 800;
    static final Pattern R2_EMPTY_CATCH = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");
    static final Pattern R2_SNEAKY = Pattern.compile("@SneakyThrows");
    static final Pattern R3_WILDCARD = Pattern.compile("import\\s+[\\w.]+\\.\\*\\s*;");

    public static void main(String[] args) throws IOException {
        if (args.length < 1) { System.err.println("usage: Scanner <dir> [dir...]"); System.exit(1); }
        for (String dir : args) {
            try (Stream<Path> files = Files.walk(Path.of(dir))) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(Scanner::check);
            }
        }
        if (violations > 0) {
            System.err.println("\n" + violations + " discipline violation(s) found");
            System.exit(1);
        }
        System.out.println("discipline check passed");
    }

    static void check(Path path) {
        try {
            var lines = Files.readAllLines(path);
            String rel = path.toString();

            // R1: LOC
            if (lines.size() > MAX_LOC) {
                System.err.println("R1 " + rel + " — " + lines.size() + " lines (max " + MAX_LOC + ")");
                violations++;
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                int ln = i + 1;

                // R2: empty catch
                if (R2_EMPTY_CATCH.matcher(line).find()) {
                    System.err.println("R2 " + rel + ":" + ln + " — empty catch: " + line);
                    violations++;
                }
                // R2: @SneakyThrows
                if (R2_SNEAKY.matcher(line).find()) {
                    System.err.println("R2 " + rel + ":" + ln + " — @SneakyThrows: " + line);
                    violations++;
                }
                // R2: printStackTrace
                if (line.contains(".printStackTrace()")) {
                    System.err.println("R2 " + rel + ":" + ln + " — printStackTrace: " + line);
                    violations++;
                }

                // R3: wildcard import
                if (R3_WILDCARD.matcher(line).find()) {
                    System.err.println("R3 " + rel + ":" + ln + " — wildcard import: " + line);
                    violations++;
                }
            }
        } catch (IOException e) {
            System.err.println("error reading " + path + ": " + e.getMessage());
        }
    }
}
