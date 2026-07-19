package net.quizverse.pack.model;

import java.util.List;

/**
 * Loaded pack: meta + schema + entities. Attrs stay server-side.
 */
public class LoadedPack {

    private final PackMeta meta;
    private final PackSchema schema;
    private final List<PackEntity> entities;

    public LoadedPack(PackMeta meta, PackSchema schema, List<PackEntity> entities) {
        this.meta = meta;
        this.schema = schema;
        this.entities = entities;
    }

    public PackMeta getMeta() {
        return meta;
    }

    public PackSchema getSchema() {
        return schema;
    }

    public List<PackEntity> getEntities() {
        return entities;
    }

    public String getId() {
        return meta.getId();
    }

    public PackEntity findEntity(String entityId) {
        if (entityId == null) {
            return null;
        }
        for (PackEntity e : entities) {
            if (entityId.equals(e.getId())) {
                return e;
            }
        }
        return null;
    }
}
