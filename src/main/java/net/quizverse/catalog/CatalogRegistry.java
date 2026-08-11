package net.quizverse.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import net.quizverse.catalog.model.CatalogCategory;
import net.quizverse.catalog.model.CatalogHot;
import net.quizverse.catalog.model.CatalogWorld;
import net.quizverse.catalog.model.HomeCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class CatalogRegistry {

    private static final Logger log = LoggerFactory.getLogger(CatalogRegistry.class);

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private HomeCatalog catalog = defaultCatalog();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("catalog/home.yaml");
            if (!resource.exists()) {
                log.warn("classpath:catalog/home.yaml missing, using built-in default catalog");
                catalog = defaultCatalog();
                return;
            }
            try (InputStream in = resource.getInputStream()) {
                HomeCatalog loaded = yamlMapper.readValue(in, HomeCatalog.class);
                catalog = normalize(loaded);
                log.info("Loaded home catalog: {} categories", catalog.getCategories().size());
            }
        } catch (Exception e) {
            log.error("Failed to load catalog/home.yaml, using built-in default", e);
            catalog = defaultCatalog();
        }
    }

    public HomeCatalog get() {
        return catalog;
    }

    private static HomeCatalog normalize(HomeCatalog loaded) {
        if (loaded == null) {
            return defaultCatalog();
        }
        if (loaded.getHot() == null) {
            loaded.setHot(new CatalogHot());
        }
        if (loaded.getCategories() == null) {
            loaded.setCategories(new ArrayList<>());
        }
        for (CatalogCategory cat : loaded.getCategories()) {
            if (cat.getWorlds() == null) {
                cat.setWorlds(new ArrayList<>());
            }
            for (CatalogWorld w : cat.getWorlds()) {
                if (w.getTone() == null || w.getTone().isBlank()) {
                    w.setTone(cat.getTone() != null ? cat.getTone() : "slate");
                }
            }
        }
        if (loaded.getCategories().isEmpty()) {
            return defaultCatalog();
        }
        return loaded;
    }

    /** 与 YAML 同结构的兜底，防止文件损坏时首页空白 */
    static HomeCatalog defaultCatalog() {
        HomeCatalog c = new HomeCatalog();
        CatalogHot hot = new CatalogHot();
        hot.setWorldIds(List.of("zhenhuan", "shuihu"));
        c.setHot(hot);

        List<CatalogCategory> cats = new ArrayList<>();
        cats.add(category("drama", "影视剧", "剧集人物辑录", "ink", List.of(
                world("zhenhuan", "zhenhuan_2011", "甄嬛传", "清宫人物资料辑", "ink"),
                world("daming", null, "大明王朝1566", "整理中", "slate"),
                world("langya", null, "琅琊榜", "整理中", "slate"),
                world("qingyunian", null, "庆余年", "整理中", "slate")
        )));
        cats.add(category("novel", "小说", "文学作品人物辑录", "warm", List.of(
                world("honglou", null, "红楼梦", "四大名著", "warm"),
                world("xiyou", null, "西游记", "四大名著", "warm"),
                world("shuihu", "shuihu_120", "水浒传", "120 回人物资料辑", "warm"),
                world("sanguo", null, "三国演义", "四大名著", "warm"),
                world("santi", null, "三体", "科幻作品", "cool")
        )));
        cats.add(category("history", "历史人物", "按朝代整理", "earth", List.of(
                world("xianqin", null, "先秦", "诸子 · 列国", "earth"),
                world("qinhan", null, "秦汉", "一统与开疆", "earth"),
                world("weijin", null, "魏晋南北朝", "乱世风流", "earth"),
                world("suitang", null, "隋唐", "盛世气象", "earth"),
                world("songyuan", null, "宋元", "文治武功", "earth"),
                world("mingqing", null, "明清", "帝制时期", "earth")
        )));
        c.setCategories(cats);
        return c;
    }

    private static CatalogCategory category(String id, String name, String hint, String tone,
                                            List<CatalogWorld> worlds) {
        CatalogCategory cat = new CatalogCategory();
        cat.setId(id);
        cat.setName(name);
        cat.setHint(hint);
        cat.setTone(tone);
        cat.setWorlds(new ArrayList<>(worlds));
        return cat;
    }

    private static CatalogWorld world(String id, String packId, String title, String subtitle, String tone) {
        CatalogWorld w = new CatalogWorld();
        w.setId(id);
        w.setPackId(packId);
        w.setTitle(title);
        w.setSubtitle(subtitle);
        w.setTone(tone);
        return w;
    }
}
