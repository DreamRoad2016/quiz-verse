package net.qihoo.guessthepattern.admin;

import net.qihoo.guessthepattern.admin.model.AdminColumn;
import net.qihoo.guessthepattern.admin.model.AdminTableSchema;
import net.qihoo.guessthepattern.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class GenericTableJdbcAdminRepository {

    private static final int MAX_PAGE_SIZE = 200;

    private final JdbcTemplate jdbcTemplate;
    private final AdminTableMetadataService metadataService;

    public GenericTableJdbcAdminRepository(JdbcTemplate jdbcTemplate, AdminTableMetadataService metadataService) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataService = metadataService;
    }

    public long count(String table, String q) {
        AdminTableSchema s = metadataService.schemaFor(table);
        SqlWhere w = buildSearchWhere(s, q);
        String sql = "SELECT COUNT(*) FROM " + quoteTable(s.getTableName()) + w.sql;
        Long n = jdbcTemplate.queryForObject(sql, Long.class, w.params.toArray());
        return n == null ? 0 : n;
    }

    public List<Map<String, Object>> page(String table, String q, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        AdminTableSchema s = metadataService.schemaFor(table);
        SqlWhere w = buildSearchWhere(s, q);
        int offset = page * size;
        String selectCols = s.getColumns().stream().map(c -> quoteIdent(c.getName())).collect(Collectors.joining(", "));
        String order = quoteIdent(s.getPkColumnName()) + " ASC NULLS LAST";
        String sql = "SELECT " + selectCols + " FROM " + quoteTable(s.getTableName()) + w.sql + " ORDER BY " + order + " LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(w.params);
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql, (rs, rowNum) -> readRow(rs, s), params.toArray());
    }

    public Map<String, Object> findByPk(String table, Object pkValue) {
        AdminTableSchema s = metadataService.schemaFor(table);
        String pk = s.getPkColumnName();
        String selectCols = s.getColumns().stream().map(c -> quoteIdent(c.getName())).collect(Collectors.joining(", "));
        String sql = "SELECT " + selectCols + " FROM " + quoteTable(s.getTableName()) + " WHERE " + quoteIdent(pk) + " = ?";
        List<Map<String, Object>> list = jdbcTemplate.query(sql, (rs, rowNum) -> readRow(rs, s), pkValue);
        return list.isEmpty() ? null : list.get(0);
    }

    public int deleteByPk(String table, Object pkValue) {
        AdminTableSchema s = metadataService.schemaFor(table);
        String sql = "DELETE FROM " + quoteTable(s.getTableName()) + " WHERE " + quoteIdent(s.getPkColumnName()) + " = ?";
        return jdbcTemplate.update(sql, pkValue);
    }

    public Map<String, Object> insert(String table, Map<String, Object> row) {
        AdminTableSchema s = metadataService.schemaFor(table);
        LinkedHashMap<String, Object> ordered = orderRowBySchema(s, row, true);
        List<String> cols = new ArrayList<>(ordered.keySet());
        if (cols.isEmpty()) {
            throw new BizException("插入数据为空");
        }
        String colSql = cols.stream().map(GenericTableJdbcAdminRepository::quoteIdent).collect(Collectors.joining(", "));
        String qs = cols.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + quoteTable(s.getTableName()) + " (" + colSql + ") VALUES (" + qs + ")";
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            int i = 1;
            for (String cname : cols) {
                AdminColumn col = s.requireColumn(cname);
                bindValue(con, ps, i++, col, ordered.get(cname));
            }
            return ps;
        });
        Object pk = ordered.get(s.getPkColumnName());
        Map<String, Object> out = findByPk(table, pk);
        if (out == null) {
            throw new BizException("插入后读取失败");
        }
        return out;
    }

    public int updateByPk(String table, Object pkValue, Map<String, Object> patch) {
        AdminTableSchema s = metadataService.schemaFor(table);
        String pkName = s.getPkColumnName();
        LinkedHashMap<String, Object> ordered = orderRowBySchema(s, patch, false);
        ordered.remove(pkName);
        if (ordered.isEmpty()) {
            return 0;
        }
        String setSql = ordered.keySet().stream()
                .map(c -> quoteIdent(c) + " = ?")
                .collect(Collectors.joining(", "));
        String sql = "UPDATE " + quoteTable(s.getTableName()) + " SET " + setSql + " WHERE " + quoteIdent(pkName) + " = ?";
        return jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            int i = 1;
            for (String cname : ordered.keySet()) {
                bindValue(con, ps, i++, s.requireColumn(cname), ordered.get(cname));
            }
            bindValue(con, ps, i, s.requireColumn(pkName), pkValue);
            return ps;
        });
    }

    public int upsert(String table, Map<String, Object> row) {
        AdminTableSchema s = metadataService.schemaFor(table);
        LinkedHashMap<String, Object> ordered = orderRowBySchema(s, row, true);
        String pkName = s.getPkColumnName();
        if (!ordered.containsKey(pkName) || ordered.get(pkName) == null) {
            throw new BizException("导入/覆盖行缺少主键列: " + pkName);
        }
        List<String> cols = new ArrayList<>(ordered.keySet());
        String colSql = cols.stream().map(GenericTableJdbcAdminRepository::quoteIdent).collect(Collectors.joining(", "));
        String qs = cols.stream().map(c -> "?").collect(Collectors.joining(", "));
        String updateSet = cols.stream()
                .filter(c -> !c.equals(pkName))
                .map(c -> quoteIdent(c) + " = EXCLUDED." + quoteIdent(c))
                .collect(Collectors.joining(", "));
        if (updateSet.isEmpty()) {
            String sqlIns = "INSERT INTO " + quoteTable(s.getTableName()) + " (" + colSql + ") VALUES (" + qs + ") ON CONFLICT ("
                    + quoteIdent(pkName) + ") DO NOTHING";
            return jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlIns);
                int i = 1;
                for (String cname : cols) {
                    bindValue(con, ps, i++, s.requireColumn(cname), ordered.get(cname));
                }
                return ps;
            });
        }
        String sql = "INSERT INTO " + quoteTable(s.getTableName()) + " (" + colSql + ") VALUES (" + qs + ") ON CONFLICT ("
                + quoteIdent(pkName) + ") DO UPDATE SET " + updateSet;
        return jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            int i = 1;
            for (String cname : cols) {
                bindValue(con, ps, i++, s.requireColumn(cname), ordered.get(cname));
            }
            return ps;
        });
    }

    public List<Map<String, Object>> exportAll(String table) {
        AdminTableSchema s = metadataService.schemaFor(table);
        String selectCols = s.getColumns().stream().map(c -> quoteIdent(c.getName())).collect(Collectors.joining(", "));
        String order = quoteIdent(s.getPkColumnName()) + " ASC NULLS LAST";
        String sql = "SELECT " + selectCols + " FROM " + quoteTable(s.getTableName()) + " ORDER BY " + order;
        return jdbcTemplate.query(sql, (rs, rowNum) -> readRow(rs, s));
    }

    private static LinkedHashMap<String, Object> orderRowBySchema(AdminTableSchema s, Map<String, Object> row, boolean includeNulls) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (AdminColumn c : s.getColumns()) {
            if (!row.containsKey(c.getName())) {
                continue;
            }
            Object v = row.get(c.getName());
            if (v == null && !includeNulls) {
                continue;
            }
            if (v == null && !c.isNullable()) {
                throw new BizException("列 " + c.getName() + " 不可为 null");
            }
            out.put(c.getName(), v);
        }
        return out;
    }

    private static SqlWhere buildSearchWhere(AdminTableSchema s, String q) {
        if (q == null || q.trim().isEmpty()) {
            return SqlWhere.EMPTY;
        }
        String p = "%" + escapeLike(q.trim()) + "%";
        StringBuilder sb = new StringBuilder();
        List<Object> params = new ArrayList<>();
        boolean any = false;
        sb.append(" WHERE (");
        for (AdminColumn c : s.getColumns()) {
            if (!c.isSearchable()) {
                continue;
            }
            if (any) {
                sb.append(" OR ");
            }
            any = true;
            sb.append(quoteIdent(c.getName())).append("::text ILIKE ?");
            params.add(p);
        }
        if (!any) {
            return SqlWhere.EMPTY;
        }
        sb.append(")");
        return new SqlWhere(sb.toString(), params);
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Map<String, Object> readRow(ResultSet rs, AdminTableSchema s) throws SQLException {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (AdminColumn c : s.getColumns()) {
            m.put(c.getName(), readValue(rs, c));
        }
        return m;
    }

    private static Object readValue(ResultSet rs, AdminColumn c) throws SQLException {
        String n = c.getName();
        if (c.isPgArray() || c.getJdbcType() == Types.ARRAY) {
            Array arr = rs.getArray(n);
            if (rs.wasNull()) {
                return null;
            }
            return readAnyArray(arr);
        }
        switch (c.getJdbcType()) {
            case Types.INTEGER:
                int iv = rs.getInt(n);
                return rs.wasNull() ? null : iv;
            case Types.SMALLINT:
                short sv = rs.getShort(n);
                return rs.wasNull() ? null : (int) sv;
            case Types.BIGINT:
                long lv = rs.getLong(n);
                return rs.wasNull() ? null : lv;
            case Types.FLOAT:
            case Types.REAL:
                float fv = rs.getFloat(n);
                return rs.wasNull() ? null : (double) fv;
            case Types.DOUBLE:
                double dv = rs.getDouble(n);
                return rs.wasNull() ? null : dv;
            case Types.NUMERIC:
            case Types.DECIMAL:
                BigDecimal bd = rs.getBigDecimal(n);
                return bd;
            case Types.BOOLEAN:
            case Types.BIT:
                boolean bv = rs.getBoolean(n);
                return rs.wasNull() ? null : bv;
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIMESTAMP:
                Timestamp t = rs.getTimestamp(n);
                return t == null ? null : t.toInstant().toString();
            case Types.DATE:
                java.sql.Date d = rs.getDate(n);
                return d == null ? null : d.toLocalDate().toString();
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return rs.getString(n);
            case Types.OTHER:
                if (c.isUuid()) {
                    Object u = rs.getObject(n);
                    return u == null ? null : u.toString();
                }
                return rs.getString(n);
            default:
                return rs.getString(n);
        }
    }

    private static List<Object> readAnyArray(Array sqlArray) throws SQLException {
        Object arr = sqlArray.getArray();
        if (arr == null) {
            return Collections.emptyList();
        }
        Object[] o = (Object[]) arr;
        return Arrays.asList(Arrays.copyOf(o, o.length));
    }

    private static void bindValue(Connection con, PreparedStatement ps, int idx, AdminColumn col, Object val)
            throws SQLException {
        if (val == null) {
            if (col.isPgArray() || col.getJdbcType() == Types.ARRAY) {
                ps.setNull(idx, Types.ARRAY);
            } else if (col.isUuid()) {
                ps.setNull(idx, Types.OTHER);
            } else {
                ps.setNull(idx, col.getJdbcType());
            }
            return;
        }
        if (col.isPgArray() || col.getJdbcType() == Types.ARRAY) {
            bindPgArray(con, ps, idx, col, val);
            return;
        }
        switch (col.getJdbcType()) {
            case Types.INTEGER:
            case Types.SMALLINT:
                ps.setInt(idx, toInt(val));
                break;
            case Types.BIGINT:
                ps.setLong(idx, toLong(val));
                break;
            case Types.FLOAT:
            case Types.REAL:
                ps.setFloat(idx, ((Number) val).floatValue());
                break;
            case Types.DOUBLE:
                ps.setDouble(idx, ((Number) val).doubleValue());
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                ps.setBigDecimal(idx, new BigDecimal(val.toString()));
                break;
            case Types.BOOLEAN:
            case Types.BIT:
                ps.setBoolean(idx, toBool(val));
                break;
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.DATE:
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                ps.setString(idx, val.toString());
                break;
            case Types.OTHER:
                if (col.isUuid()) {
                    ps.setObject(idx, UUID.fromString(val.toString()));
                } else {
                    ps.setString(idx, val.toString());
                }
                break;
            default:
                ps.setString(idx, val.toString());
        }
    }

    private static void bindPgArray(Connection con, PreparedStatement ps, int idx, AdminColumn col, Object val)
            throws SQLException {
        if (!(val instanceof List)) {
            throw new BizException("列 " + col.getName() + " 需要 JSON 数组");
        }
        List<?> list = (List<?>) val;
        String elem = col.pgArrayElementType();
        if (elem == null) {
            elem = "text";
        }
        if ("int4".equals(elem) || "integer".equals(elem)) {
            Integer[] arr = list.stream().map(x -> x == null ? null : ((Number) x).intValue()).toArray(Integer[]::new);
            ps.setArray(idx, con.createArrayOf("int4", arr));
        } else if ("int8".equals(elem) || "bigint".equals(elem)) {
            Long[] arr = list.stream().map(x -> x == null ? null : ((Number) x).longValue()).toArray(Long[]::new);
            ps.setArray(idx, con.createArrayOf("int8", arr));
        } else if ("float8".equals(elem) || "double precision".equals(elem)) {
            Double[] arr = list.stream().map(x -> x == null ? null : ((Number) x).doubleValue()).toArray(Double[]::new);
            ps.setArray(idx, con.createArrayOf("float8", arr));
        } else {
            String[] arr = list.stream().map(x -> x == null ? null : String.valueOf(x)).toArray(String[]::new);
            ps.setArray(idx, con.createArrayOf(elem, arr));
        }
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return Integer.parseInt(val.toString());
    }

    private static long toLong(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return Long.parseLong(val.toString());
    }

    private static boolean toBool(Object val) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return Boolean.parseBoolean(val.toString());
    }

    private static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "") + "\"";
    }

    private static String quoteTable(String table) {
        return quoteIdent(table);
    }

    private static final class SqlWhere {
        static final SqlWhere EMPTY = new SqlWhere("", Collections.emptyList());
        final String sql;
        final List<Object> params;

        SqlWhere(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
