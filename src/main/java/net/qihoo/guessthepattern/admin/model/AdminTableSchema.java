package net.qihoo.guessthepattern.admin.model;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 白名单内单表的结构：列顺序 + 单列主键。
 */
@Value
public class AdminTableSchema {
    String tableName;
    String pkColumnName;
    List<AdminColumn> columns;

    public Optional<AdminColumn> column(String name) {
        for (AdminColumn c : columns) {
            if (c.getName().equals(name)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public AdminColumn requireColumn(String name) {
        return column(name).orElseThrow(() -> new IllegalArgumentException("未知列: " + name));
    }

    public Map<String, AdminColumn> columnByName() {
        Map<String, AdminColumn> m = new LinkedHashMap<>();
        for (AdminColumn c : columns) {
            m.put(c.getName(), c);
        }
        return m;
    }
}
