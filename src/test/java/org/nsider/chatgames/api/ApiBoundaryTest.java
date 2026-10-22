package org.nsider.chatgames.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The api package is published on its own, so it may only use Bukkit, the JDK and itself. */
class ApiBoundaryTest {

    private static final Path API = Paths.get("src/main/java/org/nsider/chatgames/api");

    @Test
    void apiOnlyImportsBukkitAndTheJdk() throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(API)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertFalse(files.isEmpty(), "no api sources found under " + API.toAbsolutePath());

        List<String> leaks = new ArrayList<>();
        for (Path file : files) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("import ")) continue;
                String name = trimmed.replace("import static ", "").replace("import ", "");
                if (!name.startsWith("java.") && !name.startsWith("org.bukkit.") && !name.startsWith("org.nsider.chatgames.api.")) {
                    leaks.add(API.relativize(file) + ": " + trimmed);
                }
            }
        }
        assertTrue(leaks.isEmpty(), "api imports outside Bukkit/JDK: " + leaks);
    }
}
