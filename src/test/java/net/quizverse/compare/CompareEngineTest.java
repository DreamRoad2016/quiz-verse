package net.quizverse.compare;

import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackMeta;
import net.quizverse.pack.model.PackSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompareEngineTest {

    private CompareEngine engine;
    private LoadedPack pack;

    @BeforeEach
    void setUp() {
        engine = new CompareEngine();
        PackMeta meta = new PackMeta();
        meta.setId("test");
        meta.setTitle("Test");
        PackSchema schema = new PackSchema();
        PackSchema.ColumnDef name = new PackSchema.ColumnDef();
        name.setKey("name");
        name.setLabel("名");
        name.setType("identity");
        PackSchema.ColumnDef region = new PackSchema.ColumnDef();
        region.setKey("region");
        region.setLabel("区");
        region.setType("exact");
        region.setAttr("region");
        PackSchema.ColumnDef teams = new PackSchema.ColumnDef();
        teams.setKey("teams");
        teams.setLabel("队");
        teams.setType("set");
        teams.setAttr("teams");
        PackSchema.ColumnDef age = new PackSchema.ColumnDef();
        age.setKey("age");
        age.setLabel("龄");
        age.setType("number");
        age.setAttr("age");
        age.setNearThreshold(2);
        schema.setColumns(List.of(name, region, teams, age));

        PackEntity a = entity("1", "甲", Map.of("region", "LPL", "teams", List.of("A", "B"), "age", 25));
        PackEntity b = entity("2", "乙", Map.of("region", "LCK", "teams", List.of("B", "C"), "age", 26));
        pack = new LoadedPack(meta, schema, List.of(a, b));
    }

    @Test
    void exactAndPartialAndNear() {
        PackEntity guess = pack.findEntity("2");
        PackEntity answer = pack.findEntity("1");
        Map<String, CellResult> cells = engine.compare(pack, guess, answer);

        assertEquals("gray", cells.get("name").getColor());
        assertEquals("none", cells.get("region").getKind());
        assertEquals("yellow", cells.get("teams").getColor());
        assertEquals("partial", cells.get("teams").getKind());
        assertEquals(List.of("B"), cells.get("teams").getMatched());
        assertEquals(2, cells.get("teams").getItems().size());
        assertEquals("B", cells.get("teams").getItems().get(0).getLabel());
        assertEquals(true, cells.get("teams").getItems().get(0).isHit());
        assertEquals("C", cells.get("teams").getItems().get(1).getLabel());
        assertEquals(false, cells.get("teams").getItems().get(1).isHit());
        assertEquals("yellow", cells.get("age").getColor());
        assertEquals("near", cells.get("age").getKind());
        assertEquals("↓", cells.get("age").getArrow());
    }

    @Test
    void identityHit() {
        PackEntity e = pack.findEntity("1");
        Map<String, CellResult> cells = engine.compare(pack, e, e);
        assertEquals("green", cells.get("name").getColor());
        assertEquals("exact", cells.get("region").getKind());
        assertNull(cells.get("age").getArrow());
    }

    private static PackEntity entity(String id, String name, Map<String, Object> attrs) {
        PackEntity e = new PackEntity();
        e.setId(id);
        e.setName(name);
        e.setAttrs(attrs);
        return e;
    }
}
