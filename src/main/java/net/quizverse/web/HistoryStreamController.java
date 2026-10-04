package net.quizverse.web;

import net.quizverse.historystream.HistoryStreamRegistry;
import net.quizverse.historystream.model.StreamEra;
import net.quizverse.historystream.model.StreamEvent;
import net.quizverse.historystream.model.StreamFigure;
import net.quizverse.historystream.model.StreamGroup;
import net.quizverse.historystream.model.StreamLineagePerson;
import net.quizverse.historystream.model.StreamMetaMarker;
import net.quizverse.historystream.model.StreamPersonHit;
import net.quizverse.historystream.model.StreamPolity;
import net.quizverse.historystream.model.StreamReign;
import net.quizverse.historystream.model.StreamRuler;
import net.quizverse.historystream.model.StreamYearPack;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history/stream")
public class HistoryStreamController {

    private final HistoryStreamRegistry registry;

    public HistoryStreamController(HistoryStreamRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return registry.meta();
    }

    @GetMapping("/eras")
    public List<StreamEra> eras() {
        return registry.eras();
    }

    @GetMapping("/groups")
    public List<StreamGroup> groups() {
        return registry.groups();
    }

    @GetMapping("/polities")
    public List<StreamPolity> polities(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(required = false) String tiers) {
        return registry.politiesInRange(from, to, parseTiers(tiers));
    }

    @GetMapping("/markers")
    public List<StreamMetaMarker> markers(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to) {
        return registry.markersInRange(from, to);
    }

    @GetMapping("/events")
    public Map<String, Object> events(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        List<StreamEvent> items = registry.eventsInRange(from, to, limit, offset);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("count", items.size());
        body.put("empty", items.isEmpty());
        return body;
    }

    @GetMapping("/events/{eventId}")
    public StreamEvent eventDetail(@PathVariable String eventId) {
        return registry.requireEvent(eventId);
    }

    @GetMapping("/rulers")
    public List<StreamRuler> rulers(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(required = false) String polityId) {
        return registry.rulersInRange(from, to, polityId);
    }

    @GetMapping("/figures")
    public List<StreamFigure> figures(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(required = false) String polityId) {
        return registry.figuresInRange(from, to, polityId);
    }

    @GetMapping("/reigns")
    public List<StreamReign> reigns(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(required = false) String polityId,
            @RequestParam(required = false) String rulerId) {
        return registry.reignsInRange(from, to, polityId, rulerId);
    }

    /** 谱系（夏商等）：有名无可靠绝对年，不上时间轴。 */
    @GetMapping("/lineages")
    public List<StreamLineagePerson> lineages(@RequestParam String polityId) {
        return registry.lineagesForPolity(polityId);
    }

    /** 选中年信息包：时局 / 政权 / 大事 / 在位 / 年号 / 谱系。 */
    @GetMapping("/year")
    public StreamYearPack year(@RequestParam int y) {
        return registry.yearPack(y);
    }

    /** 查人：姓名 / 字 / 庙号，返回统一命中项（统治者 + 重要人物）。 */
    @GetMapping("/search")
    public List<StreamPersonHit> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        return registry.searchPeople(q, limit);
    }

    private static Set<String> parseTiers(String tiers) {
        if (tiers == null || tiers.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tiers.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
