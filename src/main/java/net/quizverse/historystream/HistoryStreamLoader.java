package net.quizverse.historystream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.quizverse.historystream.model.StreamErasDocument;
import net.quizverse.historystream.model.StreamEvent;
import net.quizverse.historystream.model.StreamFigure;
import net.quizverse.historystream.model.StreamLineagePerson;
import net.quizverse.historystream.model.StreamPolitiesDocument;
import net.quizverse.historystream.model.StreamReign;
import net.quizverse.historystream.model.StreamRuler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class HistoryStreamLoader {

    private static final Logger log = LoggerFactory.getLogger(HistoryStreamLoader.class);

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper;

    public HistoryStreamLoader(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public StreamErasDocument loadEras() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:content/history/stream/eras.yaml");
        if (!resource.exists()) {
            throw new IOException("Missing classpath:content/history/stream/eras.yaml");
        }
        try (InputStream in = resource.getInputStream()) {
            StreamErasDocument doc = yamlMapper.readValue(in, StreamErasDocument.class);
            log.info("Loaded history stream eras: {}",
                    doc.getEras() != null ? doc.getEras().size() : 0);
            return doc;
        }
    }

    public StreamPolitiesDocument loadPolities() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:content/history/stream/polities.yaml");
        if (!resource.exists()) {
            throw new IOException("Missing classpath:content/history/stream/polities.yaml");
        }
        try (InputStream in = resource.getInputStream()) {
            StreamPolitiesDocument doc = yamlMapper.readValue(in, StreamPolitiesDocument.class);
            log.info("Loaded history stream polities: {}, groups: {}",
                    doc.getPolities() != null ? doc.getPolities().size() : 0,
                    doc.getGroups() != null ? doc.getGroups().size() : 0);
            return doc;
        }
    }

    public List<StreamEvent> loadEvents() throws IOException {
        return loadJsonArray("events", new TypeReference<>() {});
    }

    public List<StreamRuler> loadRulers() throws IOException {
        return loadJsonArray("rulers", new TypeReference<>() {});
    }

    public List<StreamFigure> loadFigures() throws IOException {
        return loadJsonArray("figures", new TypeReference<>() {});
    }

    public List<StreamReign> loadReigns() throws IOException {
        return loadJsonArray("reigns", new TypeReference<>() {});
    }

    public List<StreamLineagePerson> loadLineages() throws IOException {
        return loadJsonArray("lineages", new TypeReference<>() {});
    }

    private <T> List<T> loadJsonArray(String folder, TypeReference<List<T>> type) throws IOException {
        List<T> out = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/content/history/stream/" + folder + "/*.json");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !filename.endsWith(".json")) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                List<T> batch = jsonMapper.readValue(in, type);
                if (batch != null) {
                    out.addAll(batch);
                }
                log.info("Loaded history stream {} '{}' ({} items)", folder, filename,
                        batch != null ? batch.size() : 0);
            }
        }
        return out;
    }
}
