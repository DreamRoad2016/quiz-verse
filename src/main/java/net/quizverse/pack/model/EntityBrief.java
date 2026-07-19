package net.quizverse.pack.model;

import java.util.List;

public class EntityBrief {

    private String id;
    private String name;
    private List<String> aliases;

    public EntityBrief() {
    }

    public EntityBrief(String id, String name, List<String> aliases) {
        this.id = id;
        this.name = name;
        this.aliases = aliases;
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
}
