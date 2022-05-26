package uk.ac.ed.inf;

/**
 * class representing the flightpath of the drone, records the details of a move that the drone makes
 */
public class Flightpath {

    // the order number represented as a string
    private String orderNo;

    // the coordinates of the drone before and after it makes a move
    private Double fromLongitude, fromLatitude, toLongitude, toLatitude;

    // the angle with which the drone moves
    private int angle;

    /**
     * constructor of the class assigning values representing the details of a move the drone makes
     * @param orderNo the order number
     * @param fromLongitude the longitude of the drone before the move
     * @param fromLatitude the latitude of the drone before the move
     * @param angle the angle with which the drone moves
     * @param toLongitude the longitude of the drone after the move
     * @param toLatitude the latitude of the drone after the move
     */
    public Flightpath (String orderNo, Double fromLongitude, Double fromLatitude, int angle, Double toLongitude, Double toLatitude) {
        setOrderNo(orderNo);
        setFromLatitude(fromLatitude);
        setFromLongitude(fromLongitude);
        setAngle(angle);
        setToLongitude(toLongitude);
        setToLatitude(toLatitude);
    }

    public String getOrderNo() {
        return orderNo;
    }
    public Double getFromLatitude() {
        return fromLatitude;
    }
    public Double getFromLongitude() {
        return fromLongitude;
    }
    public Double getToLatitude() {
        return toLatitude;
    }
    public Double getToLongitude() {
        return toLongitude;
    }
    public int getAngle() {
        return angle;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    public void setAngle(int angle) {
        this.angle = angle;
    }
    public void setFromLatitude(Double fromLatitude) {
        this.fromLatitude = fromLatitude;
    }
    public void setFromLongitude(Double fromLongitude) {
        this.fromLongitude = fromLongitude;
    }
    public void setToLatitude(Double toLatitude) {
        this.toLatitude = toLatitude;
    }
    public void setToLongitude(Double toLongitude) {
        this.toLongitude = toLongitude;
    }
}
