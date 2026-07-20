package net.quizverse.pack.model;

import java.util.ArrayList;
import java.util.List;

public class EntityBrief {

    private String id;
    private String name;
    private List<String> aliases;
    /** Search keys: full pinyin, spaced pinyin, initials (e.g. zhenhuan / zhen huan / zh). */
    private List<String> pinyinKeys = new ArrayList<>();

    public EntityBrief() {
    }

    public EntityBrief(String id, String name, List<String> aliases) {
        this.id = id;
        this.name = name;
        this.aliases = aliases;
    }

    public EntityBrief(String id, String name, List<String> aliases, List<String> pinyinKeys) {
        this.id = id;
        this.name = name;
        this.aliases = aliases;
        this.pinyinKeys = pinyinKeys != null ? pinyinKeys : new ArrayList<>();
    }

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

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getPinyinKeys() {
        return pinyinKeys;
    }

    public void setPinyinKeys(List<String> pinyinKeys) {
        this.pinyinKeys = pinyinKeys;
    }
}
