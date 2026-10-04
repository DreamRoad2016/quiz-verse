package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 查人搜索结果：统一 ruler（帝/王）与 figure（卿相/武将/学人/文人）两类的轻量命中项，
 * 供前端搜索框定位到人物活跃年代并下钻详情。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamPersonHit {

    /** 匹配评分：0 精确 / 1 前缀 / 2 包含，仅用于排序，不参与序列化。 */
    @JsonIgnore
    private int sortScore;

    private String id;
    private String name;
    private String personalName;
    /** ruler | figure */
    private String kind;
    private int fromYear;
    private int toYear;
    private String polityId;
    private String polityName;
    /** 中文身份：帝王 / 卿相 / 武将 / 学人 / 文人 */
    private String roleLabel;
    private boolean uncertain;
    private String note;

    public int getSortScore() {
        return sortScore;
    }

    public void setSortScore(int sortScore) {
        this.sortScore = sortScore;
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

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
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

    public String getPolityName() {
        return polityName;
    }

    public void setPolityName(String polityName) {
        this.polityName = polityName;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public boolean isUncertain() {
        return uncertain;
    }

    public void setUncertain(boolean uncertain) {
        this.uncertain = uncertain;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
