package net.quizverse.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogHot {

    private String name = "热点";
    private String hint = "正在开放的人物辑";
    /**
     * 精选宇宙 id（对应 categories.worlds[].id），按顺序展示。
     * 服务端最多取前 4 个且已 ready 的项。
     */
    private List<String> worldIds = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public List<String> getWorldIds() {
        return worldIds;
    }

    public void setWorldIds(List<String> worldIds) {
        this.worldIds = worldIds != null ? worldIds : new ArrayList<>();
    }
}
