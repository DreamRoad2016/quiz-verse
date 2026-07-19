package net.quizverse.pack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackSchema {

    private List<ColumnDef> columns = new ArrayList<>();
    /** enum key -> list of option labels, or list of {key,label} objects flattened at load */
    private Map<String, List<EnumOption>> enums = new LinkedHashMap<>();

    public List<ColumnDef> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDef> columns) {
        this.columns = columns;
    }

    public Map<String, List<EnumOption>> getEnums() {
        return enums;
    }

    public void setEnums(Map<String, List<EnumOption>> enums) {
        this.enums = enums;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnDef {
        private String key;
        private String label;
        /**
         * identity | exact | set | number
         * identity: green only when same entity id (display from name/aliases)
         */
        private String type = "exact";
        /** attr path under entity.attrs; default = key */
        private String attr;
        private Integer nearThreshold;
        private boolean inTable = true;
        /** For set/exact display: resolve enum keys via enums[enumRef] */
        private String enumRef;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAttr() {
            return attr != null ? attr : key;
        }

        public void setAttr(String attr) {
            this.attr = attr;
        }

        public Integer getNearThreshold() {
            return nearThreshold;
        }

        public void setNearThreshold(Integer nearThreshold) {
            this.nearThreshold = nearThreshold;
        }

        public boolean isInTable() {
            return inTable;
        }

        public void setInTable(boolean inTable) {
            this.inTable = inTable;
        }

        public String getEnumRef() {
            return enumRef;
        }

        public void setEnumRef(String enumRef) {
            this.enumRef = enumRef;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnumOption {
        private String key;
        private String label;

        public EnumOption() {
        }

        public EnumOption(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
