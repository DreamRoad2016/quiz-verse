package net.quizverse.catalog;

import net.quizverse.catalog.model.CatalogCategory;
import net.quizverse.catalog.model.CatalogHot;
import net.quizverse.catalog.model.CatalogWorld;
import net.quizverse.catalog.model.HomeCatalog;
import net.quizverse.pack.PackRegistry;
import net.quizverse.pack.model.PackMeta;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 YAML 目录模板与已加载题包合并，输出带 ready / badge / description 的完整首页目录。
 */
@Service
public class CatalogService {

    private final CatalogRegistry registry;
    private final PackRegistry packs;

    public CatalogService(CatalogRegistry registry, PackRegistry packs) {
        this.registry = registry;
        this.packs = packs;
    }

    public HomeCatalog enriched() {
        HomeCatalog template = registry.get();
        Map<String, PackMeta> playable = new LinkedHashMap<>();
        for (PackMeta meta : packs.listMeta()) {
            if (meta == null || meta.getId() == null || meta.getId().isBlank()) {
                continue;
            }
            if ("demo".equalsIgnoreCase(meta.getKind())) {
                continue;
            }
            playable.put(meta.getId(), meta);
        }

        HomeCatalog out = new HomeCatalog();
        out.setHot(copyHot(template.getHot()));

        List<CatalogCategory> categories = new ArrayList<>();
        for (CatalogCategory src : template.getCategories()) {
            categories.add(copyCategorySkeleton(src));
        }

        for (CatalogCategory cat : categories) {
            for (CatalogWorld world : cat.getWorlds()) {
                applyPackState(world, playable);
            }
        }

        // 服务端有、目录未挂的官方包 → 按 tag 挂到分类，否则进「更多」
        if (!playable.isEmpty()) {
            CatalogCategory more = ensureMore(categories);
            for (PackMeta meta : new ArrayList<>(playable.values())) {
                CatalogCategory cat = findCategoryForTags(categories, meta.getTags());
                if (cat == null) {
                    cat = more;
                }
                CatalogWorld world = new CatalogWorld();
                world.setId(meta.getId());
                world.setPackId(meta.getId());
                world.setTone(cat.getTone() != null ? cat.getTone() : "cool");
                markReady(world, meta);
                cat.getWorlds().add(world);
                playable.remove(meta.getId());
            }
        }

        out.setCategories(categories);
        out.setHotWorlds(resolveHotWorlds(out.getHot(), categories));
        return out;
    }

    private static final int HOT_LIMIT = 4;

    /**
     * 按 hot.worldIds 顺序挑选已 ready 的宇宙，最多 {@link #HOT_LIMIT} 个。
     * 未配置 worldIds 时：不自动塞满「全部开放」，热点为空（避免以后变成全家桶）。
     */
    private static List<CatalogWorld> resolveHotWorlds(CatalogHot hot, List<CatalogCategory> categories) {
        List<CatalogWorld> out = new ArrayList<>();
        if (hot == null || hot.getWorldIds() == null || hot.getWorldIds().isEmpty()) {
            return out;
        }
        Map<String, CatalogWorld> byId = new LinkedHashMap<>();
        for (CatalogCategory cat : categories) {
            for (CatalogWorld w : cat.getWorlds()) {
                if (w.getId() != null) {
                    byId.putIfAbsent(w.getId(), w);
                }
            }
        }
        for (String id : hot.getWorldIds()) {
            if (out.size() >= HOT_LIMIT) {
                break;
            }
            if (id == null || id.isBlank()) {
                continue;
            }
            CatalogWorld w = byId.get(id.trim());
            if (w != null && w.isReady() && w.getPackId() != null && !w.getPackId().isBlank()) {
                out.add(copyWorld(w));
            }
        }
        return out;
    }

    private void applyPackState(CatalogWorld world, Map<String, PackMeta> playable) {
        String packId = world.getPackId();
        if (packId == null || packId.isBlank()) {
            markSoon(world);
            return;
        }
        PackMeta meta = playable.remove(packId);
        if (meta == null) {
            markSoon(world);
            return;
        }
        markReady(world, meta);
    }

    private static void markReady(CatalogWorld world, PackMeta meta) {
        world.setReady(true);
        world.setStatus("ready");
        world.setBadge("可查阅");
        if (meta.getTitle() != null && !meta.getTitle().isBlank()) {
            world.setTitle(meta.getTitle());
        }
        String desc = meta.getDescription() != null ? meta.getDescription().trim() : "";
        world.setDescription(desc);
        if (!desc.isEmpty()) {
            world.setSubtitle(shortDesc(desc));
        } else if (world.getSubtitle() == null || world.getSubtitle().isBlank()) {
            world.setSubtitle("人物资料辑");
        }
    }

    private static void markSoon(CatalogWorld world) {
        world.setReady(false);
        world.setStatus("soon");
        world.setBadge("整理中");
        if (world.getSubtitle() == null || world.getSubtitle().isBlank()) {
            world.setSubtitle("整理中");
        }
    }

    private static String shortDesc(String text) {
        if (text.length() <= 28) {
            return text;
        }
        return text.substring(0, 28) + "…";
    }

    private static CatalogCategory findCategoryForTags(List<CatalogCategory> categories, List<String> tags) {
        if (tags == null) {
            return null;
        }
        for (String tag : tags) {
            for (CatalogCategory cat : categories) {
                if (cat.getId() != null && cat.getId().equals(tag) && !"more".equals(cat.getId())) {
                    return cat;
                }
            }
        }
        return null;
    }

    private static CatalogCategory ensureMore(List<CatalogCategory> categories) {
        for (CatalogCategory cat : categories) {
            if ("more".equals(cat.getId())) {
                return cat;
            }
        }
        CatalogCategory more = new CatalogCategory();
        more.setId("more");
        more.setName("更多");
        more.setHint("其它已开放题包");
        more.setTone("cool");
        more.setWorlds(new ArrayList<>());
        categories.add(more);
        return more;
    }

    private static CatalogHot copyHot(CatalogHot src) {
        CatalogHot hot = new CatalogHot();
        if (src != null) {
            if (src.getName() != null) {
                hot.setName(src.getName());
            }
            if (src.getHint() != null) {
                hot.setHint(src.getHint());
            }
            if (src.getWorldIds() != null) {
                hot.setWorldIds(new ArrayList<>(src.getWorldIds()));
            }
        }
        return hot;
    }

    private static CatalogCategory copyCategorySkeleton(CatalogCategory src) {
        CatalogCategory cat = new CatalogCategory();
        cat.setId(src.getId());
        cat.setName(src.getName());
        cat.setHint(src.getHint());
        cat.setTone(src.getTone());
        List<CatalogWorld> worlds = new ArrayList<>();
        if (src.getWorlds() != null) {
            for (CatalogWorld w : src.getWorlds()) {
                worlds.add(copyWorld(w));
            }
        }
        cat.setWorlds(worlds);
        return cat;
    }

    private static CatalogWorld copyWorld(CatalogWorld src) {
        CatalogWorld w = new CatalogWorld();
        w.setId(src.getId());
        w.setPackId(src.getPackId());
        w.setTitle(src.getTitle());
        w.setSubtitle(src.getSubtitle());
        w.setDescription(src.getDescription());
        w.setTone(src.getTone());
        w.setReady(src.isReady());
        w.setBadge(src.getBadge());
        w.setStatus(src.getStatus());
        return w;
    }
}
