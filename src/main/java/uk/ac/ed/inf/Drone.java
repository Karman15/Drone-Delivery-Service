package uk.ac.ed.inf;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * class for representing drone positions((Longitude, Latitude) coordinate) and providing methods for drone movement
 */
public class Drone {

    // the longitude and latitude of the drone position
    private final double longitude, latitude;

    // longitudes of the drone confinement area
    private static final double[] CONFINED_LONG = {-3.192473, -3.184319};

    // latitudes of the drone confinement area
    private static final double[] CONFINED_LAT = {55.942617, 55.946233};

    // coordinates of Appleton Tower
    private static final double[] APPLETON_COORDS = {-3.1869, 55.9445};

    // distance tolerance(close-to distance) in degrees
    private static final double DISTANCE_TOLERANCE = 0.00015;

    // declaring constant angles used in methods
    private static final int HOVER_ANGLE = -999;
    private static final int MIN_ANGLE = 0;
    private static final int MAX_ANGLE = 350;

    // boolean to check if anticlockwise rotation of line intersects any of the no-fly zones
    private boolean check_intersection_return_to_appleton_pos_rot, check_intersection_no_fly_zone_pos_rot;
    // boolean to check if clockwise rotation of line intersects any of the no-fly zones
    private boolean check_intersection_return_to_appleton_neg_rot, check_intersection_no_fly_zone_neg_rot;
    // boolean to check if the max_moves limit has been reached
    private boolean check_max_moves;

    // a list representing the details of the moves the drone makes to return to appleton
    private ArrayList<Flightpath> return_to_appleton_flightpath;

    // double array representing the destination coordinates the drone is moving towards
    private double[] coords;

    // represents the total number of moves the drone has made
    private int moves = 0;

