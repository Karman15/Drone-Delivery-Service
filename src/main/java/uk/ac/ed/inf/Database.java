package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * class representing the operations of the application which are dependent on the Apache Derby Database at port number supplied as command-line arguments
 */
public class Database {

    // the server name, database and web server port number and the date on which orders are placed
    private final String server, db_port, server_port, date;

    /**
     * constructor of the class to assign the server name, database and web server port number and the date on which orders are placed
     *
     * @param server the server name
     * @param db_port the database port number
     * @param server_port the web server port number
     * @param date the date on which orders are placed
     */
    public Database(String server, String db_port, String server_port, String date) {
        this.server = server;
        this.db_port = db_port;
        this.server_port = server_port;
        this.date = date;
    }

    public String getDb_port() {
        return db_port;
    }

    public String getServer_port() {
        return server_port;
    }

    public String getServer() {
        return server;
    }

    public String getDate() {
        return date;
    }

    /**
     * function to get all the orders placed on the given date by accessing the database server on the given port number
     * @return a list of orders placed
     */
    public ArrayList<Orders> getOrdersTable() {
        try {
            // a jdbc string to access the database
            String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";

            // getting a connection to the database
            Connection conn = DriverManager.getConnection(jdbcString);

            // statement to be executed, it joins the 2 tables (orders, orderDetails) based on their order number
            final String deliveryDateQuery =
                    "select * from orders o1, orderDetails o2 where deliveryDate=(?) and o1.orderNo = o2.orderNo order by o1.orderNo";
            PreparedStatement psdeliveryDateQuery =
                    conn.prepareStatement(deliveryDateQuery);
            // sets the deliveryDate in the sql statement to be equal to the date passed to the constructor
            psdeliveryDateQuery.setString(1, getDate());

            // sql statement executed
            ResultSet rs = psdeliveryDateQuery.executeQuery();
            ArrayList<Orders> orders = new ArrayList<>();
            WebServer w = new WebServer(this.getServer(), this.getServer_port());
            double[] deliverToCoords;
            double[] shopCoords;
            int price;

            // maps DeliverTo What3Words to the (Longitude, Latitude) coordinate
            HashMap<String, double[]> mapDeliverCoords = new HashMap<>();
            // maps item shop location What3Words to the (Longitude, Latitude) coordinate
            HashMap<String, double[]> mapShopCoords = new HashMap<>();
            // maps a food item to its price
            HashMap<String, Integer> mapItemPrice = new HashMap<>();
            ItemPriceShopLocation price_loc = new ItemPriceShopLocation();
            // while loop to iterate through every row of the sql table that was received after executing the sql statement
            while (rs.next()) {
                String orderNo = rs.getString("orderNo");
                Date deliveryDate = rs.getDate("deliveryDate");
                String customer = rs.getString("customer");
                String deliverTo = rs.getString("deliverTo");
                String item = rs.getString("item");

                // getting the coordinates from the deliverTo What3Words by calling a method in the WebServer class if the deliverTo string does not map to anything in the Hashmap
                deliverToCoords = mapDeliverCoords.get(deliverTo);
                if (deliverToCoords == null) {
                    deliverToCoords = w.get_coords(deliverTo);
                    mapDeliverCoords.put(deliverTo, deliverToCoords);
                }

                // getting the coordinates from the shop location What3Words by calling a method in the WebServer class if the shop location string does not map to anything in the Hashmap
                shopCoords = mapShopCoords.get(item);
                if (shopCoords == null) {
                    // gets the shop location and the price of the item
                    price_loc = w.getPriceLocation(item);
                    shopCoords = w.get_coords(price_loc.getLocation());
                    price = price_loc.getPrice();
                    mapShopCoords.put(item, shopCoords);
                    mapItemPrice.put(item, price);
                }
                price = mapItemPrice.get(item);

                // initialising an Orders object by passing all the necessary fields of an order to the constructor
                Orders order = new Orders(orderNo, deliveryDate, customer, deliverTo, item, deliverToCoords, shopCoords, price);
                // adds the order to a list of Orders objects
                orders.add(order);
            }
            // calls a method to sort the list of orders based on the total price of an order
            orders = create_insert_orders_sort_table(orders);
            return orders;

        } catch (SQLException err) {
            err.printStackTrace();
        }
        return null;
    }

