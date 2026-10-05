package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamRuler {

    private String id;
    private String name;
    private String personalName;
    private int fromYear;
    private int toYear;
    private String polityId;
    private int sort;
    private String note;
    /** 年代为约估 / 通行表，非精确到公历年 */
    private boolean uncertain;
    /** approx | traditional | exact；空则按 uncertain 推断 */
    private String yearPrecision;
    /**
     * 是否画在时间轴上。false 表示仅谱系/详情（夏商王名等）。
     * 默认 true。
     */
    private boolean onTimeline = true;
    /** 通行别称（如「武则天」相对庙号名），供 /search 匹配 */
    private List<String> aliases = new ArrayList<>();

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

    public String getYearPrecision() {
        return yearPrecision;
    }

    public void setYearPrecision(String yearPrecision) {
        this.yearPrecision = yearPrecision;
    }

    public boolean isOnTimeline() {
        return onTimeline;
    }

    public void setOnTimeline(boolean onTimeline) {
        this.onTimeline = onTimeline;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }
}
