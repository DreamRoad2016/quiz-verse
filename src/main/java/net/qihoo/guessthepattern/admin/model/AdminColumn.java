package net.qihoo.guessthepattern.admin.model;

import lombok.Value;

import java.sql.Types;

/**
 * 表列元数据（来自 JDBC {@code DatabaseMetaData}）。
 */
@Value
public class AdminColumn {
    String name;
    int jdbcType;
    String typeName;
    boolean nullable;
    boolean searchable;

    public boolean isPgArray() {
        return typeName != null && typeName.startsWith("_");
    }

    /** PostgreSQL 数组元素类型名，如 {@code text}、{@code int4}。 */
    public String pgArrayElementType() {
        if (!isPgArray()) {
            return null;
        }
        return typeName.substring(1);
    }

    public boolean isUuid() {
        return "uuid".equalsIgnoreCase(typeName);
    }

    public static boolean isSearchableColumn(String typeName, int jdbcType) {
        if (typeName != null && "bytea".equalsIgnoreCase(typeName)) {
            return false;
        }
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.BOOLEAN:
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.OTHER:
                return true;
            case Types.ARRAY:
                return true;
            default:
                return false;
        }
    }
}
