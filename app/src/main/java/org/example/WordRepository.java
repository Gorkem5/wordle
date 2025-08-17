package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads the 5-letter English words from a JSON array file.
 * Default location (classpath): /words/words_en_5.json
 * Override via env WORDLE_WORDS_JSON or file ~/.wordle/words_en_5.json
 */
public final class WordRepository {
    private static final String CLASSPATH_JSON = "/words/words_en_5.json";
    private static final String ENV_PATH = "WORDLE_WORDS_JSON";

    private static List<String> CACHE;
    private static Set<String> CACHE_SET;

    private WordRepository() {}

    public static List<String> words() {
        if (CACHE != null) return CACHE;
        List<String> out = null;

        String env = System.getenv(ENV_PATH);
        if (env != null && !env.isBlank()) {
            out = tryLoadFromFile(Paths.get(env.trim()));
        }

        if (out == null) {
            Path home = Paths.get(System.getProperty("user.home", "."), ".wordle", "words_en_5.json");
            out = tryLoadFromFile(home);
        }

        if (out == null) {
            out = tryLoadFromClasspath(CLASSPATH_JSON);
        }

        if (out == null || out.isEmpty()) {
            out = fallback();
        }
        CACHE = Collections.unmodifiableList(out);
        CACHE_SET = Collections.unmodifiableSet(new HashSet<>(out));
        return CACHE;
    }

    public static Set<String> wordSet() {
        if (CACHE_SET == null) words();
        return CACHE_SET;
    }

    private static List<String> tryLoadFromFile(Path path) {
        try {
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                return parseJson(bytes);
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static List<String> tryLoadFromClasspath(String resource) {
        try (InputStream in = WordRepository.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            return parseJson(in.readAllBytes());
        } catch (IOException ignored) {}
        return null;
    }

    private static List<String> parseJson(byte[] data) throws IOException {
        ObjectMapper om = new ObjectMapper();
        List<String> list = om.readValue(data, new TypeReference<List<String>>(){});
        return normalize(list);
    }

    private static List<String> normalize(List<String> list) {
        if (list == null) return List.of();
        return list.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> s.length() == 5)
                .filter(s -> s.chars().allMatch(Character::isLetter))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> fallback() {
        String[] data = new String[]{
                // gotta have a backup 
                "about","other","which","their","there","first","would","these","click","price",
                "cigar","rebut","sissy","humph","awake","blush","focal","evade","naval","serve",
                "heath","dwarf","karma","stink","grade","quiet","bench","abate","feign","major"
        };
        return normalize(Arrays.asList(data));
    }
}
