package net.quizverse.compare;

public class CellItem {

    private String label;
    private boolean hit;

    public CellItem() {
    }

    public CellItem(String label, boolean hit) {
        this.label = label;
        this.hit = hit;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }
}
