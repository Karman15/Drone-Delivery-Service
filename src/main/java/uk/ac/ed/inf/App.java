package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * class for controlling the main functionality of the application,
 * it processes the command-line arguments, calls methods to create the tables needed, get the orders placed (from the database server) on the given date passed as command-line arguments,
 * get the no-fly zones from the web server and get the positions the drone has moved to throughout its journey by calling the drone movement algorithm,
 * it also creates a geoJSON file with the positions supplied as coordinates for a LineString.
 *
 */
public class App {
    // the coordinates of Appleton Tower, where the drone begins and ends its delivery service for the day
    private static final double[] APPLETON_COORDS = {-3.1869, 55.9445};

    // the server name
    private static final String server = "localhost";

    /**
     * function to make a geoJSON string with the list of positions of the drone
     * @param drone_movement a list containing all the positions the drone had occupied on a given date
     * @return a geoJSON string
     */
    public static String makeGeoJSON(List<double[]> drone_movement) {
        List<Point> points = new ArrayList<>();
        for (double[] positions : drone_movement) {
            Point point = Point.fromLngLat(positions[0], positions[1]);
            points.add(point);
        }
        LineString lineString = LineString.fromLngLats(points);
        Geometry geometry = lineString;
        Feature feature = Feature.fromGeometry(geometry);
        FeatureCollection featureCollection = FeatureCollection.fromFeature(feature);

        String geoJSON = featureCollection.toJson();

        return geoJSON;
    }

    /**
     * function to create a geoJSON file
     * @param geoJSON a geoJSON string
     * @param date date received as command-line arguments
     */
    public static void createFile(String geoJSON, String date) {
        try {
            File file = new File("." + File.separator + "drone-" + date + ".geojson");
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            System.out.println("Writing geoJSON string to file");
            System.out.println("-----------------------");
            fileWriter.write(geoJSON);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException err) {
            err.getStackTrace();
        }
    }

    /**
     * main function to control the functionality of the application
     * @param args command line arguments including the date of delivery, the web server and database port number
     * @throws SQLException if there's an error accessing the database
     */
    public static void main(String[] args) throws SQLException {
        // date to pass to constructors of the Database class
        String str_date = args[2] + "-" + args[1] + "-" + args[0];
        // date format to save the geoJSON file with
        String str_date_for_file = args[0] + "-" + args[1] + "-" + args[2];
        String database_port = args[4];
        String webserver_port = args[3];
        System.out.println(str_date + database_port + webserver_port + server);

        // creating a Database object
        Database database = new Database(server, database_port, webserver_port, str_date);

        // calling methods to create the tables needed
        database.create_deliveries_table();
        database.create_deliveries_flightpath();

        // gets a list of orders placed on the given date
        ArrayList<Orders> orders = database.getOrdersTable();

        WebServer w1 = new WebServer(server, webserver_port);
        // gets the no-fly zones
        List<List<List<Double>>> no_fly_zones = w1.get_no_fly_zones();

        Drone l1 = new Drone(APPLETON_COORDS[0], APPLETON_COORDS[1]);
        // calls the drone_movement method which returns a list of positions the drone has moved to throughout its journey
        List<double[]> drone_movement = l1.drone_movement(orders, no_fly_zones, database);

        // makes and creates a geoJSON file
        String geoJSON = makeGeoJSON(drone_movement);
        createFile(geoJSON, str_date_for_file);
    }
}
