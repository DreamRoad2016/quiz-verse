package net.quizverse.history;

import jakarta.annotation.PostConstruct;
import net.quizverse.history.model.HistoryEvent;
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
import java.util.Map;

@Component
public class HistoryEventRegistry {

    private static final Logger log = LoggerFactory.getLogger(HistoryEventRegistry.class);

    private final HistoryEventLoader loader;
    private final Map<String, List<HistoryEvent>> byDay = new LinkedHashMap<>();
    private final Map<String, HistoryEvent> byId = new LinkedHashMap<>();

    public HistoryEventRegistry(HistoryEventLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() throws IOException {
        byDay.clear();
        byId.clear();
        for (HistoryEventLoader.DayFile dayFile : loader.loadAll()) {
            int[] md = HistoryEventLoader.parseDayKey(dayFile.filename());
            if (md == null) {
                continue;
            }
            String key = HistoryEventLoader.dayKey(md[0], md[1]);
            List<HistoryEvent> normalized = new ArrayList<>();
            for (HistoryEvent raw : dayFile.events()) {
                HistoryEvent e = normalize(raw, md[0], md[1]);
                if (e == null) {
                    continue;
                }
                if (byId.containsKey(e.getEventId())) {
                    log.warn("Duplicate history eventId {}, skipping later copy", e.getEventId());
                    continue;
                }
                normalized.add(e);
                byId.put(e.getEventId(), e);
            }
            normalized.sort(Comparator
                    .comparingInt(HistoryEvent::getImportance).reversed()
                    .thenComparingInt(HistoryEvent::getYear));
            byDay.put(key, List.copyOf(normalized));
        }
        log.info("History registry ready: {} days, {} events", byDay.size(), byId.size());
    }

    public List<HistoryEvent> byDate(int month, int day, Integer limit) {
        requireMonthDay(month, day);
        List<HistoryEvent> list = byDay.getOrDefault(HistoryEventLoader.dayKey(month, day), List.of());
        if (limit == null || limit <= 0 || limit >= list.size()) {
            return list;
        }
        return list.subList(0, limit);
    }

    public HistoryEvent require(String eventId) {
        HistoryEvent e = byId.get(eventId);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "事件不存在: " + eventId);
        }
        return e;
    }

    public int dayCount() {
        return byDay.size();
    }

    public int eventCount() {
        return byId.size();
    }

    private static HistoryEvent normalize(HistoryEvent raw, int month, int day) {
        if (raw == null || raw.getEventId() == null || raw.getEventId().isBlank()) {
            return null;
        }
        if (raw.getTitle() == null || raw.getTitle().isBlank()) {
            return null;
        }
        raw.setMonth(month);
        raw.setDay(day);
        if (raw.getTags() == null) {
            raw.setTags(new ArrayList<>());
        }
        if (raw.getImportance() < 1) {
            raw.setImportance(1);
        }
        if (raw.getImportance() > 5) {
            raw.setImportance(5);
        }
        return raw;
    }

    private static void requireMonthDay(int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month/day 无效");
        }
    }
}
