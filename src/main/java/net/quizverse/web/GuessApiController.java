package net.quizverse.web;

import jakarta.validation.Valid;
import net.quizverse.match.MatchService;
import net.quizverse.pack.PackRegistry;
import net.quizverse.pack.model.EntityBrief;
import net.quizverse.pack.model.PackMeta;
import net.quizverse.web.dto.GuessRequest;
import net.quizverse.web.dto.GuessResponse;
import net.quizverse.web.dto.StartMatchRequest;
import net.quizverse.web.dto.StartMatchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GuessApiController {

    private final PackRegistry packs;
    private final MatchService matches;

    public GuessApiController(PackRegistry packs, MatchService matches) {
        this.packs = packs;
        this.matches = matches;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("ok", true);
        m.put("packs", packs.listMeta().size());
        return m;
    }

    @GetMapping("/packs")
    public List<PackMeta> listPacks() {
        return packs.listMeta();
    }

    @GetMapping("/packs/{packId}/briefs")
    public List<EntityBrief> briefs(@PathVariable String packId) {
        return packs.briefs(packId);
    }

    @PostMapping("/matches")
    public StartMatchResponse start(@RequestBody @Valid StartMatchRequest request) {
        return matches.start(request);
    }

    @PostMapping("/matches/{matchId}/guess")
    public GuessResponse guess(@PathVariable String matchId,
                               @RequestBody @Valid GuessRequest request) {
        return matches.guess(matchId, request);
    }

    @PostMapping("/matches/{matchId}/give-up")
    public GuessResponse giveUp(@PathVariable String matchId) {
        return matches.giveUp(matchId);
    }
}
