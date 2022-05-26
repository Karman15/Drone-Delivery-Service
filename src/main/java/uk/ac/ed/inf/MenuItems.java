package uk.ac.ed.inf;

/**
 * class for representing the items on a menu and their price
 */
public class MenuItems {
    private String item;
    private int pence;

    public String getItem() {
        return item;
    }

    public int getPence() {
        return pence;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setPence(int pence) {
        this.pence = pence;
    }
}
