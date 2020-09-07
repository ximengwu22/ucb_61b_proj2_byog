
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    Map<Long, Vertice> vertice = new LinkedHashMap<>();
    Map<Long, Way> way = new LinkedHashMap<>();
    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        //@source : https://stackoverflow.com/questions/8104692/
        // how-to-avoid-java-util-concurrentmodificationexception-when-iterating-through-an
        ArrayList<Long> aloneNode = new ArrayList<>();
        for (Long i : vertice.keySet()) {
            //System.out.println(i + "has adj: " + vertice.get(i).adjacent);
            if (vertice.get(i).adjacent.size() == 0 || vertice.get(i).adjacent == null) {
                aloneNode.add(i);
            }
        }
        vertice.keySet().removeAll(aloneNode);
        //System.out.println("==After delete: " + vertices());
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE
        return vertice.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return vertice.get(v).adjacent;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double maxRange = Double.MAX_VALUE;
        double minV = maxRange;
        long minID = 0;
        for (long i : vertice.keySet()) {
            if (distance(lon, lat, lon(i), lat(i)) < minV) {
                minID = i;
                minV = distance(lon, lat, lon(i), lat(i));
            }
        }
        return minID;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return vertice.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return vertice.get(v).lat;
    }

    void addVertice(Vertice n) {
        vertice.put(n.id, n);
    }

    void deleteVertice(Vertice n) {
        vertice.remove(n.id);
    }

    void addEdge(long v1, long v2) {
        //add continuous two adjacent vertices in the same way to adjs of each other
        vertice.get(v1).addAdj(v2);
        vertice.get(v2).addAdj(v1);
    }

    void addWay(Long id, Way w) {
        //add all nodes in a way
        if (!way.containsKey(id)) {
            way.put(id, w);
        } else {
            return;
        }
    }

    void addRef(Long wid, ArrayList<Long> niw) {
        if (way.containsKey(wid)) {
            way.get(wid).ref = niw;
        } else {
            return;
        }
    }
    ArrayList<Long> getRef() {
        ArrayList<Long> n = new ArrayList<>();
        for (Long i : way.keySet()) {
            n.add(i);
        }
        return n;
    }
    //void addInfo(Long id, String name, String info) {
//        way.get(id).extraInfo.put(name, info);
//    }
    void setNodeName(Long id, String n) {
        vertice.get(id).name = n;
    }

    ArrayList<Vertice> getVertive() {
        ArrayList<Vertice>  gv = new ArrayList<>();
        for (Vertice i : this.vertice.values()) {
            gv.add(i);
        }
        return gv;
    }
    /**
     * A Vertice.
     */
    public static class Vertice {
        Long id;
        Double lon, lat;
        String name;
        ArrayList<Long> adjacent; //id & distance from target
        Vertice preNode;
        double priority;

        public Vertice(Long id, Double lon, Double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.adjacent = new ArrayList<>();
            this.name = null;
            preNode = null;
            priority = Double.POSITIVE_INFINITY;
        }
        void addAdj(Long vid) {
            adjacent.add(vid);
        }
    }

    static class Way {
        ArrayList<Long> ref;  //nodes in way
        Long id;
        Long dist;
        Map<String, String> extraInfo; //type, length, information
        Way(Long id) {
            this.ref = new ArrayList<>();
            this.id = id;
            this.dist = null;
            extraInfo = new HashMap<>();
        }
    }
}