    /**
     * function to sort a list of Orders in descending order based on the total price of each order by accessing the database
     * @param orders a list of Orders placed
     * @return sorted list of Orders placed
     * @throws SQLException if there's an error accessing the database
     */
    public ArrayList<Orders> create_insert_orders_sort_table(ArrayList<Orders> orders) throws SQLException {
        String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";
        Connection conn = DriverManager.getConnection(jdbcString);
        // creating a statement object that we can use for running various
        // SQL statement commands against the database.
        Statement statement = conn.createStatement();

        DatabaseMetaData databaseMetadata = conn.getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables(null, null, "ORDERS_SORT", null);
        // If the resultSet is not empty then the table exists, so we can drop it
        if (resultSet.next()) {
            statement.execute("drop table orders_sort");
        }

        // creates an orders_sort table in order to sort the orders table
        statement.execute(
                "create table orders_sort(" +
                        "orderNo char(8), " +
                        "deliveryDate DATE, " +
                        "customer char(8), " +
                        "deliverTo varchar(19), " +
                        "item varchar(58), " +
                        "deliverToCoords varchar(30), " +
                        "shopCoords varchar(30), " +
                        "itemPrice int)"
        );
        PreparedStatement psOrders = conn.prepareStatement(
                "insert into orders_sort values (?, ?, ?, ?, ?, ?, ?, ?)");

        // inserting all the orders' information into the table
        for (Orders order : orders) {
            psOrders.setString(1, order.getOrderNo());
            psOrders.setDate(2, order.getDeliveryDate());
            psOrders.setString(3, order.getCustomer());
            psOrders.setString(4, order.getDeliverTo());
            psOrders.setString(5, order.getItem());
            // inserting the coordinates as a String
            psOrders.setString(6, String.valueOf(order.getDeliverToCoords()[0]) + ' ' + order.getDeliverToCoords()[1]);
            psOrders.setString(7, String.valueOf(order.getShopCoords()[0]) + ' ' + order.getShopCoords()[1]);
            psOrders.setInt(8, order.getItemPrice());
            psOrders.execute();
        }

        ArrayList<Orders> sorted_orders = new ArrayList<>();

        // statement to sort the orders table
        final String deliveryDateQuery =
                "select o1.orderNo, o2.deliveryDate, o2.customer, o2.deliverTo, o2.item, o2.deliverToCoords, o2.shopCoords, o2.itemPrice from (select orderNo from orders_sort group by orderNo order by SUM(itemPrice) DESC) o1, orders_sort o2 where o1.orderNo = o2.orderNo";
        PreparedStatement psdeliveryDateQuery =
                conn.prepareStatement(deliveryDateQuery);
        ResultSet rs = psdeliveryDateQuery.executeQuery();

        // while loop to get the sorted list of orders
        while (rs.next()) {
            String orderNo = rs.getString("orderNo");
            Date deliveryDate = rs.getDate("deliveryDate");
            String customer = rs.getString("customer");
            String deliverTo = rs.getString("deliverTo");
            String item = rs.getString("item");
            // getting the coordinates from the String
            String deliverToLong = rs.getString("deliverToCoords").split(" ")[0];
            String deliverToLat = rs.getString("deliverToCoords").split(" ")[1];
            double[] deliverToCoords = new double[]{Double.parseDouble(deliverToLong), Double.parseDouble(deliverToLat)};
            int price = rs.getInt("itemPrice");
            // getting the coordinates from the String
            String shopLong = rs.getString("shopCoords").split(" ")[0];
            String shopLat = rs.getString("shopCoords").split(" ")[1];
            double[] shopCoords = new double[]{Double.parseDouble(shopLong), Double.parseDouble(shopLat)};
            Orders o = new Orders(orderNo, deliveryDate, customer, deliverTo, item, deliverToCoords, shopCoords, price);
            sorted_orders.add(o);
        }

        // statement to drop the orders_sort table
        statement.execute("drop table orders_sort");

        return sorted_orders;
    }

