package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamPolitiesDocument {

    private int schemaVersion = 1;
    private String title;
    private Integer cutoffYear;
    private String yearConvention;
    private String sourceNote;
    private List<String> tiers = new ArrayList<>();
    private List<String> axes = new ArrayList<>();
    private List<StreamGroup> groups = new ArrayList<>();
    private List<StreamPolity> polities = new ArrayList<>();
    private List<StreamMetaMarker> metaMarkers = new ArrayList<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCutoffYear() {
        return cutoffYear;
    }

    public void setCutoffYear(Integer cutoffYear) {
        this.cutoffYear = cutoffYear;
    }

    public String getYearConvention() {
        return yearConvention;
    }

    public void setYearConvention(String yearConvention) {
        this.yearConvention = yearConvention;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    public List<String> getTiers() {
        return tiers;
    }

    public void setTiers(List<String> tiers) {
        this.tiers = tiers != null ? tiers : new ArrayList<>();
    }

    public List<String> getAxes() {
        return axes;
    }

    public void setAxes(List<String> axes) {
        this.axes = axes != null ? axes : new ArrayList<>();
    }

    public List<StreamGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<StreamGroup> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    public List<StreamPolity> getPolities() {
        return polities;
    }

    public void setPolities(List<StreamPolity> polities) {
        this.polities = polities != null ? polities : new ArrayList<>();
    }

    public List<StreamMetaMarker> getMetaMarkers() {
        return metaMarkers;
    }

    public void setMetaMarkers(List<StreamMetaMarker> metaMarkers) {
        this.metaMarkers = metaMarkers != null ? metaMarkers : new ArrayList<>();
    }
}
