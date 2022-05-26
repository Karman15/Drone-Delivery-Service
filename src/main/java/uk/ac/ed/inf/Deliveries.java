package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * class representing the deliveries that the drone makes
 */
public class Deliveries {

    // order number and What3Words delivery location
    private String orderNo, deliveredTo;
    // total delivery cost of an order
    private int costInPence;

    // the angle when the drone hovers at a position
    private static final int HOVER_ANGLE = -999;

    /**
     * constructor of the Deliveries class to assign the delivery details
     * @param orderNo the order number
     * @param deliveredTo the What3Words location of the delivery location
     * @param costInPence the total delivery cost of an order
     */
    public Deliveries(String orderNo, String deliveredTo, int costInPence) {
        setOrderNo(orderNo);
        setDeliveredTo(deliveredTo);
        setCostInPence(costInPence);
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getDeliveredTo() {
        return deliveredTo;
    }

    public int getCostInPence() {
        return costInPence;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setDeliveredTo(String deliveredTo) {
        this.deliveredTo = deliveredTo;
    }

    public void setCostInPence(int costInPence) {
        this.costInPence = costInPence;
    }

    /**
     * function to return the order number of the last delivery made
     * @param flightPaths a list of Flightpath objects representing the details of a move that the drone makes
     * @return the order number of the last delivery made
     */
    public String lastDeliveryMade(ArrayList<Flightpath> flightPaths) {
        String lastDelivery = "appleton";
        // iterates over the list of Flightpath objects
        for (Flightpath flight_path : flightPaths)
            // checks if the drone is hovering
            if (flight_path.getAngle() == HOVER_ANGLE)
                // checks if the drone is not at appleton
                if (!flight_path.getOrderNo().equals("appleton"))
                    // checks if the next Flightpath object and the current one have different order numbers in order to check if all items of an order have been delivered
                    if (!flightPaths.get(flightPaths.indexOf(flight_path) + 1).getOrderNo().equals(flight_path.getOrderNo()))
                        lastDelivery = flight_path.getOrderNo();
        return lastDelivery;
    }
}
