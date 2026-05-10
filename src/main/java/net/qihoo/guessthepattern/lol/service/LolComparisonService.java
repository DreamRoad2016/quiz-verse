package net.qihoo.guessthepattern.lol.service;

import net.qihoo.guessthepattern.lol.domain.LolPlayerRow;
import net.qihoo.guessthepattern.lol.dto.LolCellResultDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LoL 选手属性比对（对齐 guessassin / 文档：绿=全等，黄=集合部分相交，灰=不等，数值带 ↑↓，接近可黄）。
 */
@Service
public class LolComparisonService {

    private static final int THRESHOLD_AGE = 3;
    private static final int THRESHOLD_WORLDS = 1;
    private static final int THRESHOLD_CHAMPS = 0;

    public Map<String, LolCellResultDTO> compare(LolPlayerRow g, LolPlayerRow a) {
        Map<String, LolCellResultDTO> out = new LinkedHashMap<>();
        out.put("gameName", compareName(g, a));
        out.put("age", compareNumber(g.getAge(), a.getAge(), THRESHOLD_AGE));
        out.put("region", compareSingle(g.getRegion(), a.getRegion()));
        out.put("team", compareSingle(g.getCurrentTeam(), a.getCurrentTeam()));
        out.put("histTeams", compareSet(g.getHistoricalTeams(), a.getHistoricalTeams()));
        out.put("leagues", compareSet(g.getIdentityRegions(), a.getIdentityRegions()));
        out.put("positions", compareSet(g.getPositions(), a.getPositions()));
        out.put("birthplace", compareSingle(g.getBirthplace(), a.getBirthplace()));
        out.put("champions", compareSet(g.getChampions(), a.getChampions()));
        out.put("status", compareSingle(g.getStatus(), a.getStatus()));
        out.put("worlds", compareInt(g.getWorldsCount(), a.getWorldsCount(), THRESHOLD_WORLDS));
        out.put("champs", compareInt(g.getChampionshipsCount(), a.getChampionshipsCount(), THRESHOLD_CHAMPS));
        return out;
    }

    public Map<String, String> buildGuessDisplay(LolPlayerRow g) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("gameName", g.getGameId() + " (" + nullToEmpty(g.getRealName()) + ")");
        m.put("age", g.getAge() == null ? "?" : String.valueOf(g.getAge()));
        m.put("region", nullToEmpty(g.getRegion()));
        m.put("team", nullToEmpty(g.getCurrentTeam()));
        m.put("histTeams", join(g.getHistoricalTeams()));
        m.put("leagues", join(g.getIdentityRegions()));
        m.put("positions", join(g.getPositions()));
        m.put("birthplace", nullToEmpty(g.getBirthplace()));
        m.put("champions", join(g.getChampions()));
        m.put("status", formatStatus(g.getStatus()));
        m.put("worlds", String.valueOf(g.getWorldsCount()));
        m.put("champs", String.valueOf(g.getChampionshipsCount()));
        return m;
    }

    private static String formatStatus(String s) {
        if (s == null) {
            return "?";
        }
        if ("Active".equalsIgnoreCase(s)) {
            return "现役";
        }
        if ("Retired".equalsIgnoreCase(s)) {
            return "退役";
        }
        return s;
    }

    private static LolCellResultDTO compareName(LolPlayerRow g, LolPlayerRow a) {
        if (g.getId().equals(a.getId())) {
            return cell("exact", "green", null, null);
        }
        return cell("none", "gray", null, null);
    }

    private static LolCellResultDTO compareSingle(String gv, String av) {
        if (gv == null || av == null) {
            return cell("unknown", "gray", null, null);
        }
        if (gv.equals(av)) {
            return cell("exact", "green", null, null);
        }
        return cell("none", "gray", null, null);
    }

    private static LolCellResultDTO compareSet(String[] gArr, String[] aArr) {
        Set<String> gs = toSet(gArr);
        Set<String> as = toSet(aArr);
        if (gs.isEmpty() || as.isEmpty()) {
            return cell("unknown", "gray", null, null);
        }
        if (gs.equals(as)) {
            return cell("exact", "green", null, null);
        }
        Set<String> inter = new HashSet<>(gs);
        inter.retainAll(as);
        if (!inter.isEmpty()) {
            List<String> matched = new ArrayList<>(inter);
            Collections.sort(matched);
            return cell("partial", "yellow", null, matched);
        }
        return cell("none", "gray", null, null);
    }

    private static LolCellResultDTO compareNumber(Integer gv, Integer av, int threshold) {
        if (gv == null || av == null) {
            return cell("unknown", "gray", null, null);
        }
        int g = gv;
        int a = av;
        if (g == a) {
            return cell("exact", "green", null, null);
        }
        int diff = a - g;
        String arrow = diff > 0 ? "↑" : "↓";
        if (Math.abs(diff) <= threshold) {
            return cell("near", "yellow", arrow, null);
        }
        if (diff > 0) {
            return cell("higher", "gray", arrow, null);
        }
        return cell("lower", "gray", arrow, null);
    }

    private static LolCellResultDTO compareInt(int g, int a, int threshold) {
        if (g == a) {
            return cell("exact", "green", null, null);
        }
        int diff = a - g;
        String arrow = diff > 0 ? "↑" : "↓";
        if (Math.abs(diff) <= threshold) {
            return cell("near", "yellow", arrow, null);
        }
        if (diff > 0) {
            return cell("higher", "gray", arrow, null);
        }
        return cell("lower", "gray", arrow, null);
    }

    private static Set<String> toSet(String[] arr) {
        if (arr == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(arr).filter(s -> s != null && !s.isEmpty()).collect(Collectors.toSet());
    }

    private static String join(String[] arr) {
        if (arr == null || arr.length == 0) {
            return "-";
        }
        return String.join("、", arr);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static LolCellResultDTO cell(String kind, String color, String arrow, List<String> matched) {
        return new LolCellResultDTO(kind, color, arrow, matched);
    }
}
