package net.quizverse.historystream.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/** 选中公元年的聚合信息包。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamYearPack {

    private int year;
    private String yearLabel;
    private String headline;
    /** 本年没有精确命中的事件点（仍可能有政权/在位者）。 */
    private boolean emptyEvents;
    private List<StreamEra> eras = new ArrayList<>();
    private List<YearPolity> polities = new ArrayList<>();
    private List<StreamEvent> events = new ArrayList<>();
    private List<YearRuler> rulers = new ArrayList<>();
    private List<YearFigure> figures = new ArrayList<>();
    private List<YearReign> reigns = new ArrayList<>();
    private List<YearLineage> lineages = new ArrayList<>();
    private List<StreamMetaMarker> markers = new ArrayList<>();
    private Integer prevRecordedYear;
    private Integer nextRecordedYear;
    private YearHook prevRecorded;
    private YearHook nextRecorded;
    /** 选中年之前、落在邻近窗内的事件（近→远）。 */
    private List<YearHook> beforeHooks = new ArrayList<>();
    /** 选中年之后、落在邻近窗内的事件（近→远）。 */
    private List<YearHook> afterHooks = new ArrayList<>();

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getYearLabel() {
        return yearLabel;
    }

    public void setYearLabel(String yearLabel) {
        this.yearLabel = yearLabel;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public boolean isEmptyEvents() {
        return emptyEvents;
    }

    public void setEmptyEvents(boolean emptyEvents) {
        this.emptyEvents = emptyEvents;
    }

    public List<StreamEra> getEras() {
        return eras;
    }

    public void setEras(List<StreamEra> eras) {
        this.eras = eras != null ? eras : new ArrayList<>();
    }

    public List<YearPolity> getPolities() {
        return polities;
    }

    public void setPolities(List<YearPolity> polities) {
        this.polities = polities != null ? polities : new ArrayList<>();
    }

    public List<StreamEvent> getEvents() {
        return events;
    }

    public void setEvents(List<StreamEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
    }

    public List<YearRuler> getRulers() {
        return rulers;
    }

    public void setRulers(List<YearRuler> rulers) {
        this.rulers = rulers != null ? rulers : new ArrayList<>();
    }

    public List<YearFigure> getFigures() {
        return figures;
    }

    public void setFigures(List<YearFigure> figures) {
        this.figures = figures != null ? figures : new ArrayList<>();
    }

    public List<YearReign> getReigns() {
        return reigns;
    }

    public void setReigns(List<YearReign> reigns) {
        this.reigns = reigns != null ? reigns : new ArrayList<>();
    }

    public List<YearLineage> getLineages() {
        return lineages;
    }

    public void setLineages(List<YearLineage> lineages) {
        this.lineages = lineages != null ? lineages : new ArrayList<>();
    }

    public List<StreamMetaMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<StreamMetaMarker> markers) {
        this.markers = markers != null ? markers : new ArrayList<>();
    }

    public Integer getPrevRecordedYear() {
        return prevRecordedYear;
    }

    public void setPrevRecordedYear(Integer prevRecordedYear) {
        this.prevRecordedYear = prevRecordedYear;
    }

    public Integer getNextRecordedYear() {
        return nextRecordedYear;
    }

    public void setNextRecordedYear(Integer nextRecordedYear) {
        this.nextRecordedYear = nextRecordedYear;
    }

    public YearHook getPrevRecorded() {
        return prevRecorded;
    }

    public void setPrevRecorded(YearHook prevRecorded) {
        this.prevRecorded = prevRecorded;
    }

    public YearHook getNextRecorded() {
        return nextRecorded;
    }

    public void setNextRecorded(YearHook nextRecorded) {
        this.nextRecorded = nextRecorded;
    }

    public List<YearHook> getBeforeHooks() {
        return beforeHooks;
    }

    public void setBeforeHooks(List<YearHook> beforeHooks) {
        this.beforeHooks = beforeHooks != null ? beforeHooks : new ArrayList<>();
    }

    public List<YearHook> getAfterHooks() {
        return afterHooks;
    }

    public void setAfterHooks(List<YearHook> afterHooks) {
        this.afterHooks = afterHooks != null ? afterHooks : new ArrayList<>();
    }

    public static class YearHook {
        private int year;
        private String title;
        private String eventId;
        private String kind;
        /** year - selectedYear，负为之前。 */
        private int delta;

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public int getDelta() {
            return delta;
        }

        public void setDelta(int delta) {
            this.delta = delta;
        }
    }

    public static class YearPolity {
        private String id;
        private String name;
        private String shortName;
        private String tier;
        private String axis;
        private int fromYear;
        private int toYear;
        private boolean uncertain;
        /** founding | ongoing | ending */
        private String phase;
        private String capital;
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

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }

        public String getAxis() {
            return axis;
        }

        public void setAxis(String axis) {
            this.axis = axis;
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

        public boolean isUncertain() {
            return uncertain;
        }

        public void setUncertain(boolean uncertain) {
            this.uncertain = uncertain;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public String getCapital() {
            return capital;
        }

        public void setCapital(String capital) {
            this.capital = capital;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class YearRuler {
        private String id;
        private String name;
        private String personalName;
        private String polityId;
        private int fromYear;
        private int toYear;
        private boolean uncertain;
        private int yearOfReign;
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

        public boolean isUncertain() {
            return uncertain;
        }

        public void setUncertain(boolean uncertain) {
            this.uncertain = uncertain;
        }

        public int getYearOfReign() {
            return yearOfReign;
        }

        public void setYearOfReign(int yearOfReign) {
            this.yearOfReign = yearOfReign;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class YearReign {
        private String id;
        private String name;
        private String rulerId;
        private String polityId;
        private int fromYear;
        private int toYear;
        private int yearOfEra;

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

        public String getRulerId() {
            return rulerId;
        }

        public void setRulerId(String rulerId) {
            this.rulerId = rulerId;
        }

        public String getPolityId() {
            return polityId;
        }

        public void setPolityId(String polityId) {
            this.polityId = polityId;
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

        public int getYearOfEra() {
            return yearOfEra;
        }

        public void setYearOfEra(int yearOfEra) {
            this.yearOfEra = yearOfEra;
        }
    }

    public static class YearLineage {
        private String polityId;
        private String polityName;
        private List<StreamLineagePerson> persons = new ArrayList<>();

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

        public List<StreamLineagePerson> getPersons() {
            return persons;
        }

        public void setPersons(List<StreamLineagePerson> persons) {
            this.persons = persons != null ? persons : new ArrayList<>();
        }
    }

    public static class YearFigure {
        private String id;
        private String name;
        private String personalName;
        private String role;
        private String roleLabel;
        private String polityId;
        private int fromYear;
        private int toYear;
        private boolean uncertain;
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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getRoleLabel() {
            return roleLabel;
        }

        public void setRoleLabel(String roleLabel) {
            this.roleLabel = roleLabel;
        }

        public String getPolityId() {
            return polityId;
        }

        public void setPolityId(String polityId) {
            this.polityId = polityId;
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
}
