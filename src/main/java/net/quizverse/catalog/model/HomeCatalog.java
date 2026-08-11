package net.quizverse.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeCatalog {

    private CatalogHot hot = new CatalogHot();
    private List<CatalogCategory> categories = new ArrayList<>();
    /** 热点精选（已按配置解析，最多 4 个已开放宇宙） */
    private List<CatalogWorld> hotWorlds = new ArrayList<>();

    public CatalogHot getHot() {
        return hot;
    }

    public void setHot(CatalogHot hot) {
        this.hot = hot != null ? hot : new CatalogHot();
    }

    public List<CatalogCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<CatalogCategory> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public List<CatalogWorld> getHotWorlds() {
        return hotWorlds;
    }

    public void setHotWorlds(List<CatalogWorld> hotWorlds) {
        this.hotWorlds = hotWorlds != null ? hotWorlds : new ArrayList<>();
    }
}
