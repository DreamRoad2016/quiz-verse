package net.quizverse.web.dto;

import net.quizverse.history.model.HistoryEvent;

import java.util.ArrayList;
import java.util.List;

public class HistoryEventResponse {

    private String eventId;
    private int year;
    private int month;
    private int day;
    private String title;
    private String description;
    private List<String> tags = new ArrayList<>();
    private String region;
    private int importance;
    private String eventType;

    public static HistoryEventResponse from(HistoryEvent e) {
        HistoryEventResponse r = new HistoryEventResponse();
        r.eventId = e.getEventId();
        r.year = e.getYear();
        r.month = e.getMonth();
        r.day = e.getDay();
        r.title = e.getTitle();
        r.description = e.getDescription();
        r.tags = e.getTags() != null ? new ArrayList<>(e.getTags()) : new ArrayList<>();
        r.region = e.getRegion();
        r.importance = e.getImportance();
        r.eventType = e.getEventType();
        return r;
    }

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

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
