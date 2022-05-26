package uk.ac.ed.inf;

import java.sql.Date;

/**
 * class for representing all the details of an order placed on a given date
 */
public class Orders {
    private String orderNo;
    private Date deliveryDate;
    private String customer;
    private String deliverTo;
    private String item;
    private double[] deliverToCoords;
    private double[] shopCoords;
    private int itemPrice;

    /**
     * constructor of the Orders class assigning values representing the details of an order
     * @param orderNo the order number
     * @param deliveryDate the date the order was placed on
     * @param customer the student ID
     * @param deliverTo the What3Words location to deliver to
     * @param item the food item
     * @param deliverToCoords the coordinates of the delivery location
     * @param shopCoords the coordinates of the shop
     * @param itemPrice the price of the food item
     */
    public Orders(String orderNo, Date deliveryDate, String customer, String deliverTo, String item, double[] deliverToCoords, double[] shopCoords, int itemPrice) {
        setOrderNo(orderNo);
        setDeliveryDate(deliveryDate);
        setCustomer(customer);
        setDeliverTo(deliverTo);
        setDeliverToCoords(deliverToCoords);
        setShopCoords(shopCoords);
        setItemPrice(itemPrice);
        setItem(item);
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public String getCustomer() {
        return customer;
    }

    public String getDeliverTo() {
        return deliverTo;
    }

    public String getItem() {
        return item;
    }

    public double[] getDeliverToCoords() {
        return deliverToCoords;
    }

    public double[] getShopCoords() {
        return shopCoords;
    }

    public int getItemPrice() {
        return itemPrice;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public void setDeliverTo(String deliverTo) {
        this.deliverTo = deliverTo;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public void setDeliverToCoords(double[] deliverToCoords) {
        this.deliverToCoords = deliverToCoords;
    }

    public void setShopCoords(double[] shopCoords) {
        this.shopCoords = shopCoords;
    }

    public void setItemPrice(int itemPrice) {
        this.itemPrice = itemPrice;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * function to display the details of an order
     */
    public void display() {
        System.out.println("orderNo: " + this.getOrderNo() +
                " deliveryDate: " + this.getDeliveryDate() +
                " customer: " + this.getCustomer() +
                " deliverTo: " + this.getDeliverTo() +
                " deliverToCoords: " + this.getDeliverToCoords()[0] + ", " + this.getDeliverToCoords()[1] +
                " item: " + this.getItem() +
                " shopCoords: " + getShopCoords()[0] + ", " + getShopCoords()[1] +
                " itemPrice: " + this.itemPrice);
    }
}
