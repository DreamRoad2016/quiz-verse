package net.qihoo.guessthepattern.lol.repo;

import net.qihoo.guessthepattern.lol.domain.LolPlayerRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LolPlayerJdbcRepository {

    private static final RowMapper<LolPlayerRow> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private final JdbcTemplate jdbcTemplate;

    public LolPlayerJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String[] textArray(ResultSet rs, String col) throws SQLException {
        Array arr = rs.getArray(col);
        if (arr == null) {
            return new String[0];
        }
        Object[] o = (Object[]) arr.getArray();
        String[] r = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            r[i] = o[i] == null ? "" : String.valueOf(o[i]);
        }
        return r;
    }

    private static LolPlayerRow mapRow(ResultSet rs) throws SQLException {
        return LolPlayerRow.builder()
                .id(rs.getObject("id", UUID.class))
                .gameId(rs.getString("game_id"))
                .realName(rs.getString("real_name"))
                .age(rs.getObject("age") == null ? null : rs.getInt("age"))
                .currentTeam(rs.getString("current_team"))
                .historicalTeams(textArray(rs, "historical_teams"))
                .region(rs.getString("region"))
                .identityRegions(textArray(rs, "identity_regions"))
                .positions(textArray(rs, "positions"))
                .birthplace(rs.getString("birthplace"))
                .champions(textArray(rs, "champions"))
                .status(rs.getString("status"))
                .worldsCount(rs.getInt("worlds_count"))
                .championshipsCount(rs.getInt("championships_count"))
                .build();
    }

    public int countAll() {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_lol_player", Integer.class);
        return n == null ? 0 : n;
    }

    public UUID pickRandomAnswerId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM demo_lol_player ORDER BY random() LIMIT 1",
                UUID.class);
    }

    public Optional<LolPlayerRow> findById(UUID id) {
        List<LolPlayerRow> list = jdbcTemplate.query(
                "SELECT id, game_id, real_name, age, current_team, historical_teams, region, "
                        + "identity_regions, positions, birthplace, champions, status, worlds_count, championships_count "
                        + "FROM demo_lol_player WHERE id = ?",
                ROW_MAPPER,
                id);
        return list.stream().findFirst();
    }

    public boolean existsById(UUID id) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_lol_player WHERE id = ?",
                Integer.class,
                id);
        return n != null && n > 0;
    }

    /**
     * 联想列表：不含答案以外的敏感逻辑字段，供前端下拉。
     */
    public List<LolBriefProjection> listAllBriefs() {
        return jdbcTemplate.query(
                "SELECT id, game_id, real_name FROM demo_lol_player ORDER BY lower(game_id)",
                (rs, i) -> new LolBriefProjection(
                        rs.getObject("id", UUID.class),
                        rs.getString("game_id"),
                        rs.getString("real_name")));
    }

    public static final class LolBriefProjection {
        public final UUID id;
        public final String gameId;
        public final String realName;

        public LolBriefProjection(UUID id, String gameId, String realName) {
            this.id = id;
            this.gameId = gameId;
            this.realName = realName;
        }
    }
}
