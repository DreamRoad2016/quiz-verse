package net.qihoo.guessthepattern.admin;

import net.qihoo.guessthepattern.admin.model.AdminColumn;
import net.qihoo.guessthepattern.admin.model.AdminTableSchema;
import net.qihoo.guessthepattern.config.QuizAdminProperties;
import net.qihoo.guessthepattern.exception.BizException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 从 JDBC 元数据加载表白名单内的表结构（带缓存）。
 */
@Service
public class AdminTableMetadataService {

    private static final Pattern SAFE_IDENT = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final Map<String, AdminTableSchema> cache = new ConcurrentHashMap<>();

    @Resource
    private DataSource dataSource;
    @Resource
    private QuizAdminProperties quizAdminProperties;

    public List<String> listAllowedTables() {
        return new ArrayList<>(quizAdminProperties.getTables());
    }

    public AdminTableSchema schemaFor(String table) {
        assertWhitelisted(table);
        return cache.computeIfAbsent(table, this::loadSchema);
    }

    public void evictCache(String table) {
        if (table != null) {
            cache.remove(table);
        }
    }

    public void evictAll() {
        cache.clear();
    }

    private void assertWhitelisted(String table) {
        if (table == null || !SAFE_IDENT.matcher(table).matches()) {
            throw new BizException("非法表名");
        }
        List<String> allowed = quizAdminProperties.getTables();
        if (allowed == null || allowed.isEmpty() || allowed.stream().noneMatch(table::equals)) {
            throw new BizException("表未在白名单 quiz.admin.tables 中: " + table);
        }
    }

    private AdminTableSchema loadSchema(String table) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            List<String> pkCols = readPrimaryKeyColumns(md, conn, table);
            if (pkCols.size() != 1) {
                throw new BizException(
                        "通用管理仅支持「单列主键」表，当前表 " + table + " 主键列数=" + pkCols.size());
            }
            String pk = pkCols.get(0);
            TreeMap<Integer, AdminColumn> byOrd = new TreeMap<>();
            try (ResultSet rs = md.getColumns(null, "public", table, null)) {
                while (rs.next()) {
                    int ord = rs.getInt("ORDINAL_POSITION");
                    String colName = rs.getString("COLUMN_NAME");
                    int dataType = rs.getInt("DATA_TYPE");
                    String typeName = rs.getString("TYPE_NAME");
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("NULLABLE"));
                    boolean searchable = AdminColumn.isSearchableColumn(typeName, dataType);
                    byOrd.put(ord, new AdminColumn(colName, dataType, typeName, nullable, searchable));
                }
            }
            if (byOrd.isEmpty()) {
                try (ResultSet rs = md.getColumns(null, null, table, null)) {
                    while (rs.next()) {
                        int ord = rs.getInt("ORDINAL_POSITION");
                        String colName = rs.getString("COLUMN_NAME");
                        int dataType = rs.getInt("DATA_TYPE");
                        String typeName = rs.getString("TYPE_NAME");
                        boolean nullable = "YES".equalsIgnoreCase(rs.getString("NULLABLE"));
                        boolean searchable = AdminColumn.isSearchableColumn(typeName, dataType);
                        byOrd.put(ord, new AdminColumn(colName, dataType, typeName, nullable, searchable));
                    }
                }
            }
            if (byOrd.isEmpty()) {
                throw new BizException("无法读取表结构（请确认 schema 为 public 且表存在）: " + table);
            }
            List<AdminColumn> cols = new ArrayList<>(byOrd.values());
            boolean pkFound = cols.stream().anyMatch(c -> c.getName().equals(pk));
            if (!pkFound) {
                throw new BizException("主键列 " + pk + " 不在列清单中");
            }
            return new AdminTableSchema(table, pk, cols);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("加载表结构失败: " + table + " — " + e.getMessage());
        }
    }

    private static List<String> readPrimaryKeyColumns(DatabaseMetaData md, Connection conn, String table)
            throws Exception {
        List<String> pk = readPk(md, table, "public");
        if (!pk.isEmpty()) {
            return pk;
        }
        pk = readPk(md, table, null);
        if (!pk.isEmpty()) {
            return pk;
        }
        return readPrimaryKeyFromInformationSchema(conn, table);
    }

    /**
     * 部分 PostgreSQL 版本/驱动下 {@link DatabaseMetaData#getPrimaryKeys} 会返回空行，
     * 改用 {@code information_schema} 读取主键列（仍仅支持单列主键业务约束）。
     */
    private static List<String> readPrimaryKeyFromInformationSchema(Connection conn, String table) throws Exception {
        Set<String> schemas = new LinkedHashSet<>();
        String current = conn.getSchema();
        if (current != null && !current.trim().isEmpty()) {
            schemas.add(current.trim());
        }
        schemas.add("public");
        List<String> found = new ArrayList<>();
        String sql =
                "SELECT kcu.column_name "
                        + "FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.key_column_usage kcu "
                        + "ON tc.constraint_schema = kcu.constraint_schema "
                        + "AND tc.constraint_name = kcu.constraint_name "
                        + "WHERE tc.table_schema = ? AND tc.table_name = ? "
                        + "AND tc.constraint_type = 'PRIMARY KEY' "
                        + "ORDER BY kcu.ordinal_position";
        for (String schema : schemas) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, schema);
                ps.setString(2, table);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found.add(rs.getString("column_name"));
                    }
                }
            }
            if (!found.isEmpty()) {
                return found;
            }
            found.clear();
        }
        return found;
    }

    private static List<String> readPk(DatabaseMetaData md, String table, String schema) throws Exception {
        TreeMap<Short, String> bySeq = new TreeMap<>(Comparator.naturalOrder());
        try (ResultSet rs = md.getPrimaryKeys(null, schema, table)) {
            while (rs.next()) {
                bySeq.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
            }
        }
        return new ArrayList<>(bySeq.values());
    }
}
