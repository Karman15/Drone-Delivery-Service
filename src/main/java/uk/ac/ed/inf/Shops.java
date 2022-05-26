package uk.ac.ed.inf;

/**
 * class for representing the details of a shop which includes :
 *      name of the shop
 *      location of the shop
 *      menu of the shop
 */
public class Shops {
    private String name, location;

    private MenuItems[] menu;

    public void setLocation(String location) {
        this.location = location;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MenuItems[] getMenu() {
        return menu;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
