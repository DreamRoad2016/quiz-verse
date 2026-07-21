package net.quizverse.compare;

import java.util.List;

public class CellResult {

    /** exact | partial | none | near | higher | lower | unknown */
    private String kind;
    /** green | yellow | gray */
    private String color;
    private String arrow;
    /** Raw intersection keys (set compare); kept for compatibility */
    private List<String> matched;
    /** Guess values in order, each marked hit/miss — for set columns in UI */
    private List<CellItem> items;

    public CellResult() {
    }

    public CellResult(String kind, String color, String arrow, List<String> matched) {
        this(kind, color, arrow, matched, null);
    }

    public CellResult(String kind, String color, String arrow, List<String> matched, List<CellItem> items) {
        this.kind = kind;
        this.color = color;
        this.arrow = arrow;
        this.matched = matched;
        this.items = items;
    }

    public static CellResult of(String kind, String color) {
        return new CellResult(kind, color, null, null, null);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getArrow() {
        return arrow;
    }

    public void setArrow(String arrow) {
        this.arrow = arrow;
    }

    public List<String> getMatched() {
        return matched;
    }

    public void setMatched(List<String> matched) {
        this.matched = matched;
    }

    public List<CellItem> getItems() {
        return items;
    }

    public void setItems(List<CellItem> items) {
        this.items = items;
    }
}
