package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamEvent {

    private String eventId;
    private int year;
    private Integer endYear;
    private String title;
    private String summary;
    private String description;
    private String kind;
    private List<String> polityIds = new ArrayList<>();
    private int importance = 3;
    private List<String> tags = new ArrayList<>();
    /** 一事一典：与本事件直接相关的成语。 */
    private List<String> idioms = new ArrayList<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getPolityIds() {
        return polityIds;
    }

    public void setPolityIds(List<String> polityIds) {
        this.polityIds = polityIds != null ? polityIds : new ArrayList<>();
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<String> getIdioms() {
        return idioms;
    }

    public void setIdioms(List<String> idioms) {
        this.idioms = idioms != null ? idioms : new ArrayList<>();
    }
}
