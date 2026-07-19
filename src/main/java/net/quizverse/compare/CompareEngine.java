package net.quizverse.compare;

import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema-driven comparison: identity / exact / set / number.
 */
@Component
public class CompareEngine {

    public Map<String, CellResult> compare(LoadedPack pack, PackEntity guess, PackEntity answer) {
        Map<String, CellResult> out = new LinkedHashMap<>();
        for (PackSchema.ColumnDef col : pack.getSchema().getColumns()) {
            if (!col.isInTable()) {
                continue;
            }
            out.put(col.getKey(), compareColumn(pack, col, guess, answer));
        }
        return out;
    }

    public Map<String, String> display(LoadedPack pack, PackEntity entity) {
        Map<String, String> out = new LinkedHashMap<>();
        for (PackSchema.ColumnDef col : pack.getSchema().getColumns()) {
            if (!col.isInTable()) {
                continue;
            }
            out.put(col.getKey(), formatDisplay(pack, col, entity));
        }
        return out;
    }

    private CellResult compareColumn(LoadedPack pack, PackSchema.ColumnDef col,
                                     PackEntity guess, PackEntity answer) {
        String type = col.getType() == null ? "exact" : col.getType().toLowerCase();
        switch (type) {
            case "identity":
                return guess.getId().equals(answer.getId())
                        ? CellResult.of("exact", "green")
                        : CellResult.of("none", "gray");
            case "exact":
                return compareExact(attrValue(guess, col), attrValue(answer, col));
            case "set":
                return compareSet(toStringSet(attrValue(guess, col)), toStringSet(attrValue(answer, col)));
            case "number":
                int threshold = col.getNearThreshold() == null ? 0 : col.getNearThreshold();
                return compareNumber(toNumber(attrValue(guess, col)), toNumber(attrValue(answer, col)), threshold);
            default:
                throw new IllegalStateException("Unknown column type: " + type + " in pack " + pack.getId());
        }
    }

    private String formatDisplay(LoadedPack pack, PackSchema.ColumnDef col, PackEntity entity) {
        String type = col.getType() == null ? "exact" : col.getType().toLowerCase();
        if ("identity".equals(type)) {
            String alias = entity.getAliases() == null || entity.getAliases().isEmpty()
                    ? ""
                    : " (" + entity.getAliases().get(0) + ")";
            return entity.getName() + alias;
        }
        Object raw = attrValue(entity, col);
        if (raw == null) {
            return "-";
        }
        if ("set".equals(type)) {
            List<String> parts = toStringList(raw).stream()
                    .map(v -> resolveEnumLabel(pack, col, v))
                    .collect(Collectors.toList());
            return parts.isEmpty() ? "-" : String.join("、", parts);
        }
        if ("number".equals(type)) {
            return String.valueOf(raw);
        }
        return resolveEnumLabel(pack, col, String.valueOf(raw));
    }

    private String resolveEnumLabel(LoadedPack pack, PackSchema.ColumnDef col, String key) {
        PackEntity linked = pack.findEntity(key);
        if (linked != null) {
            return linked.getName();
        }
        if (col.getEnumRef() == null || col.getEnumRef().isBlank()) {
            return key;
        }
        List<PackSchema.EnumOption> options = pack.getSchema().getEnums().get(col.getEnumRef());
        if (options == null) {
            return key;
        }
        for (PackSchema.EnumOption opt : options) {
            if (key.equals(opt.getKey())) {
                return opt.getLabel();
            }
        }
        return key;
    }

    private static Object attrValue(PackEntity entity, PackSchema.ColumnDef col) {
        if (entity.getAttrs() == null) {
            return null;
        }
        return entity.getAttrs().get(col.getAttr());
    }

    private static CellResult compareExact(Object gv, Object av) {
        if (gv == null || av == null) {
            return CellResult.of("unknown", "gray");
        }
        if (String.valueOf(gv).equals(String.valueOf(av))) {
            return CellResult.of("exact", "green");
        }
        return CellResult.of("none", "gray");
    }

    private static CellResult compareSet(Set<String> gs, Set<String> as) {
        if (gs.isEmpty() || as.isEmpty()) {
            return CellResult.of("unknown", "gray");
        }
        if (gs.equals(as)) {
            return CellResult.of("exact", "green");
        }
        Set<String> inter = new HashSet<>(gs);
        inter.retainAll(as);
        if (!inter.isEmpty()) {
            List<String> matched = new ArrayList<>(inter);
            Collections.sort(matched);
            return new CellResult("partial", "yellow", null, matched);
        }
        return CellResult.of("none", "gray");
    }

    private static CellResult compareNumber(Double gv, Double av, int threshold) {
        if (gv == null || av == null) {
            return CellResult.of("unknown", "gray");
        }
        if (Double.compare(gv, av) == 0) {
            return CellResult.of("exact", "green");
        }
        double diff = av - gv;
        String arrow = diff > 0 ? "↑" : "↓";
        if (Math.abs(diff) <= threshold) {
            return new CellResult("near", "yellow", arrow, null);
        }
        if (diff > 0) {
            return new CellResult("higher", "gray", arrow, null);
        }
        return new CellResult("lower", "gray", arrow, null);
    }

    private static Double toNumber(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Set<String> toStringSet(Object v) {
        return new LinkedHashSet<>(toStringList(v));
    }

    private static List<String> toStringList(Object v) {
        if (v == null) {
            return Collections.emptyList();
        }
        if (v instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object o : (List<?>) v) {
                if (o != null) {
                    String s = String.valueOf(o);
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
            return out;
        }
        if (v.getClass().isArray()) {
            // JSON never produces arrays after Jackson Map; keep simple
            return Collections.emptyList();
        }
        String s = String.valueOf(v);
        if (s.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(s);
    }
}
