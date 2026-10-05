package net.quizverse.historystream;

import net.quizverse.historystream.model.StreamPersonHit;
import net.quizverse.historystream.model.StreamYearPack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HistoryStreamSearchTest {

    @Autowired
    HistoryStreamRegistry registry;

    @Test
    void searchExactFigureName() {
        List<StreamPersonHit> hits = registry.searchPeople("霍去病", 20);
        assertTrue(hits.stream().anyMatch(h -> "fig_huo_qubing".equals(h.getId())));
        StreamPersonHit top = hits.stream().filter(h -> "fig_huo_qubing".equals(h.getId())).findFirst().orElseThrow();
        assertEquals("figure", top.getKind());
        assertEquals("武将", top.getRoleLabel());
    }

    @Test
    void searchByAliasWolongHitsZhugeLiang() {
        List<StreamPersonHit> hits = registry.searchPeople("卧龙", 20);
        assertFalse(hits.isEmpty());
        assertEquals("fig_zhuge_liang", hits.get(0).getId());
        assertEquals(0, hits.get(0).getSortScore());
    }

    @Test
    void searchByCourtesyNameKongming() {
        List<StreamPersonHit> hits = registry.searchPeople("孔明", 20);
        assertTrue(hits.stream().anyMatch(h -> "fig_zhuge_liang".equals(h.getId())));
    }

    @Test
    void searchWangYangmingAndAlias() {
        assertTrue(registry.searchPeople("王阳明", 20).stream()
                .anyMatch(h -> "fig_wang_yangming".equals(h.getId())));
        assertTrue(registry.searchPeople("王守仁", 20).stream()
                .anyMatch(h -> "fig_wang_yangming".equals(h.getId())));
    }

    @Test
    void searchTempleNameDisambiguatesByPolity() {
        List<StreamPersonHit> hits = registry.searchPeople("文帝", 50);
        assertTrue(hits.size() >= 2);
        assertTrue(hits.stream().allMatch(h -> h.getPolityName() != null && !h.getPolityName().isBlank()));
        assertTrue(hits.stream().map(StreamPersonHit::getPolityName).distinct().count() >= 2);
    }

    @Test
    void yearPackIncludesExpandedFigure() {
        StreamYearPack pack = registry.yearPack(-119);
        assertTrue(pack.getFigures().stream().anyMatch(f -> "fig_huo_qubing".equals(f.getId())));
        assertTrue(pack.getFigures().stream().anyMatch(f -> "fig_wei_qing".equals(f.getId())));
    }

    @Test
    void blankQueryReturnsEmpty() {
        assertTrue(registry.searchPeople("  ", 10).isEmpty());
        assertTrue(registry.searchPeople(null, 10).isEmpty());
    }

    @Test
    void figureInventoryGrewPastEighty() {
        assertTrue(registry.meta().get("figureCount") instanceof Number);
        assertTrue(((Number) registry.meta().get("figureCount")).intValue() >= 80);
    }
}
