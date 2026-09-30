package net.quizverse.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.quizverse.config.QuizProperties;
import net.quizverse.history.model.HistoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HistoryEventLoader {

    private static final Logger log = LoggerFactory.getLogger(HistoryEventLoader.class);
    private static final Pattern DAY_FILE = Pattern.compile("^(\\d{2})-(\\d{2})\\.json$");

    private final ObjectMapper jsonMapper;
    private final QuizProperties properties;

    public HistoryEventLoader(ObjectMapper jsonMapper, QuizProperties properties) {
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    public List<DayFile> loadAll() throws IOException {
        List<DayFile> out = new ArrayList<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/content/history/days/*.json");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !DAY_FILE.matcher(filename).matches()) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                List<HistoryEvent> events = jsonMapper.readValue(in, new TypeReference<>() {});
                out.add(new DayFile(filename, events != null ? events : List.of()));
                log.info("Loaded history day '{}' ({} events) from classpath", filename,
                        events != null ? events.size() : 0);
            }
        }

        String extra = properties.getHistory().getExtraDir();
        if (extra != null && !extra.isBlank()) {
            Path root = Paths.get(extra);
            if (Files.isDirectory(root)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.json")) {
                    for (Path file : stream) {
                        String filename = file.getFileName().toString();
                        if (!DAY_FILE.matcher(filename).matches()) {
                            continue;
                        }
                        List<HistoryEvent> events = jsonMapper.readValue(file.toFile(), new TypeReference<>() {});
                        out.add(new DayFile(filename, events != null ? events : List.of()));
                        log.info("Loaded history day '{}' ({} events) from {}", filename,
                                events != null ? events.size() : 0, file);
                    }
                }
            } else {
                log.warn("quiz.history.extra-dir is not a directory: {}", extra);
            }
        }

        return out;
    }

    public static String dayKey(int month, int day) {
        return String.format(Locale.ROOT, "%02d-%02d", month, day);
    }

    public static int[] parseDayKey(String filename) {
        Matcher m = DAY_FILE.matcher(filename);
        if (!m.matches()) {
            return null;
        }
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
    }

    public record DayFile(String filename, List<HistoryEvent> events) {
    }
}