    /**
     * function to create a deliveries table by accessing the database
     * @throws SQLException if there's an error accessing the database
     */
    public void create_deliveries_table() throws SQLException {
        String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";
        Connection conn = DriverManager.getConnection(jdbcString);
        // creating a statement object that we can use for running various
        // SQL statement commands against the database.
        Statement statement = conn.createStatement();

        DatabaseMetaData databaseMetadata = conn.getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables(null, null, "DELIVERIES", null);
        // If the resultSet is not empty then the table exists, so we can drop it
        if (resultSet.next()) {
            statement.execute("drop table deliveries");
        }
        statement.execute(
                "create table deliveries(" +
                        "orderNo char(8), " +
                        "deliveredTo varchar(19), " +
                        "costInPence int)"
        );
    }

    /**
     * function to create a flightpath table by accessing the database
     * @throws SQLException if there's an error accessing the database
     */
    public void create_deliveries_flightpath() throws SQLException {
        String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";
        Connection conn = DriverManager.getConnection(jdbcString);
        // creating a statement object that we can use for running various
        // SQL statement commands against the database.
        Statement statement = conn.createStatement();

        DatabaseMetaData databaseMetadata = conn.getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables(null, null, "FLIGHTPATH", null);
        // If the resultSet is not empty then the table exists, so we can drop it
        if (resultSet.next()) {
            statement.execute("drop table flightpath");
        }
        statement.execute(
                "create table flightpath(" +
                        "orderNo char(8), " +
                        "fromLongitude double, " +
                        "fromLatitude double, " +
                        "angle integer, " +
                        "toLongitude double, " +
                        "toLatitude double)"
        );
    }

    /**
     * function to insert values into the flightpath table by accessing the database
     * @param flightPaths a list of Flightpath objects which provide a detailed record of every move
     * made by the drone while making the day’s lunch deliveries
     * @throws SQLException if there's an error accessing the database
     */
    public void insert_into_flightpath_database(ArrayList<Flightpath> flightPaths) throws SQLException {
        String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";
        Connection conn = DriverManager.getConnection(jdbcString);
        PreparedStatement psFlightpath = conn.prepareStatement(
                "insert into flightpath values (?, ?, ?, ?, ?, ?)");

        for (Flightpath flightpath : flightPaths) {
            psFlightpath.setString(1, flightpath.getOrderNo());
            psFlightpath.setDouble(2, flightpath.getFromLongitude());
            psFlightpath.setDouble(3, flightpath.getFromLatitude());
            psFlightpath.setInt(4, flightpath.getAngle());
            psFlightpath.setDouble(5, flightpath.getToLongitude());
            psFlightpath.setDouble(6, flightpath.getToLatitude());
            psFlightpath.execute();
        }
    }

    /**
     * function to insert values into the deliveries table by accessing the database
     * @param deliveries a list of every lunch delivery which the drone makes
     * @throws SQLException if there's an error accessing the database
     */
    public void insert_into_deliveries_database(ArrayList<Deliveries> deliveries) throws SQLException {
        String jdbcString = "jdbc:derby://" + this.getServer() + ":" + this.getDb_port() + "/derbyDB";
        Connection conn = DriverManager.getConnection(jdbcString);
        // creating a statement object that we can use for running various
        // SQL statement commands against the database.
        //Statement statement = conn.createStatement();

        PreparedStatement psFlightpath = conn.prepareStatement(
                "insert into deliveries values (?, ?, ?)");

        for (Deliveries delivery : deliveries) {
            psFlightpath.setString(1, delivery.getOrderNo());
            psFlightpath.setString(2, delivery.getDeliveredTo());
            psFlightpath.setInt(3, delivery.getCostInPence());
            psFlightpath.execute();
        }
    }
}
