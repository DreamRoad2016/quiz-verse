package net.qihoo.guessthepattern.admin;

import lombok.Value;
import net.qihoo.guessthepattern.enums.ResultEnum;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.admin.model.AdminColumn;
import net.qihoo.guessthepattern.admin.model.AdminTableSchema;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GenericTableAdminService {

    @Resource
    private AdminTableMetadataService adminTableMetadataService;
    @Resource
    private GenericTableJdbcAdminRepository genericTableJdbcAdminRepository;

    public List<String> listTables() {
        return adminTableMetadataService.listAllowedTables();
    }

    public AdminSchemaView schema(String table) {
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        List<Map<String, Object>> cols = new ArrayList<>();
        for (AdminColumn c : s.getColumns()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("jdbcType", c.getJdbcType());
            m.put("typeName", c.getTypeName());
            m.put("nullable", c.isNullable());
            m.put("searchable", c.isSearchable());
            m.put("array", c.isPgArray() || c.getJdbcType() == java.sql.Types.ARRAY);
            cols.add(m);
        }
        return new AdminSchemaView(s.getTableName(), s.getPkColumnName(), cols);
    }

    public GenericPageView page(String table, String q, int page, int size) {
        long total = genericTableJdbcAdminRepository.count(table, q);
        List<Map<String, Object>> rows = genericTableJdbcAdminRepository.page(table, q, page, size);
        return new GenericPageView(total, page, size, rows);
    }

    public Map<String, Object> getRow(String table, String pkStr) {
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        Object pk = parsePk(s, pkStr);
        Map<String, Object> row = genericTableJdbcAdminRepository.findByPk(table, pk);
        if (row == null) {
            throw new BizException("记录不存在", ResultEnum.RECORD_NOT_EXIST_EXCEPTION);
        }
        return row;
    }

    public Map<String, Object> createRow(String table, Map<String, Object> body) {
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(body);
        ensurePkForInsert(s, copy);
        return genericTableJdbcAdminRepository.insert(table, copy);
    }

    public Map<String, Object> updateRow(String table, String pkStr, Map<String, Object> body) {
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        Object pk = parsePk(s, pkStr);
        if (genericTableJdbcAdminRepository.findByPk(table, pk) == null) {
            throw new BizException("记录不存在", ResultEnum.RECORD_NOT_EXIST_EXCEPTION);
        }
        genericTableJdbcAdminRepository.updateByPk(table, pk, body);
        return genericTableJdbcAdminRepository.findByPk(table, pk);
    }

    public void deleteRow(String table, String pkStr) {
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        Object pk = parsePk(s, pkStr);
        int n = genericTableJdbcAdminRepository.deleteByPk(table, pk);
        if (n == 0) {
            throw new BizException("记录不存在", ResultEnum.RECORD_NOT_EXIST_EXCEPTION);
        }
    }

    public List<Map<String, Object>> exportAll(String table) {
        return genericTableJdbcAdminRepository.exportAll(table);
    }

    public ImportResultView importRows(String table, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入列表为空");
        }
        AdminTableSchema s = adminTableMetadataService.schemaFor(table);
        int n = 0;
        for (Map<String, Object> raw : rows) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>(raw);
            ensurePkForUpsert(s, row);
            try {
                genericTableJdbcAdminRepository.upsert(table, row);
            } catch (DataAccessException e) {
                Throwable c = e.getMostSpecificCause();
                String msg = c != null ? c.getMessage() : e.getMessage();
                throw new BizException("第 " + (n + 1) + " 行写入失败: " + msg);
            }
            n++;
        }
        return new ImportResultView(n);
    }

    private static void ensurePkForInsert(AdminTableSchema s, LinkedHashMap<String, Object> row) {
        String pkName = s.getPkColumnName();
        AdminColumn pkCol = s.requireColumn(pkName);
        Object cur = row.get(pkName);
        if (cur == null || (cur instanceof String && !StringUtils.hasText((String) cur))) {
            if (pkCol.isUuid()) {
                row.put(pkName, UUID.randomUUID().toString());
            } else {
                throw new BizException("新建需提供主键列 " + pkName + "（或非 UUID 时由客户端生成）");
            }
        }
    }

    private static void ensurePkForUpsert(AdminTableSchema s, LinkedHashMap<String, Object> row) {
        String pkName = s.getPkColumnName();
        AdminColumn pkCol = s.requireColumn(pkName);
        Object cur = row.get(pkName);
        if (cur == null || (cur instanceof String && !StringUtils.hasText((String) cur))) {
            if (pkCol.isUuid()) {
                row.put(pkName, UUID.randomUUID().toString());
            } else {
                throw new BizException("导入行缺少主键 " + pkName);
            }
        }
    }

    private static Object parsePk(AdminTableSchema s, String pkStr) {
        AdminColumn pkCol = s.requireColumn(s.getPkColumnName());
        try {
            if (pkCol.isUuid()) {
                return UUID.fromString(pkStr);
            }
            switch (pkCol.getJdbcType()) {
                case java.sql.Types.INTEGER:
                case java.sql.Types.SMALLINT:
                    return Integer.parseInt(pkStr);
                case java.sql.Types.BIGINT:
                    return Long.parseLong(pkStr);
                default:
                    return pkStr;
            }
        } catch (Exception e) {
            throw new BizException("主键格式无效: " + e.getMessage());
        }
    }

    @Value
    public static class AdminSchemaView {
        String table;
        String pkColumn;
        List<Map<String, Object>> columns;
    }

    @Value
    public static class GenericPageView {
        long total;
        int page;
        int size;
        List<Map<String, Object>> rows;
    }

    @Value
    public static class ImportResultView {
        int upserted;
    }
}
