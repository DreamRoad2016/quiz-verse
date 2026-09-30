package net.quizverse.web;

import net.quizverse.history.HistoryEventRegistry;
import net.quizverse.history.model.HistoryEvent;
import net.quizverse.web.dto.HistoryEventResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryTodayController {

    private final HistoryEventRegistry registry;

    public HistoryTodayController(HistoryEventRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/events")
    public List<HistoryEventResponse> listByDate(
            @RequestParam int month,
            @RequestParam int day,
            @RequestParam(required = false) Integer limit) {
        return registry.byDate(month, day, limit).stream()
                .map(HistoryEventResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/events/{eventId}")
    public HistoryEventResponse detail(@PathVariable String eventId) {
        HistoryEvent e = registry.require(eventId);
        return HistoryEventResponse.from(e);
    }
}
