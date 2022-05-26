package uk.ac.ed.inf;

/**
 * class representing the details of a food item, i.e., the price of the food item and the What3Words location of the shop where the food item is sold
 */
public class ItemPriceShopLocation {
    private int price;
    private String location;

    public int getPrice() {
        return price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
