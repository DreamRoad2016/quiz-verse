package net.qihoo.guessthepattern.work.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import net.qihoo.guessthepattern.work.domain.WorkCharacterRow;
import net.qihoo.guessthepattern.work.domain.WorkRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WorkJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static List<String> textArray(ResultSet rs, String col) throws SQLException {
        java.sql.Array arr = rs.getArray(col);
        if (arr == null) {
            return Collections.emptyList();
        }
        Object[] o = (Object[]) arr.getArray();
        List<String> list = new ArrayList<>();
        for (Object item : o) {
            list.add(item == null ? "" : String.valueOf(item));
        }
        return list;
    }

    private static Map<String, Object> parseAttrs(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
    }

    private static String toJsonbString(Object value) {
        return JSON.toJSONString(value == null ? Collections.emptyMap() : value);
    }

    private static final RowMapper<WorkRow> WORK_MAPPER = (rs, n) -> WorkRow.builder()
            .id(rs.getString("id"))
            .titleCn(rs.getString("title_cn"))
            .category(rs.getString("category"))
            .poolType(rs.getString("pool_type"))
            .schemaVersion(rs.getInt("schema_version"))
            .configDir(rs.getString("config_dir"))
            .enabled(rs.getBoolean("enabled"))
            .createdAt(toInstant(rs.getTimestamp("created_at")))
            .updatedAt(toInstant(rs.getTimestamp("updated_at")))
            .build();

    private static final RowMapper<WorkCharacterRow> CHAR_MAPPER = (rs, n) -> WorkCharacterRow.builder()
            .id(rs.getLong("id"))
            .workId(rs.getString("work_id"))
            .displayName(rs.getString("display_name"))
            .callNames(textArray(rs, "call_names"))
            .attrs(parseAttrs(rs.getString("attrs")))
            .status(rs.getString("status"))
            .isActive(rs.getBoolean("is_active"))
            .sortOrder(rs.getInt("sort_order"))
            .createdAt(toInstant(rs.getTimestamp("created_at")))
            .updatedAt(toInstant(rs.getTimestamp("updated_at")))
            .build();

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    public List<WorkRow> listWorks() {
        return jdbcTemplate.query(
                "SELECT * FROM work ORDER BY created_at DESC, id",
                WORK_MAPPER);
    }

    public Optional<WorkRow> findWork(String id) {
        List<WorkRow> list = jdbcTemplate.query(
                "SELECT * FROM work WHERE id = ?",
                WORK_MAPPER,
                id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public boolean workExists(String id) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work WHERE id = ?",
                Integer.class,
                id);
        return n != null && n > 0;
    }

    public void insertWork(String id, String titleCn, String configDir) {
        jdbcTemplate.update(
                "INSERT INTO work (id, title_cn, category, pool_type, schema_version, config_dir, enabled) "
                        + "VALUES (?, ?, 'drama', 'single_work', 3, ?, true)",
                id, titleCn, configDir);
    }

    public void copyWorkColumns(String fromWorkId, String toWorkId) {
        jdbcTemplate.update(
                "INSERT INTO work_column (work_id, column_key, label_cn, value_type, compare_rule, attrs_path, "
                        + "in_guess_table, sort_order, description) "
                        + "SELECT ?, column_key, label_cn, value_type, compare_rule, attrs_path, "
                        + "in_guess_table, sort_order, description "
                        + "FROM work_column WHERE work_id = ? "
                        + "ON CONFLICT (work_id, column_key) DO UPDATE SET "
                        + "label_cn = EXCLUDED.label_cn, value_type = EXCLUDED.value_type, "
                        + "compare_rule = EXCLUDED.compare_rule, attrs_path = EXCLUDED.attrs_path, "
                        + "in_guess_table = EXCLUDED.in_guess_table, sort_order = EXCLUDED.sort_order, "
                        + "description = EXCLUDED.description",
                toWorkId, fromWorkId);
    }

    public int countCharacters(String workId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_character WHERE work_id = ?",
                Integer.class,
                workId);
        return n == null ? 0 : n;
    }

    public int countActiveCharacters(String workId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_character WHERE work_id = ? AND is_active = true",
                Integer.class,
                workId);
        return n == null ? 0 : n;
    }

    public List<WorkCharacterRow> listCharacters(String workId) {
        return jdbcTemplate.query(
                "SELECT * FROM work_character WHERE work_id = ? ORDER BY sort_order, id",
                CHAR_MAPPER,
                workId);
    }

    public Optional<WorkCharacterRow> findCharacter(String workId, long id) {
        List<WorkCharacterRow> list = jdbcTemplate.query(
                "SELECT * FROM work_character WHERE work_id = ? AND id = ?",
                CHAR_MAPPER,
                workId, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long insertCharacter(String workId, String displayName, List<String> callNames,
                                Map<String, Object> attrs, String status, boolean isActive, int sortOrder) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO work_character (work_id, display_name, call_names, attrs, status, is_active, sort_order) "
                            + "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, workId);
            ps.setString(2, displayName);
            ps.setArray(3, con.createArrayOf("text", callNames == null
                    ? new String[0] : callNames.toArray(new String[0])));
            ps.setString(4, toJsonbString(attrs));
            ps.setString(5, status);
            ps.setBoolean(6, isActive);
            ps.setInt(7, sortOrder);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? 0L : key.longValue();
    }

    public void updateCharacter(long id, String workId, String displayName, List<String> callNames,
                                Map<String, Object> attrs, String status, boolean isActive, int sortOrder) {
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE work_character SET display_name = ?, call_names = ?, attrs = ?::jsonb, "
                            + "status = ?, is_active = ?, sort_order = ?, updated_at = now() "
                            + "WHERE id = ? AND work_id = ?");
            ps.setString(1, displayName);
            ps.setArray(2, con.createArrayOf("text", callNames == null
                    ? new String[0] : callNames.toArray(new String[0])));
            ps.setString(3, toJsonbString(attrs));
            ps.setString(4, status);
            ps.setBoolean(5, isActive);
            ps.setInt(6, sortOrder);
            ps.setLong(7, id);
            ps.setString(8, workId);
            return ps;
        });
    }

    public void deleteCharacter(String workId, long id) {
        jdbcTemplate.update("DELETE FROM work_character WHERE work_id = ? AND id = ?", workId, id);
    }
}