    /**
     * constructor of the class to assign the longitude and latitude values
     *
     * @param longitude value of drone position
     * @param latitude  value of drone position
     */
    public Drone(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * checks if the drones position is within a confined area
     *
     * @return true if the current drone position is within the drone
     * confinement area defined in the coursework specification,
     * false otherwise
     */
    public boolean isConfined() {
        return CONFINED_LONG[0] < longitude && longitude < CONFINED_LONG[1] && CONFINED_LAT[0] < latitude && latitude < CONFINED_LAT[1];
    }

    /**
     * calculates the distance between current drone position
     * and position of LongLat object passed as parameter
     *
     * @param position a LongLat object
     * @return the Pythagorean distance between the two points as a value of type double
     */
    public double distanceTo(Drone position) {
        return Math.sqrt(Math.pow(position.longitude - longitude, 2) + Math.pow(position.latitude - latitude, 2));
    }

    /**
     * checks if the drone position is close to the position
     * of LongLat object passed as a parameter
     *
     * @param position a LongLat object
     * @return true if the current drone position and the position of
     * the LongLat object passed as a parameter are close to each other (<0.00015 degrees),
     * false otherwise
     */
    public boolean closeTo(Drone position) {
        return distanceTo(position) < DISTANCE_TOLERANCE;
    }

    /**
     * calculates the next position of the drone given a suitable angle
     *
     * @param angle an integer value representing the angle at which the drone
     *              might make a move
     * @return a LongLat object which represents the next position of the drone
     * if it makes a move in the direction of the angle
     */
    public Drone nextPosition(int angle) {
        Drone position = this;
        if (angle % 10 == 0 && MIN_ANGLE <= angle && angle <= MAX_ANGLE)
            position = new Drone(longitude + DISTANCE_TOLERANCE * Math.cos(Math.toRadians(angle)), latitude + DISTANCE_TOLERANCE * Math.sin(Math.toRadians(angle)));
        // checks if the angle is -999 which represents the drone hovering at the current position
        else if (angle == HOVER_ANGLE)
            position = new Drone(longitude, latitude);
        return position;
    }

    /**
     * function that gets the transformation matrix given an angle to rotate around the given points
     * @param angle rotation angle
     * @param longitude the longitude of the point to rotate around
     * @param latitude the latitude of the point to rotate around
     * @return the transformation matrix
     */
    public AffineTransform getTransformationMatrix(int angle, double longitude, double latitude) {
        AffineTransform transform_matrix =
                AffineTransform.getRotateInstance(
                        Math.toRadians(angle), longitude, latitude
                );
        return transform_matrix;
    }

    public int getSuitableAngle(double angle) {
        if (angle < 0)
            angle = angle + 360;
        int rounded_angle = (int) (Math.round(angle / 10.0)) * 10;
        rounded_angle %= 360;
        return rounded_angle;
    }

    /**
     * function to return all the positions the drone moves to from current position in order to return to Appleton Tower
     * @param coords the current position of the drone
     * @param no_fly_zones the no-fly zones
     * @return a list of coordinates representing the path the drone takes to return to Appleton Tower
     */
    public List<double[]> return_to_Appleton(double[] coords, List<List<List<Double>>> no_fly_zones) {
        // represents a line segment from current drone position to Appleton Tower
        Line2D line_curr_to_appleton;
        Drone appleton = new Drone(APPLETON_COORDS[0], APPLETON_COORDS[1]);
        // represents current position of the drone
        Drone current_pos = new Drone(coords[0], coords[1]);
        Drone temp;
        // represents the path the drone takes to return to Appleton Tower
        List<double[]> path = new ArrayList<>();

        Flightpath flightpath;
        return_to_appleton_flightpath = new ArrayList<>();

        // while loop to check whether current position of the drone is close to Appleton tower
        while (!current_pos.closeTo(appleton)) {
            temp = current_pos;
            line_curr_to_appleton = new Line2D.Double(current_pos.longitude, current_pos.latitude, APPLETON_COORDS[0], APPLETON_COORDS[1]);
            // checks if the line segment intersects with any of the no-fly zones
            boolean check_intersection_return_to_appleton = check_intersection_no_fly_zone(line_curr_to_appleton, no_fly_zones);
            // angle between the current position and Appleton Tower
            double angle = Math.toDegrees(Math.atan2(APPLETON_COORDS[1] - current_pos.latitude, APPLETON_COORDS[0] - current_pos.longitude));
            // making sure the angle is suitable(positive, rounded off to its tens place and takes values from 0 to 350)
            int rounded_angle = getSuitableAngle(angle);
            // true if line segment intersects with any of the no-fly zones
            if (check_intersection_return_to_appleton) {
                Line2D rotated_line;
                double x_initial, y_initial, x_final, y_final;
                int final_rotation;
                // represents the rotation angle
                int[] rot_angle = {0, 0};
                // represents the distance from the rotated destination point to the destination(Appleton tower)
                double[] dist_rot_point_from_dest = {2000, 2000};

                // gets the current position
                x_initial = line_curr_to_appleton.getX1();
                y_initial = line_curr_to_appleton.getY1();
                Point2D.Double point = new Point2D.Double(line_curr_to_appleton.getX2(), line_curr_to_appleton.getY2());

                // for loop to rotate the line segment in an anticlockwise direction till it does not intersect with any of the no-fly zones
                for (int rotation_angle = 0; rotation_angle <= 180; rotation_angle += 20) {
                    AffineTransform transform_matrix = getTransformationMatrix(rotation_angle, x_initial, y_initial);
                    x_final = transform_matrix.transform(point, null).getX();
                    y_final = transform_matrix.transform(point, null).getY();
                    dist_rot_point_from_dest[1] = distance_from_to(x_final, y_final, APPLETON_COORDS[0], APPLETON_COORDS[1]);
                    rotated_line = new Line2D.Double(x_initial, y_initial, x_final, y_final);
                    // checks if rotated line intersects with no-fly zones
                    check_intersection_return_to_appleton_pos_rot = check_intersection_no_fly_zone(rotated_line, no_fly_zones);
                    // goes into the if-block if it does not intersect
                    if (!check_intersection_return_to_appleton_pos_rot) {
                        rot_angle[0] = rotation_angle + rounded_angle;
                        rotated_line = new Line2D.Double(x_initial, y_initial, current_pos.nextPosition(rot_angle[0]).longitude, current_pos.nextPosition(rot_angle[0]).latitude);
                        // checks if the line segment from current position to next position intersects with the no-fly zones due to rounding off errors
                        if (check_intersection_no_fly_zone(rotated_line, no_fly_zones)) {
                            // if it intersects it either increments or decrements the rotation angle by 10
                            rotated_line = new Line2D.Double(x_initial, y_initial, current_pos.nextPosition(rot_angle[0] + 10).longitude, current_pos.nextPosition(rot_angle[0] + 10).latitude);
                            if (check_intersection_no_fly_zone(rotated_line, no_fly_zones))
                                rot_angle[0] -= 10;
                            else
                                rot_angle[0] += 10;
                        }
                        // checks if the next drone position is confined within the confinement zone
                        if (current_pos.nextPosition(rot_angle[0]).isConfined())
                            break;
                    }
                }
                // for loop to rotate the line segment in an clockwise direction till it does not intersect with any of the no-fly zones
                for (int rotation_angle = -10; rotation_angle > -180; rotation_angle -= 20) {
                    AffineTransform transform_matrix = getTransformationMatrix(rotation_angle, x_initial, y_initial);
                    x_final = transform_matrix.transform(point, null).getX();
                    y_final = transform_matrix.transform(point, null).getY();
                    dist_rot_point_from_dest[1] = distance_from_to(x_final, y_final, APPLETON_COORDS[0], APPLETON_COORDS[1]);
                    rotated_line = new Line2D.Double(x_initial, y_initial, x_final, y_final);
                    // checks if rotated line intersects with no-fly zones
                    check_intersection_return_to_appleton_neg_rot = check_intersection_no_fly_zone(rotated_line, no_fly_zones);
                    // goes into the if-block if it does not intersect
                    if (!check_intersection_return_to_appleton_neg_rot) {
                        rot_angle[1] = rounded_angle + rotation_angle;
                        rotated_line = new Line2D.Double(x_initial, y_initial, current_pos.nextPosition(rot_angle[1]).longitude, current_pos.nextPosition(rot_angle[1]).latitude);
                        // checks if the line segment from current position to next position intersects with the no-fly zones
                        if (check_intersection_no_fly_zone(rotated_line, no_fly_zones)) {
                            // if it intersects it either increments or decrements the rotation angle by 10
                            rotated_line = new Line2D.Double(x_initial, y_initial, current_pos.nextPosition(rot_angle[1] + 10).longitude, current_pos.nextPosition(rot_angle[1] + 10).latitude);
                            if (check_intersection_no_fly_zone(rotated_line, no_fly_zones))
                                rot_angle[1] -= 10;
                            else
                                rot_angle[1] += 10;
                        }
                        // checks if the next drone position is confined within the confinement zone
                        if (current_pos.nextPosition(rot_angle[1]).isConfined())
                            break;
                    }
                }
                // if the algorithm has found 2 possible rotations in either direction, it picks the one where the rotated destination point is closest to Appleton Tower
                if (!check_intersection_return_to_appleton_pos_rot && !check_intersection_return_to_appleton_neg_rot) {
                    if (dist_rot_point_from_dest[0] <= dist_rot_point_from_dest[1])
                        final_rotation = rot_angle[0];
                    else
                        final_rotation = rot_angle[1];
                } else if (!check_intersection_return_to_appleton_pos_rot)  // only found possible rotation in anticlockwise direction
                    final_rotation = rot_angle[0];
                else if (!check_intersection_return_to_appleton_neg_rot) // only found possible rotation in clockwise direction
                    final_rotation = rot_angle[1];
                else // no possible rotation found
                    break;
                // makes sure the angle is suitable
                final_rotation = getSuitableAngle(final_rotation);
                // makes the current position point to the next position
                current_pos = current_pos.nextPosition(final_rotation);
                flightpath = new Flightpath("appleton", temp.longitude, temp.latitude, final_rotation, current_pos.longitude, current_pos.latitude);
            } else {
                //if line segment doesn't intersect with any of the no-fly zones
                Line2D rotated_line = new Line2D.Double(current_pos.longitude, current_pos.latitude, current_pos.nextPosition(rounded_angle).longitude, current_pos.nextPosition(rounded_angle).latitude);
                // checks if the line segment from current position to next position intersects with the no-fly zones
                if (check_intersection_no_fly_zone(rotated_line, no_fly_zones) || !current_pos.nextPosition(rounded_angle).isConfined()) {
                    // if it intersects or leaves the confinement zone it either increments or decrements the rotation angle by 10
                    rotated_line = new Line2D.Double(current_pos.longitude, current_pos.latitude, current_pos.nextPosition(rounded_angle + 10).longitude, current_pos.nextPosition(rounded_angle + 10).latitude);
                    if (check_intersection_no_fly_zone(rotated_line, no_fly_zones) || !current_pos.nextPosition(rounded_angle + 10).isConfined())
                        rounded_angle -= 10;
                    else
                        rounded_angle += 10;
                }
                // makes sure the rounded angle is suitable
                rounded_angle = getSuitableAngle(rounded_angle);
                // makes the current position point to the next position
                current_pos = current_pos.nextPosition(rounded_angle);
                flightpath = new Flightpath("appleton", temp.longitude, temp.latitude, rounded_angle, current_pos.longitude, current_pos.latitude);
            }
            // appending the current position of the drone to the list of positions the drone moves to
            path.add(new double[]{current_pos.longitude, current_pos.latitude});
            // appends the flightpath object to the list of Flightpath objects
            return_to_appleton_flightpath.add(flightpath);
        }
        // appends the final move the drone makes
        return_to_appleton_flightpath.add(new Flightpath("appleton", current_pos.longitude, current_pos.latitude, HOVER_ANGLE, current_pos.longitude, current_pos.latitude));
        return path;
    }

    // calculates the distance from one point to another
    public double distance_from_to(double from_x, double from_y, double to_x, double to_y) {
        return Math.pow(from_x - to_x, 2) + Math.pow(from_y - to_y, 2);
    }

    /**
     * function to return a list of all the positions the drone has moved to throughout its journey delivering orders.
     * @param orders a list of all the orders placed
     * @param no_fly_zones the no-fly zones
     * @param database a database object
     * @return a list of all the positions where the drone moves to
     * @throws SQLException if there's an error accessing the database in one of the method called
     */
    public List<double[]> drone_movement(ArrayList<Orders> orders, List<List<List<Double>>> no_fly_zones, Database database) throws SQLException {
        // appends 2 dummy orders so that the drone returns to Appleton Tower
        orders.add(new Orders("appleton", Date.valueOf("2001-11-15"), "customer", "appleton", "empty", APPLETON_COORDS, APPLETON_COORDS, 0));
        orders.add(new Orders("appleton", Date.valueOf("2001-11-15"), "customer", "appleton", "empty", APPLETON_COORDS, APPLETON_COORDS, 0));
        // represents the previous order placed
        Orders prev_order = null;
        // represents the current position
        Drone current_position = this;
        // temporary variable holding the current position
        Drone temp;
        // checks if the line segment from current position to destination coordinate intersects with any of the no-fly zones
        boolean check_intersection;
        // angle between the current position and destination coordinate
        double angle;
        // rounded angle
        int rounded_angle;
        // list of positions that the drone moves to
        List<double[]> positions = new ArrayList<>();
        positions.add(APPLETON_COORDS);

        // a list representing the details of the moves the drone makes
        ArrayList<Flightpath> flightPaths = new ArrayList<>();
        Flightpath flightPath;
        // a list representing the deliveries made by the drone
        ArrayList<Deliveries> deliveries = new ArrayList<>();
        Deliveries delivery;
        int i;
        // iterates over the orders placed
        for (i = 0; i < orders.size(); ++i) {
            if (prev_order != null) {
                if (prev_order.getOrderNo().equals(orders.get(i).getOrderNo()))
                    // if the current and previous order number are same, destination coordinate is the previous orders' shop location
                    coords = prev_order.getShopCoords();
                else {
                    if (Arrays.equals(coords, prev_order.getShopCoords()))
                        // if the current destination equals the previous orders shop coordinates, destination is the previous orders' delivery location
                        coords = prev_order.getDeliverToCoords();
                    else {
                        // else current destination is the previous orders' shop location and the program stays at the current order for one more iteration
                        coords = prev_order.getShopCoords();
                        --i;
                    }
                }
            } else
                // if it's the first order, destination coordinate is the shop location
                coords = orders.get(i).getShopCoords();
            // line segment joining current position and destination coordinate
            Line2D line_curr_to_dest;
            // destination coordinate
            Drone destination_coords = new Drone(coords[0], coords[1]);
            // calculates moves between current position and destination coordinate
            int counter = 0;

            // while loop to check whether current position of the drone is close to destination coordinate
            while (!current_position.closeTo(destination_coords)) {
                ++counter;
                line_curr_to_dest = new Line2D.Double(current_position.longitude, current_position.latitude, coords[0], coords[1]);
                check_intersection = check_intersection_no_fly_zone(line_curr_to_dest, no_fly_zones);
                // angle between the current position and destination coordinate
                angle = Math.toDegrees(Math.atan2(coords[1] - current_position.latitude, coords[0] - current_position.longitude));
                // making sure the angle is suitable
                rounded_angle = getSuitableAngle(angle);
                // true if line segment intersects with any of the no-fly zones
                if (check_intersection) {
                    Line2D rotated_line;
                    double x_initial, y_initial, x_final, y_final;
                    int final_rotation;
                    // represents the rotation angle
                    int[] rot_angle = {0, 0};
                    // represents the distance from the rotated destination point to the destination coordinate
                    double[] dist_rot_point_from_dest = {2000, 2000};
                    // gets the current position
                    x_initial = line_curr_to_dest.getX1();
                    y_initial = line_curr_to_dest.getY1();
                    Point2D.Double point = new Point2D.Double(line_curr_to_dest.getX2(), line_curr_to_dest.getY2());
                    // for loop to rotate the line segment in an anticlockwise direction till it does not intersect with any of the no-fly zones
                    for (int rotation_angle = 0; rotation_angle <= 180; rotation_angle += 30) {
                        AffineTransform transform_matrix = getTransformationMatrix(rotation_angle, x_initial, y_initial);
                        x_final = transform_matrix.transform(point, null).getX();
                        y_final = transform_matrix.transform(point, null).getY();
                        dist_rot_point_from_dest[0] = distance_from_to(x_final, y_final, coords[0], coords[1]);
                        rotated_line = new Line2D.Double(x_initial, y_initial, x_final, y_final);
                        // checks if rotated line intersects with no-fly zones
                        check_intersection_no_fly_zone_pos_rot = check_intersection_no_fly_zone(rotated_line, no_fly_zones);
                        // goes into the if-block if it does not intersect
                        if (!check_intersection_no_fly_zone_pos_rot) {
                            rot_angle[0] = rotation_angle + rounded_angle;
                            rotated_line = new Line2D.Double(x_initial, y_initial, current_position.nextPosition(rot_angle[0]).longitude, current_position.nextPosition(rot_angle[0]).latitude);
                            // checks if the line segment from current position to next position intersects with the no-fly zones due to rounding off errors
                            if (check_intersection_no_fly_zone(rotated_line, no_fly_zones)) {
                                // if it intersects it either increments or decrements the rotation angle by 10
                                rotated_line = new Line2D.Double(x_initial, y_initial, current_position.nextPosition(rot_angle[0] + 10).longitude, current_position.nextPosition(rot_angle[0] + 10).latitude);
                                if (check_intersection_no_fly_zone(rotated_line, no_fly_zones))
                                    rot_angle[0] -= 10;
                                else
                                    rot_angle[0] += 10;
                            }
                            // makes sure the angle is suitable
                            final_rotation = rot_angle[0];
                            final_rotation = getSuitableAngle(final_rotation);
                            // checks if the next drone position is confined within the confinement zone
                            if (current_position.nextPosition(final_rotation).isConfined())
                                break;
                        }
                    }
                    // for loop to rotate the line segment in an clockwise direction till it does not intersect with any of the no-fly zones
                    for (int rotation_angle = -10; rotation_angle > -180; rotation_angle -= 30) {
                        AffineTransform transform_matrix = getTransformationMatrix(rotation_angle, x_initial, y_initial);
                        x_final = transform_matrix.transform(point, null).getX();
                        y_final = transform_matrix.transform(point, null).getY();
                        dist_rot_point_from_dest[1] = distance_from_to(x_final, y_final, coords[0], coords[1]);
                        rotated_line = new Line2D.Double(x_initial, y_initial, x_final, y_final);
                        check_intersection_no_fly_zone_neg_rot = check_intersection_no_fly_zone(rotated_line, no_fly_zones);
                        if (!check_intersection_no_fly_zone_neg_rot) {
                            rot_angle[1] = rounded_angle + rotation_angle;
                            rotated_line = new Line2D.Double(x_initial, y_initial, current_position.nextPosition(rot_angle[1]).longitude, current_position.nextPosition(rot_angle[1]).latitude);
                            if (check_intersection_no_fly_zone(rotated_line, no_fly_zones)) {
                                rotated_line = new Line2D.Double(x_initial, y_initial, current_position.nextPosition(rot_angle[1] + 10).longitude, current_position.nextPosition(rot_angle[1] + 10).latitude);
                                if (check_intersection_no_fly_zone(rotated_line, no_fly_zones))
                                    rot_angle[1] -= 10;
                                else
                                    rot_angle[1] += 10;
                            }
                            final_rotation = rot_angle[1];
                            final_rotation = getSuitableAngle(final_rotation);
                            // checks if the next drone position is confined within the confinement zone
                            if (current_position.nextPosition(final_rotation).isConfined())
                                break;
                        }
                    }
                    // if the algorithm has found 2 possible rotations in either direction, it picks the one where the rotated destination point is closest to destination coordinate
                    if (!check_intersection_no_fly_zone_pos_rot && !check_intersection_no_fly_zone_neg_rot) {
                        if (dist_rot_point_from_dest[0] <= dist_rot_point_from_dest[1])
                            final_rotation = rot_angle[0];
                        else
                            final_rotation = rot_angle[1];
                    } else if (!check_intersection_no_fly_zone_pos_rot) // only found possible rotation in anticlockwise direction
                        final_rotation = rot_angle[0];
                    else if (!check_intersection_no_fly_zone_neg_rot) // only found possible rotation in clockwise direction
                        final_rotation = rot_angle[1];
                    else // no possible rotation found
                        break;
                    // makes sure the angle is suitable
                    final_rotation = getSuitableAngle(final_rotation);
                    // makes a temporary variable to hold the next position
                    temp = current_position.nextPosition(final_rotation);
                    String orderNo;
                    if (prev_order != null) {
                        orderNo = prev_order.getOrderNo();
                    } else
                        orderNo = orders.get(i).getOrderNo();
                    flightPath = new Flightpath(orderNo, current_position.longitude, current_position.latitude, final_rotation, temp.longitude, temp.latitude);
                } else {
                    //if line segment doesn't intersect with any of the no-fly zones
                    Line2D rotated_line = new Line2D.Double(current_position.longitude, current_position.latitude, current_position.nextPosition(rounded_angle).longitude, current_position.nextPosition(rounded_angle).latitude);
                    // checks if the line segment from current position to next position intersects with the no-fly zones
                    if (check_intersection_no_fly_zone(rotated_line, no_fly_zones) || !current_position.nextPosition(rounded_angle).isConfined()) {
                        // if it intersects or leaves the confinement zone it either increments or decrements the rotation angle by 10
                        rotated_line = new Line2D.Double(current_position.longitude, current_position.latitude, current_position.nextPosition(rounded_angle + 10).longitude, current_position.nextPosition(rounded_angle + 10).latitude);
                        if (check_intersection_no_fly_zone(rotated_line, no_fly_zones) || !current_position.nextPosition(rounded_angle + 10).isConfined())
                            rounded_angle -= 10;
                        else
                            rounded_angle += 10;
                    }
                    // makes sure the rounded angle is suitable
                    rounded_angle = getSuitableAngle(rounded_angle);
                    String orderNo;
                    if (prev_order != null) {
                        orderNo = prev_order.getOrderNo();
                    } else
                        orderNo = orders.get(i).getOrderNo();
                    // makes a temporary variable to hold the next position
                    temp = current_position.nextPosition(rounded_angle);
                    flightPath = new Flightpath(orderNo, current_position.longitude, current_position.latitude, rounded_angle, temp.longitude, temp.latitude);
                }
                // holds the number of moves to go to appleton from the next position in the temporary variable
                int next_to_appleton = return_to_Appleton(new double[]{temp.longitude, temp.latitude}, no_fly_zones).size() + moves + 2;
                if (next_to_appleton <= 1500) {
                    // if the moves <= 1500, current position gets updated to next position
                    // flightpath object containing the details of the current move gets appended into a list of Flightpath objects
                    flightPaths.add(flightPath);
                    current_position = temp;
                    // the current position of the drone gets appended to the list of all positions the drone moves to
                    positions.add(new double[]{current_position.longitude, current_position.latitude});
                    // incrementing the total number of moves
                    ++moves;
                } else {
                    // if the number of moves exceeds 1500, the drone travels to appleton tower from current position
                    // holds the positions the drone makes to move from current position to Appleton tower
                    List<double[]> return_to_appleton_pos = return_to_Appleton(new double[]{current_position.longitude, current_position.latitude}, no_fly_zones);
                    // sets boolean value to true
                    check_max_moves = true;
                    // appends the list of positions the drone makes to move to Appleton to the list that holds the positions the drone moves to
                    positions.addAll(return_to_appleton_pos);
                    //positions.add(return_to_appleton_pos.get(return_to_appleton_pos.size() - 1));
                    // list of flightpath objects representing details of moves a drone makes to move to appleton tower gets appended to the list of flightpath objects that represents the details of all the moves the drone makes
                    flightPaths.addAll(return_to_appleton_flightpath);
                    // moves gets equated to current number of moves + size of list of positions the drone makes to return to appleton + 1 to hover at Appleton
                    moves = moves + return_to_appleton_pos.size() + 1;
                    // breaks out of the while loop
                    break;
                }
            }
            // if max moves reached, it breaks out of the for loop
            if (check_max_moves) {
                break;
            } else {
                // drone hovers for a turn
                current_position = current_position.nextPosition(HOVER_ANGLE);
                String orderNo;
                if (counter > 0) {
                    // checks if the drone makes at least one move to go from current to destination coordinate to check if an order has several items from the same shop
                    if (prev_order != null) {
                        orderNo = prev_order.getOrderNo();
                    } else {
                        orderNo = orders.get(i).getOrderNo();
                    }
                    flightPath = new Flightpath(orderNo, current_position.longitude, current_position.latitude, HOVER_ANGLE, current_position.longitude, current_position.latitude);
                    // appends flightpath object
                    flightPaths.add(flightPath);
                    // appends current position of the drone to the list of positions
                    positions.add(new double[]{current_position.longitude, current_position.latitude});
                    ++moves;
                }
            }
            prev_order = orders.get(i);
        }
        // calls method to insert list of flightpath objects into flightpath table
        database.insert_into_flightpath_database(flightPaths);

        Deliveries delivery_temp = new Deliveries("null", "null", 0);
        // gets the orderNo of the last delivery made
        String orderNo = delivery_temp.lastDeliveryMade(flightPaths);
        // for loop to iterate over each order placed
        for (i = 0; i < orders.size(); ++i) {
            int itemPrice;
            // adding the delivery fee to the item price
            itemPrice = orders.get(i).getItemPrice() + 50;
            // if not the last order placeed
            if (!orderNo.equals(orders.get(i).getOrderNo())) {
                // while current and the next order number are equal, keep adding the item price of individual items to itemPrice
                while (orders.get(i).getOrderNo().equals(orders.get(i + 1).getOrderNo())) {
                    itemPrice += orders.get(i + 1).getItemPrice();
                    ++i;
                }
                // make a Deliveries object storing values of the current order placed
                delivery = new Deliveries(orders.get(i).getOrderNo(), orders.get(i).getDeliverTo(), itemPrice);
                // append the object to the list of Deliveries object
                deliveries.add(delivery);
            } else {
                // if last order placed, find the total price of the order, create a Deliveries object and append it to the list of Deliveries object and break out of the loop
                while (orders.get(i).getOrderNo().equals(orders.get(i + 1).getOrderNo())) {
                    itemPrice += orders.get(i + 1).getItemPrice();
                    ++i;
                }
                delivery = new Deliveries(orders.get(i).getOrderNo(), orders.get(i).getDeliverTo(), itemPrice);
                deliveries.add(delivery);
                break;
            }
        }
        // calls method to insert list of deliveries objects into deliveries table
        database.insert_into_deliveries_database(deliveries);
        return positions;
    }

    /**
     * function to check if given line segment intersects with any of the no-fly zones
     * @param line a line segment representing the direction of drone movement
     * @param no_fly_zones the no-fly zones
     * @return true if the line segment intersects with any of the no-fly zones, false otherwise
     */
    public boolean check_intersection_no_fly_zone(Line2D line, List<List<List<Double>>> no_fly_zones) {
        Line2D line_no_fly_zone;
        List<Double> curr_point, next_point, initial_point;

        boolean check_intersection = false;
        for (List<List<Double>> no_fly_region : no_fly_zones) {
            for (int i = 0; i < no_fly_region.size(); ++i) {
                curr_point = no_fly_region.get(i);
                if (i == no_fly_region.size() - 1) {
                    initial_point = no_fly_region.get(0);
                    line_no_fly_zone = new Line2D.Double(curr_point.get(0), curr_point.get(1), initial_point.get(0), initial_point.get(1));
                } else {
                    next_point = no_fly_region.get(i + 1);
                    line_no_fly_zone = new Line2D.Double(curr_point.get(0), curr_point.get(1), next_point.get(0), next_point.get(1));
                }
                if (line.intersectsLine(line_no_fly_zone)) {
                    check_intersection = true;
                    break;
                }
            if (check_intersection)
                break;
            }
        }
        return check_intersection;
    }
}
