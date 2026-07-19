package net.quizverse.pack;

import jakarta.annotation.PostConstruct;
import net.quizverse.pack.model.EntityBrief;
import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackMeta;
import net.quizverse.pack.model.PackSchema;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PackRegistry {

    private final PackLoader loader;
    private final Map<String, LoadedPack> packs = new LinkedHashMap<>();

    public PackRegistry(PackLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() throws IOException {
        for (LoadedPack pack : loader.loadAll()) {
            packs.put(pack.getId(), pack);
        }
        if (packs.isEmpty()) {
            throw new IllegalStateException("No quiz packs found under classpath:/packs");
        }
    }

    public Collection<LoadedPack> all() {
        return packs.values();
    }

    public LoadedPack require(String packId) {
        LoadedPack pack = packs.get(packId);
        if (pack == null) {
            throw new PackNotFoundException(packId);
        }
        return pack;
    }

    public List<PackMeta> listMeta() {
        return packs.values().stream().map(LoadedPack::getMeta).collect(Collectors.toList());
    }

    public List<EntityBrief> briefs(String packId) {
        LoadedPack pack = require(packId);
        List<EntityBrief> list = new ArrayList<>();
        for (PackEntity e : pack.getEntities()) {
            list.add(new EntityBrief(e.getId(), e.getName(), e.getAliases()));
        }
        return list;
    }

    public List<PackSchema.ColumnDef> tableColumns(String packId) {
        return require(packId).getSchema().getColumns().stream()
                .filter(PackSchema.ColumnDef::isInTable)
                .collect(Collectors.toList());
    }
}
