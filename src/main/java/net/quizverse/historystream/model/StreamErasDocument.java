package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamErasDocument {

    private int schemaVersion = 1;
    private String title;
    private Integer cutoffYear;
    private List<StreamEra> eras = new ArrayList<>();

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

    public List<StreamEra> getEras() {
        return eras;
    }

    public void setEras(List<StreamEra> eras) {
        this.eras = eras != null ? eras : new ArrayList<>();
    }
}
