package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 谱系条目：有名无可靠绝对年（夏商等），不上时间轴。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamLineagePerson {

    private String id;
    private String name;
    private String personalName;
    private String polityId;
    private int sort;
    private String note;

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
}
