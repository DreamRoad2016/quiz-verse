package net.quizverse.historystream;

import jakarta.annotation.PostConstruct;
import net.quizverse.historystream.model.StreamEra;
import net.quizverse.historystream.model.StreamEvent;
import net.quizverse.historystream.model.StreamErasDocument;
import net.quizverse.historystream.model.StreamFigure;
import net.quizverse.historystream.model.StreamGroup;
import net.quizverse.historystream.model.StreamLineagePerson;
import net.quizverse.historystream.model.StreamMetaMarker;
import net.quizverse.historystream.model.StreamPolitiesDocument;
import net.quizverse.historystream.model.StreamPolity;
import net.quizverse.historystream.model.StreamReign;
import net.quizverse.historystream.model.StreamRuler;
import net.quizverse.historystream.model.StreamYearPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HistoryStreamRegistry {

    private static final Logger log = LoggerFactory.getLogger(HistoryStreamRegistry.class);

    private final HistoryStreamLoader loader;

    private List<StreamEra> eras = List.of();
    private List<StreamGroup> groups = List.of();
    private List<StreamPolity> polities = List.of();
    private List<StreamMetaMarker> metaMarkers = List.of();
    private final Map<String, StreamEvent> eventsById = new LinkedHashMap<>();
    private List<StreamEvent> eventsSorted = List.of();
    private final Map<String, StreamRuler> rulersById = new LinkedHashMap<>();
    private List<StreamRuler> rulersSorted = List.of();
    private final Map<String, StreamFigure> figuresById = new LinkedHashMap<>();
    private List<StreamFigure> figuresSorted = List.of();
    private final Map<String, StreamReign> reignsById = new LinkedHashMap<>();
    private List<StreamReign> reignsSorted = List.of();
    private final Map<String, StreamLineagePerson> lineagesById = new LinkedHashMap<>();
    private List<StreamLineagePerson> lineagesSorted = List.of();
    private int minYear;
    private int maxYear;
    private int cutoffYear = 1949;
    private static final int HOOK_SPAN = 8;
    private static final int HOOK_LIMIT = 3;

    public HistoryStreamRegistry(HistoryStreamLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() throws IOException {
        StreamErasDocument erasDoc = loader.loadEras();
        StreamPolitiesDocument politiesDoc = loader.loadPolities();

        List<StreamEra> eraList = new ArrayList<>();
        for (StreamEra era : erasDoc.getEras()) {
            if (era == null || era.getId() == null || era.getId().isBlank()) {
                continue;
            }
            if (era.getFromYear() > era.getToYear()) {
                log.warn("Skip era {} with inverted years", era.getId());
                continue;
            }
            eraList.add(era);
        }
        eraList.sort(Comparator.comparingInt(StreamEra::getSort).thenComparing(StreamEra::getId));
        this.eras = List.copyOf(eraList);

        List<StreamGroup> groupList = new ArrayList<>();
        for (StreamGroup g : politiesDoc.getGroups()) {
            if (g == null || g.getId() == null || g.getId().isBlank()) {
                continue;
            }
            groupList.add(g);
        }
        this.groups = List.copyOf(groupList);

        Map<String, StreamPolity> byId = new LinkedHashMap<>();
        for (StreamPolity p : politiesDoc.getPolities()) {
            if (p == null || p.getId() == null || p.getId().isBlank() || p.getName() == null || p.getName().isBlank()) {
                continue;
            }
            if (p.getFromYear() > p.getToYear()) {
                log.warn("Skip polity {} with inverted years", p.getId());
                continue;
            }
            if (byId.containsKey(p.getId())) {
                log.warn("Duplicate stream polity id {}, skipping later copy", p.getId());
                continue;
            }
            if (p.getTier() == null || p.getTier().isBlank()) {
                p.setTier("spine");
            }
            if (p.getAxis() == null || p.getAxis().isBlank()) {
                p.setAxis("unified");
            }
            if (p.getShortName() == null || p.getShortName().isBlank()) {
                p.setShortName(p.getName());
            }
            byId.put(p.getId(), p);
        }
        List<StreamPolity> polityList = new ArrayList<>(byId.values());
        polityList.sort(Comparator.comparingInt(StreamPolity::getSort).thenComparing(StreamPolity::getId));
        this.polities = List.copyOf(polityList);

        List<StreamMetaMarker> markers = new ArrayList<>();
        for (StreamMetaMarker m : politiesDoc.getMetaMarkers()) {
            if (m == null || m.getId() == null || m.getId().isBlank()) {
                continue;
            }
            markers.add(m);
        }
        markers.sort(Comparator.comparingInt(StreamMetaMarker::getYear).thenComparing(StreamMetaMarker::getId));
        this.metaMarkers = List.copyOf(markers);

        if (politiesDoc.getCutoffYear() != null) {
            this.cutoffYear = politiesDoc.getCutoffYear();
        } else if (erasDoc.getCutoffYear() != null) {
            this.cutoffYear = erasDoc.getCutoffYear();
        }

        eventsById.clear();
        for (StreamEvent raw : loader.loadEvents()) {
            StreamEvent e = normalizeEvent(raw);
            if (e == null) {
                continue;
            }
            if (eventsById.containsKey(e.getEventId())) {
                log.warn("Duplicate stream eventId {}, skipping later copy", e.getEventId());
                continue;
            }
            eventsById.put(e.getEventId(), e);
        }
        List<StreamEvent> sorted = new ArrayList<>(eventsById.values());
        sorted.sort(Comparator
                .comparingInt(StreamEvent::getYear)
                .thenComparing(Comparator.comparingInt(StreamEvent::getImportance).reversed())
                .thenComparing(StreamEvent::getEventId));
        this.eventsSorted = List.copyOf(sorted);

        rulersById.clear();
        for (StreamRuler raw : loader.loadRulers()) {
            if (raw == null || raw.getId() == null || raw.getId().isBlank()
                    || raw.getName() == null || raw.getName().isBlank()) {
                continue;
            }
            if (raw.getFromYear() > raw.getToYear()) {
                log.warn("Skip ruler {} with inverted years", raw.getId());
                continue;
            }
            if (rulersById.containsKey(raw.getId())) {
                log.warn("Duplicate stream ruler id {}, skipping later copy", raw.getId());
                continue;
            }
            rulersById.put(raw.getId(), raw);
        }
        List<StreamRuler> rulerList = new ArrayList<>(rulersById.values());
        rulerList.sort(Comparator.comparingInt(StreamRuler::getSort).thenComparing(StreamRuler::getId));
        this.rulersSorted = List.copyOf(rulerList);

        figuresById.clear();
        for (StreamFigure raw : loader.loadFigures()) {
            if (raw == null || raw.getId() == null || raw.getId().isBlank()
                    || raw.getName() == null || raw.getName().isBlank()) {
                continue;
            }
            if (raw.getFromYear() > raw.getToYear()) {
                log.warn("Skip figure {} with inverted years", raw.getId());
                continue;
            }
            if (figuresById.containsKey(raw.getId())) {
                log.warn("Duplicate stream figure id {}, skipping later copy", raw.getId());
                continue;
            }
            if (raw.getRelatedEventIds() == null) {
                raw.setRelatedEventIds(new ArrayList<>());
            }
            figuresById.put(raw.getId(), raw);
        }
        List<StreamFigure> figureList = new ArrayList<>(figuresById.values());
        figureList.sort(Comparator.comparingInt(StreamFigure::getSort).thenComparing(StreamFigure::getId));
        this.figuresSorted = List.copyOf(figureList);

        reignsById.clear();
        for (StreamReign raw : loader.loadReigns()) {
            if (raw == null || raw.getId() == null || raw.getId().isBlank()
                    || raw.getName() == null || raw.getName().isBlank()) {
                continue;
            }
            if (raw.getFromYear() > raw.getToYear()) {
                log.warn("Skip reign {} with inverted years", raw.getId());
                continue;
            }
            if (reignsById.containsKey(raw.getId())) {
                log.warn("Duplicate stream reign id {}, skipping later copy", raw.getId());
                continue;
            }
            reignsById.put(raw.getId(), raw);
        }
        List<StreamReign> reignList = new ArrayList<>(reignsById.values());
        reignList.sort(Comparator.comparingInt(StreamReign::getSort).thenComparing(StreamReign::getId));
        this.reignsSorted = List.copyOf(reignList);

        lineagesById.clear();
        for (StreamLineagePerson raw : loader.loadLineages()) {
            if (raw == null || raw.getId() == null || raw.getId().isBlank()
                    || raw.getName() == null || raw.getName().isBlank()
                    || raw.getPolityId() == null || raw.getPolityId().isBlank()) {
                continue;
            }
            if (lineagesById.containsKey(raw.getId())) {
                log.warn("Duplicate stream lineage id {}, skipping later copy", raw.getId());
                continue;
            }
            lineagesById.put(raw.getId(), raw);
        }
        List<StreamLineagePerson> lineageList = new ArrayList<>(lineagesById.values());
        lineageList.sort(Comparator.comparingInt(StreamLineagePerson::getSort).thenComparing(StreamLineagePerson::getId));
        this.lineagesSorted = List.copyOf(lineageList);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (StreamEra era : eras) {
            min = Math.min(min, era.getFromYear());
            max = Math.max(max, era.getToYear());
        }
        for (StreamPolity p : polities) {
            min = Math.min(min, p.getFromYear());
            max = Math.max(max, p.getToYear());
        }
        if (min == Integer.MAX_VALUE) {
            min = -2070;
            max = cutoffYear;
        }
        this.minYear = min;
        this.maxYear = Math.min(max, cutoffYear);

        log.info("History stream ready: {} eras, {} polities, {} events, {} rulers, {} figures, {} reigns, {} lineages, years {}..{}",
                eras.size(), polities.size(), eventsById.size(), rulersById.size(), figuresById.size(), reignsById.size(),
                lineagesById.size(), minYear, maxYear);
    }

    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minYear", minYear);
        m.put("maxYear", maxYear);
        m.put("cutoffYear", cutoffYear);
        m.put("eraCount", eras.size());
        m.put("polityCount", polities.size());
        m.put("groupCount", groups.size());
        m.put("eventCount", eventsById.size());
        m.put("rulerCount", rulersById.size());
        m.put("figureCount", figuresById.size());
        m.put("reignCount", reignsById.size());
        m.put("lineageCount", lineagesById.size());
        m.put("markerCount", metaMarkers.size());
        m.put("defaultEraId", defaultEraId());
        return m;
    }

    public List<StreamEra> eras() {
        return eras;
    }

    public List<StreamGroup> groups() {
        return groups;
    }

    public List<StreamMetaMarker> markersInRange(Integer from, Integer to) {
        int[] range = resolveRange(from, to);
        return metaMarkers.stream()
                .filter(m -> YearRange.yearInRange(m.getYear(), range[0], range[1]))
                .collect(Collectors.toList());
    }

    public List<StreamPolity> politiesInRange(Integer from, Integer to, Set<String> tiers) {
        int[] range = resolveRange(from, to);
        return polities.stream()
                .filter(p -> YearRange.intersects(p.getFromYear(), p.getToYear(), range[0], range[1]))
                .filter(p -> tiers == null || tiers.isEmpty() || tiers.contains(normalizeTier(p.getTier())))
                .collect(Collectors.toList());
    }

    public List<StreamEvent> eventsInRange(Integer from, Integer to, Integer limit, Integer offset) {
        int[] range = resolveRange(from, to);
        List<StreamEvent> matched = eventsSorted.stream()
                .filter(e -> eventIntersects(e, range[0], range[1]))
                .collect(Collectors.toList());
        int start = offset == null || offset < 0 ? 0 : offset;
        if (start >= matched.size()) {
            return List.of();
        }
        int end = matched.size();
        if (limit != null && limit > 0) {
            end = Math.min(matched.size(), start + limit);
        }
        return matched.subList(start, end);
    }

    public StreamEvent requireEvent(String eventId) {
        StreamEvent e = eventsById.get(eventId);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "事件不存在: " + eventId);
        }
        return e;
    }

    public List<StreamRuler> rulersInRange(Integer from, Integer to, String polityId) {
        int[] range = resolveRange(from, to);
        return rulersSorted.stream()
                .filter(StreamRuler::isOnTimeline)
                .filter(r -> YearRange.intersects(r.getFromYear(), r.getToYear(), range[0], range[1]))
                .filter(r -> polityId == null || polityId.isBlank() || polityId.equals(r.getPolityId()))
                .collect(Collectors.toList());
    }

    public List<StreamFigure> figuresInRange(Integer from, Integer to, String polityId) {
        int[] range = resolveRange(from, to);
        return figuresSorted.stream()
                .filter(f -> YearRange.intersects(f.getFromYear(), f.getToYear(), range[0], range[1]))
                .filter(f -> polityId == null || polityId.isBlank() || polityId.equals(f.getPolityId()))
                .collect(Collectors.toList());
    }

    public List<StreamLineagePerson> lineagesForPolity(String polityId) {
        if (polityId == null || polityId.isBlank()) {
            return List.of();
        }
        return lineagesSorted.stream()
                .filter(p -> polityId.equals(p.getPolityId()))
                .collect(Collectors.toList());
    }

    public List<StreamReign> reignsInRange(Integer from, Integer to, String polityId, String rulerId) {
        int[] range = resolveRange(from, to);
        return reignsSorted.stream()
                .filter(r -> YearRange.intersects(r.getFromYear(), r.getToYear(), range[0], range[1]))
                .filter(r -> polityId == null || polityId.isBlank() || polityId.equals(r.getPolityId()))
                .filter(r -> rulerId == null || rulerId.isBlank() || rulerId.equals(r.getRulerId()))
                .collect(Collectors.toList());
    }

    public StreamPolity findPolity(String polityId) {
        if (polityId == null || polityId.isBlank()) {
            return null;
        }
        return polities.stream().filter(p -> polityId.equals(p.getId())).findFirst().orElse(null);
    }

    public StreamYearPack yearPack(int year) {
        if (year < minYear || year > maxYear) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "年份超出范围 " + minYear + ".." + maxYear);
        }
        StreamYearPack pack = new StreamYearPack();
        pack.setYear(year);
        pack.setYearLabel(formatYearLabel(year));

        List<StreamEra> hitEras = eras.stream()
                .filter(e -> YearRange.yearInRange(year, e.getFromYear(), e.getToYear()))
                .collect(Collectors.toList());
        pack.setEras(hitEras);

        List<StreamYearPack.YearPolity> hitPolities = polities.stream()
                .filter(p -> YearRange.yearInRange(year, p.getFromYear(), p.getToYear()))
                .sorted(Comparator
                        .comparingInt((StreamPolity p) -> tierRank(p.getTier()))
                        .thenComparingInt(p -> axisRank(p.getAxis()))
                        .thenComparingInt(StreamPolity::getSort))
                .map(p -> toYearPolity(p, year))
                .collect(Collectors.toList());
        pack.setPolities(hitPolities);

        List<StreamEvent> hitEvents = eventsSorted.stream()
                .filter(e -> eventIntersects(e, year, year))
                .collect(Collectors.toList());
        pack.setEvents(hitEvents);
        pack.setEmptyEvents(hitEvents.isEmpty());

        List<StreamYearPack.YearRuler> hitRulers = rulersSorted.stream()
                .filter(StreamRuler::isOnTimeline)
                .filter(r -> YearRange.yearInRange(year, r.getFromYear(), r.getToYear()))
                .map(r -> toYearRuler(r, year))
                .collect(Collectors.toList());
        pack.setRulers(hitRulers);

        List<StreamYearPack.YearFigure> hitFigures = figuresSorted.stream()
                .filter(f -> YearRange.yearInRange(year, f.getFromYear(), f.getToYear()))
                .map(HistoryStreamRegistry::toYearFigure)
                .collect(Collectors.toList());
        pack.setFigures(hitFigures);

        List<StreamYearPack.YearReign> hitReigns = reignsSorted.stream()
                .filter(r -> YearRange.yearInRange(year, r.getFromYear(), r.getToYear()))
                .map(r -> toYearReign(r, year))
                .collect(Collectors.toList());
        pack.setReigns(hitReigns);

        pack.setMarkers(metaMarkers.stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList()));

        List<StreamYearPack.YearLineage> lineageBlocks = new ArrayList<>();
        for (StreamYearPack.YearPolity yp : hitPolities) {
            List<StreamLineagePerson> persons = lineagesForPolity(yp.getId());
            if (!persons.isEmpty()) {
                StreamYearPack.YearLineage block = new StreamYearPack.YearLineage();
                block.setPolityId(yp.getId());
                block.setPolityName(yp.getName());
                block.setPersons(persons);
                lineageBlocks.add(block);
            }
        }
        pack.setLineages(lineageBlocks);

        List<StreamEvent> earlier = eventsSorted.stream()
                .filter(e -> e.getYear() < year)
                .collect(Collectors.toList());
        List<StreamEvent> later = eventsSorted.stream()
                .filter(e -> e.getYear() > year)
                .collect(Collectors.toList());
        if (!earlier.isEmpty()) {
            StreamEvent last = earlier.get(earlier.size() - 1);
            pack.setPrevRecordedYear(last.getYear());
            pack.setPrevRecorded(toHook(last, year));
        }
        if (!later.isEmpty()) {
            StreamEvent first = later.get(0);
            pack.setNextRecordedYear(first.getYear());
            pack.setNextRecorded(toHook(first, year));
        }
        pack.setBeforeHooks(hooksFrom(earlier, year, true));
        pack.setAfterHooks(hooksFrom(later, year, false));
        pack.setHeadline(buildHeadline(pack));
        return pack;
    }

    public int minYear() {
        return minYear;
    }

    public int maxYear() {
        return maxYear;
    }

    public int polityCount() {
        return polities.size();
    }

    public int eventCount() {
        return eventsById.size();
    }

    public int rulerCount() {
        return rulersById.size();
    }

    public int reignCount() {
        return reignsById.size();
    }

    private String defaultEraId() {
        for (String preferred : List.of("qin_han", "song_liao_xia_jin", "wei_jin_nanbei")) {
            for (StreamEra era : eras) {
                if (preferred.equals(era.getId())) {
                    return era.getId();
                }
            }
        }
        return eras.isEmpty() ? null : eras.get(0).getId();
    }

    private int[] resolveRange(Integer from, Integer to) {
        int f = from != null ? from : minYear;
        int t = to != null ? to : maxYear;
        if (f > t) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from 不能大于 to");
        }
        return new int[]{f, t};
    }

    private static boolean eventIntersects(StreamEvent e, int from, int to) {
        int end = e.getEndYear() != null ? e.getEndYear() : e.getYear();
        int start = e.getYear();
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        return YearRange.intersects(start, end, from, to);
    }

    private static StreamEvent normalizeEvent(StreamEvent raw) {
        if (raw == null || raw.getEventId() == null || raw.getEventId().isBlank()) {
            return null;
        }
        if (raw.getTitle() == null || raw.getTitle().isBlank()) {
            return null;
        }
        if (raw.getImportance() < 1) {
            raw.setImportance(1);
        }
        if (raw.getImportance() > 5) {
            raw.setImportance(5);
        }
        if (raw.getTags() == null) {
            raw.setTags(new ArrayList<>());
        }
        if (raw.getPolityIds() == null) {
            raw.setPolityIds(new ArrayList<>());
        }
        if (raw.getIdioms() == null) {
            raw.setIdioms(new ArrayList<>());
        }
        raw.setKind(normalizeEventKind(raw.getKind()));
        return raw;
    }

    private static String normalizeEventKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return "politics";
        }
        String k = kind.trim().toLowerCase(Locale.ROOT);
        if ("battle".equals(k) || "politics".equals(k) || "culture".equals(k)) {
            return k;
        }
        if ("person".equals(k) || "event".equals(k)) {
            return "politics";
        }
        return "politics";
    }

    private static String normalizeTier(String tier) {
        return tier == null ? "" : tier.trim().toLowerCase(Locale.ROOT);
    }

    private List<StreamYearPack.YearHook> hooksFrom(List<StreamEvent> ordered, int year, boolean before) {
        List<StreamEvent> windowed = new ArrayList<>();
        if (before) {
            for (int i = ordered.size() - 1; i >= 0 && windowed.size() < HOOK_LIMIT; i--) {
                StreamEvent e = ordered.get(i);
                if (year - e.getYear() > HOOK_SPAN) {
                    break;
                }
                windowed.add(e);
            }
        } else {
            for (int i = 0; i < ordered.size() && windowed.size() < HOOK_LIMIT; i++) {
                StreamEvent e = ordered.get(i);
                if (e.getYear() - year > HOOK_SPAN) {
                    break;
                }
                windowed.add(e);
            }
        }
        List<StreamYearPack.YearHook> out = new ArrayList<>();
        for (StreamEvent e : windowed) {
            out.add(toHook(e, year));
        }
        return out;
    }

    private static StreamYearPack.YearHook toHook(StreamEvent e, int selectedYear) {
        StreamYearPack.YearHook h = new StreamYearPack.YearHook();
        h.setYear(e.getYear());
        h.setTitle(e.getTitle());
        h.setEventId(e.getEventId());
        h.setKind(e.getKind() != null ? e.getKind() : "politics");
        h.setDelta(e.getYear() - selectedYear);
        return h;
    }

    private static StreamYearPack.YearPolity toYearPolity(StreamPolity p, int year) {
        StreamYearPack.YearPolity yp = new StreamYearPack.YearPolity();
        yp.setId(p.getId());
        yp.setName(p.getName());
        yp.setShortName(p.getShortName());
        yp.setTier(p.getTier());
        yp.setAxis(p.getAxis());
        yp.setFromYear(p.getFromYear());
        yp.setToYear(p.getToYear());
        yp.setUncertain(p.isUncertain());
        yp.setCapital(p.getCapital());
        yp.setNote(p.getNote());
        if (year == p.getFromYear() && year == p.getToYear()) {
            yp.setPhase("founding");
        } else if (year == p.getFromYear()) {
            yp.setPhase("founding");
        } else if (year == p.getToYear()) {
            yp.setPhase("ending");
        } else {
            yp.setPhase("ongoing");
        }
        return yp;
    }

    private static StreamYearPack.YearRuler toYearRuler(StreamRuler r, int year) {
        StreamYearPack.YearRuler yr = new StreamYearPack.YearRuler();
        yr.setId(r.getId());
        yr.setName(r.getName());
        yr.setPersonalName(r.getPersonalName());
        yr.setPolityId(r.getPolityId());
        yr.setFromYear(r.getFromYear());
        yr.setToYear(r.getToYear());
        yr.setUncertain(r.isUncertain());
        yr.setNote(r.getNote());
        yr.setYearOfReign(YearRange.ordinalInRange(year, r.getFromYear(), r.getToYear()));
        return yr;
    }

    private static StreamYearPack.YearFigure toYearFigure(StreamFigure f) {
        StreamYearPack.YearFigure yf = new StreamYearPack.YearFigure();
        yf.setId(f.getId());
        yf.setName(f.getName());
        yf.setPersonalName(f.getPersonalName());
        yf.setRole(f.getRole());
        yf.setRoleLabel(figureRoleLabel(f.getRole()));
        yf.setPolityId(f.getPolityId());
        yf.setFromYear(f.getFromYear());
        yf.setToYear(f.getToYear());
        yf.setUncertain(f.isUncertain());
        yf.setNote(f.getNote());
        return yf;
    }

    private static String figureRoleLabel(String role) {
        if ("general".equals(role)) {
            return "武将";
        }
        if ("scholar".equals(role)) {
            return "学人";
        }
        if ("poet".equals(role)) {
            return "文人";
        }
        return "卿相";
    }

    private static StreamYearPack.YearReign toYearReign(StreamReign r, int year) {
        StreamYearPack.YearReign ye = new StreamYearPack.YearReign();
        ye.setId(r.getId());
        ye.setName(r.getName());
        ye.setRulerId(r.getRulerId());
        ye.setPolityId(r.getPolityId());
        ye.setFromYear(r.getFromYear());
        ye.setToYear(r.getToYear());
        ye.setYearOfEra(YearRange.ordinalInRange(year, r.getFromYear(), r.getToYear()));
        return ye;
    }

    private static String buildHeadline(StreamYearPack pack) {
        StringBuilder sb = new StringBuilder(pack.getYearLabel());
        if (!pack.getEras().isEmpty()) {
            sb.append(" · ").append(pack.getEras().get(0).getLabel());
        }
        pack.getReigns().stream().findFirst().ifPresent(r ->
                sb.append(" · ").append(r.getName())
                        .append(r.getYearOfEra() == 1 ? "元年" : r.getYearOfEra() + "年"));
        pack.getPolities().stream()
                .filter(p -> "spine".equals(p.getTier()))
                .findFirst()
                .ifPresent(p -> sb.append(" · ").append(p.getName()));
        if (!pack.getEvents().isEmpty()) {
            sb.append(" · ").append(pack.getEvents().get(0).getTitle());
        } else {
            pack.getRulers().stream().findFirst().ifPresent(r ->
                    sb.append(" · ").append(r.getName()).append("在位第").append(r.getYearOfReign()).append("年"));
            sb.append(" · 本年无大事记载");
        }
        return sb.toString();
    }

    static String formatYearLabel(int year) {
        if (year < 0) {
            return "前" + (-year);
        }
        if (year == 0) {
            return "公元元年";
        }
        return year + "年";
    }

    private static int tierRank(String tier) {
        String t = normalizeTier(tier);
        if ("spine".equals(t)) {
            return 0;
        }
        if ("parallel".equals(t)) {
            return 1;
        }
        return 2;
    }

    private static int axisRank(String axis) {
        if ("unified".equals(axis)) {
            return 0;
        }
        if ("south".equals(axis)) {
            return 1;
        }
        if ("north".equals(axis)) {
            return 2;
        }
        return 3;
    }
}
