package net.quizverse.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogCategory {

    private String id;
    private String name;
    private String hint = "";
    private String tone = "slate";
    private List<CatalogWorld> worlds = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public List<CatalogWorld> getWorlds() {
        return worlds;
    }

    public void setWorlds(List<CatalogWorld> worlds) {
        this.worlds = worlds != null ? worlds : new ArrayList<>();
    }
}
