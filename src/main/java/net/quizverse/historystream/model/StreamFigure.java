package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** 非帝/王人物：卿相、武将、学人、文人。不上时间轴，进本年包。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamFigure {

    private String id;
    private String name;
    private String personalName;
    /** minister | general | scholar | poet */
    private String role;
    private int fromYear;
    private int toYear;
    private String polityId;
    private int sort;
    private String note;
    private boolean uncertain;
    /** 字号以外的通行别称（如「卧龙」「东坡」），供 /search 匹配 */
    private List<String> aliases = new ArrayList<>();
    private List<String> relatedEventIds = new ArrayList<>();

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

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getFromYear() {
        return fromYear;
    }

    public void setFromYear(int fromYear) {
        this.fromYear = fromYear;
    }

    public int getToYear() {
        return toYear;
    }

    public void setToYear(int toYear) {
        this.toYear = toYear;
    }

    public String getPolityId() {
        return polityId;
    }

    public void setPolityId(String polityId) {
        this.polityId = polityId;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isUncertain() {
        return uncertain;
    }

    public void setUncertain(boolean uncertain) {
        this.uncertain = uncertain;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    public List<String> getRelatedEventIds() {
        return relatedEventIds;
    }

    public void setRelatedEventIds(List<String> relatedEventIds) {
        this.relatedEventIds = relatedEventIds != null ? relatedEventIds : new ArrayList<>();
    }
}
