package net.quizverse.pack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.quizverse.config.QuizProperties;
import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackMeta;
import net.quizverse.pack.model.PackSchema;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PackLoader {

    private static final Logger log = LoggerFactory.getLogger(PackLoader.class);

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper;
    private final QuizProperties properties;

    public PackLoader(ObjectMapper jsonMapper, QuizProperties properties) {
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    public List<LoadedPack> loadAll() throws IOException {
        Map<String, LoadedPack> byId = new LinkedHashMap<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] packYamls = resolver.getResources("classpath*:/packs/*/pack.yaml");
        for (Resource packYaml : packYamls) {
            LoadedPack pack = loadFromClasspathSibling(packYaml);
            byId.put(pack.getId(), pack);
            log.info("Loaded pack '{}' ({} entities) from classpath", pack.getId(), pack.getEntities().size());
        }

        String extra = properties.getPacks().getExtraDir();
        if (extra != null && !extra.isBlank()) {
            Path root = Paths.get(extra);
            if (Files.isDirectory(root)) {
                try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root)) {
                    for (Path dir : dirs) {
                        if (Files.isDirectory(dir) && Files.exists(dir.resolve("pack.yaml"))) {
                            LoadedPack pack = loadFromDirectory(dir);
                            byId.put(pack.getId(), pack);
                            log.info("Loaded pack '{}' ({} entities) from {}", pack.getId(),
                                    pack.getEntities().size(), dir);
                        }
                    }
                }
            } else {
                log.warn("quiz.packs.extra-dir is not a directory: {}", extra);
            }
        }

        return new ArrayList<>(byId.values());
    }

    private LoadedPack loadFromClasspathSibling(Resource packYaml) throws IOException {
        String uri = packYaml.getURI().toString();
        String base = uri.substring(0, uri.lastIndexOf('/') + 1);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        PackMeta meta;
        try (InputStream in = packYaml.getInputStream()) {
            meta = yamlMapper.readValue(in, PackMeta.class);
        }

        PackSchema schema;
        try (InputStream in = resolver.getResource(base + "schema.yaml").getInputStream()) {
            schema = parseSchema(yamlMapper.readTree(in));
        }

        List<PackEntity> entities;
        try (InputStream in = resolver.getResource(base + "entities.json").getInputStream()) {
            entities = jsonMapper.readValue(in, new TypeReference<List<PackEntity>>() {
            });
        }

        validate(meta, schema, entities);
        return new LoadedPack(meta, schema, entities);
    }

    private LoadedPack loadFromDirectory(Path dir) throws IOException {
        PackMeta meta = yamlMapper.readValue(Files.readAllBytes(dir.resolve("pack.yaml")), PackMeta.class);
        PackSchema schema = parseSchema(yamlMapper.readTree(Files.readAllBytes(dir.resolve("schema.yaml"))));
        List<PackEntity> entities = jsonMapper.readValue(
                Files.readAllBytes(dir.resolve("entities.json")),
                new TypeReference<List<PackEntity>>() {
                });
        validate(meta, schema, entities);
        return new LoadedPack(meta, schema, entities);
    }

    private PackSchema parseSchema(JsonNode root) {
        PackSchema schema = new PackSchema();
        if (root.has("columns")) {
            schema.setColumns(yamlMapper.convertValue(
                    root.get("columns"),
                    new TypeReference<List<PackSchema.ColumnDef>>() {
                    }));
        }
        Map<String, List<PackSchema.EnumOption>> enums = new LinkedHashMap<>();
        if (root.has("enums") && root.get("enums").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.get("enums").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                enums.put(e.getKey(), normalizeEnumOptions(e.getValue()));
            }
        }
        schema.setEnums(enums);
        return schema;
    }

    private List<PackSchema.EnumOption> normalizeEnumOptions(JsonNode node) {
        List<PackSchema.EnumOption> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String v = item.asText();
                out.add(new PackSchema.EnumOption(v, v));
            } else if (item.isObject()) {
                String key = item.has("key") ? item.get("key").asText() : item.get("label").asText();
                String label = item.has("label") ? item.get("label").asText() : key;
                out.add(new PackSchema.EnumOption(key, label));
            }
        }
        return out;
    }

    private void validate(PackMeta meta, PackSchema schema, List<PackEntity> entities) {
        if (meta.getId() == null || meta.getId().isBlank()) {
            throw new IllegalStateException("pack.yaml missing id");
        }
        if (schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new IllegalStateException("schema.yaml for " + meta.getId() + " has no columns");
        }
        if (entities == null || entities.isEmpty()) {
            throw new IllegalStateException("entities.json for " + meta.getId() + " is empty");
        }
    }
}
