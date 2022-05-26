package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * class representing the operations of the application which are dependent on the web server at port number supplied as command-line arguments
 */
public class WebServer {

    // the server name and the server port number
    private final String server, port;

    // maps food item to its price
    private static HashMap<String, Integer> item_price = new HashMap<>();
    // maps food item to its shop location
    private static HashMap<String, String> shop_loc = new HashMap<>();

    // number response.StatusCode() returns if the request made to the
    // server does not fail
    private static final int POSITIVE_STATUS_CODE = 200;

    // an HttpClient object used to send requests and retrieve their responses
    // shared between all HTTPRequests
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * constructor of the class to assign the server name and the server port number
     * @param server the server name
     * @param port the port number
     */
    public WebServer(String server, String port) {
        this.server = server;
        this.port = port;
    }

    public String getServer() {
        return server;
    }

    public String getPort() {
        return port;
    }

    /**
     * function to get the no-fly zones from the geoJSON file on the web server
     * @return a list of all the no-fly zones represented as list of points which is further represented as list of coordinates
     */
    public List<List<List<Double>>> get_no_fly_zones() {
        String geoJSON = "http://" + getServer() + ":" + getPort() + "/buildings/no-fly-zones.geojson";
        // HttpRequest object to access a resource on the server
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geoJSON))
                .build();
        List<List<List<Double>>> no_fly_array = new ArrayList<>();
        try {
            // HttpResponse object provide the client with the resource it requested,
            // or inform the client that the action it requested has been carried out;
            // or else to inform the client that an error occurred in processing its request.
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            // if the request does not fail
            if (response.statusCode() == POSITIVE_STATUS_CODE) {
                // getting the individual elements from a geoJSON file
                FeatureCollection fc = FeatureCollection.fromJson(response.body());
                List<Feature> features = fc.features();
                Geometry geom;
                Polygon poly;
                List<List<Point>> point;
                // iterates over the feature elements of the FeatureCollection element
                for (Feature feature : features) {
                    geom = feature.geometry();
                    poly = (Polygon) geom;
                    point = poly.coordinates();
                    List<List<Double>> p = new ArrayList<>();
                    // getting the list of points for each no-fly region
                    for (int i = 0; i < point.get(0).size(); ++i)
                        p.add(point.get(0).get(i).coordinates());
                    no_fly_array.add(p);
                }
            }
        } catch (IOException | InterruptedException | NullPointerException err) {
            err.printStackTrace();
        }
        return no_fly_array;
    }

    /**
     * function to get the coordinates from the json file given a What3Words string
     * @param string a What3Words string
     * @return the coordinates
     */
    public double[] get_coords(String string) {
        String[] what3words = string.split("\\.");
        // HttpRequest object to access a resource on the server
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + getServer() + ":" + getPort() + "/words/" + what3words[0] + "/" + what3words[1] + "/" + what3words[2] + "/details.json"))
                .build();
        double[] coords = null;
        try {
            // HttpResponse object provide the client with the resource it requested,
            // or inform the client that the action it requested has been carried out;
            // or else to inform the client that an error occurred in processing its request.
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == POSITIVE_STATUS_CODE) {
                // splits the json string on every ":" and accesses particular elements
                String longitude = response.body().split(":")[11];
                String latitude = response.body().split(":")[12];
                // splits the string containing longitude and latitude on particular expressions and accesses the first element
                double lon = Double.parseDouble(longitude.split(",")[0]);
                double lat = Double.parseDouble(latitude.split("}")[0]);
                coords = new double[]{lon, lat};
            }
        } catch (IOException | InterruptedException err) {
            err.printStackTrace();
        }
        return coords;
    }

    /**
     * function to get the price of given item and the shop which sells the item
     * @param food_item the food item
     * @return an object containing the price of given item and location of the shop
     */
    public ItemPriceShopLocation getPriceLocation(String food_item) {
        // HttpRequest object to access a resource on the server
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + getServer() + ":" + getPort() + "/menus/menus.json"))
                .build();
        String shopLocation;
        ItemPriceShopLocation object = new ItemPriceShopLocation();
        int itemPrice = 0;
        try {
            // HttpResponse object provide the client with the resource it requested,
            // or inform the client that the action it requested has been carried out;
            // or else to inform the client that an error occurred in processing its request.
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == POSITIVE_STATUS_CODE) {
                Type listType =
                        new TypeToken<ArrayList<Shops>>() {
                        }.getType();
                ArrayList<Shops> shopsList =
                        new Gson().fromJson(response.body(), listType);
                int check = 0;
                Integer price = item_price.get(food_item);
                String loc = shop_loc.get(food_item);
                // checks if the food item maps to anything in the hashmap
                if (price == null)
                    //loops through the available shops to find the food item
                    for (Shops shops : shopsList) {
                        for (MenuItems items : shops.getMenu())
                            if (items.getItem().equals(food_item)) {
                                itemPrice += items.getPence();
                                object.setPrice(itemPrice);
                                item_price.put(food_item, itemPrice);
                                shopLocation = shops.getLocation();
                                object.setLocation(shopLocation);
                                shop_loc.put(food_item, shopLocation);
                                check = 1;
                                break;
                            }
                        if (check == 1)
                            break;
                    }
                price = item_price.get(food_item);
                loc = shop_loc.get(food_item);
                object.setPrice(price);
                object.setLocation(loc);
            }
        } catch (IOException | InterruptedException err) {
            System.err.println("Fatal error: Unable to connect to " +
                    server + " at port " + port + ".");
        }
        return object;
    }
}
