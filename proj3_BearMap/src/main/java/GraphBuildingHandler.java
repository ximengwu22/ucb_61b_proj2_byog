import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads for
 *  pathfinding, under some constraints.
 *  See OSM documentation on
 *  <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 *  and the java
 *  <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 *
 *  You may find the CSCourseGraphDB and CSCourseGraphDBHandler examples useful.
 *
 *  The idea here is that some external library is going to walk through the XML
 *  file, and your override method tells Java what to do every time it gets to the next
 *  element in the file. This is a very common but strange-when-you-first-see it pattern.
 *  It is similar to the Visitor pattern we discussed for graphs.
 *
 *  @author Alan Yao, Maurice Lee
 */
public class GraphBuildingHandler extends DefaultHandler {
    /**
     * Only allow for non-service roads; this prevents going on pedestrian streets as much as
     * possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
     * roads, but in practice we walk all over them with such impunity that we forget cars can
     * actually drive on them.
     */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private final GraphDB g;

    //private ArrayList<Long> nodesInWay;
    private Long lastVertice;
    private boolean isWayValid;
    private GraphDB.Way way;


    /**
     * Create a new GraphBuildingHandler.
     * @param g The graph to populate with the XML data.
     */
    public GraphBuildingHandler(GraphDB g) {
        this.g = g;
        lastVertice = null;
        isWayValid = false;
    }

    /**
     * Called at the beginning of an element. Typically, you will want to handle each element in
     * here, and you may want to track the parent element.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available. This tells us which element we're looking at.
     * @param attributes The attributes attached to the element. If there are no attributes, it
     *                   shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see Attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        /* Some example code on how you might begin to parse XML files. */
        if (qName.equals("node")) {
            /* We encountered a new <node...> tag. */
            activeState = "node";

            /* TODO Use the above information to save a "node" to somewhere. */
            /* Hint: A graph-like structure would be nice. */
            Long id = Long.valueOf(attributes.getValue("id"));
            Double lon = Double.valueOf(attributes.getValue("lon"));
            Double lat = Double.valueOf(attributes.getValue("lat"));
//            System.out.println("Node id: " + id);
//            System.out.println("Node lon: " + lon);
//            System.out.println("Node lat: " + lat);
            GraphDB.Vertice newNode = new GraphDB.Vertice(id, lon, lat);
            g.addVertice(newNode);
            lastVertice = newNode.id;

        } else if (qName.equals("way")) {
            /* We encountered a new <way...> tag. */
            activeState = "way";
            // System.out.println("Beginning a way...");
            Long wid = Long.valueOf(attributes.getValue("id"));
            way = new GraphDB.Way(wid);
            //lastVertice = null;
            //System.out.println("====Beginning a way..." + way.id);
        } else if (activeState.equals("way") && qName.equals("nd")) {
            /* While looking at a way, we found a <nd...> tag. */
            //System.out.println("Id of a node in this way: " + attributes.getValue("ref"));

            /* TODO Use the above id to make "possible" connections between the nodes in this way */
            /* Hint1: It would be useful to remember what was the last node in this way. */
            /* Hint2: Not all ways are valid. So, directly connecting the nodes here would be
            cumbersome since you might have to remove the connections if you later see a tag that
            makes this way invalid. Instead, think of keeping a list of possible connections and
            remember whether this way is valid or not. */
            Long ref = Long.valueOf(attributes.getValue("ref"));
            way.ref.add(ref);
            lastVertice = ref;
            //System.out.println("Nodes added in way: " + way.ref);


        } else if (activeState.equals("way") && qName.equals("tag")) {
            /* While looking at a way, we found a <tag...> tag. */
//            Long wid = Long.valueOf(attributes.getValue("id"));
            String k = attributes.getValue("k");
            String v = attributes.getValue("v");
            //Long wid = Long.valueOf(attributes.getValue("way"));
            if (k.equals("maxspeed")) {
                //System.out.println("Max Speed: " + v);
                /* TODO set the max speed of the "current way" here. */
                String x = attributes.getValue("maxspeed");
            } else if (k.equals("highway")) {
                /* TODO Figure out whether this way and its connections are valid. */
                /* Hint: Setting a "flag" is good enough! */
                isWayValid = ALLOWED_HIGHWAY_TYPES.contains(v); //flag
                //System.out.println("Highway type: " + v + "==is valid" + isWayValid);

            } else if (k.equals("name")) {
                // System.out.println("~~Way : ");
                String name = attributes.getValue("name");
                //way.addInfo(k, v);
//                System.out.println("way EXtra Info :" + way.extraInfo + " ====wayID  " + way.id);
            }
            // System.out.println("Tag with k=" + k + ", v=" + v + ".");
        } else if (activeState.equals("node") && qName.equals("tag") && attributes.getValue("k")
                .equals("name")) {
            /* While looking at a node, we found a <tag...> with k="name". */
            /* TODO Create a location. */
            /* Hint: Since we found this <tag...> INSIDE a node, we should probably remember which
            node this tag belongs to. Remember XML is parsed top-to-bottom, so probably it's the
            last node that you looked at (check the first if-case). */
            //System.out.println("Node's name: " + attributes.getValue("v"));
            String v = attributes.getValue("v");
            //Long nid = Long.valueOf(attributes.getValue("id"));
            //g.addInfo(wid, k, v);
            g.setNodeName(lastVertice, v);
        }
    }

    /**
     * Receive notification of the end of an element. You may want to take specific terminating
     * actions here, like finalizing vertices or edges found.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available.
     * @throws SAXException  Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("way")) {
            /* We are done looking at a way. (We finished looking at the nodes, speeds, etc...)*/
            /* Hint1: If you have stored the possible connections for this way, here's your
            chance to actually connect the nodes together if the way is valid. */
            //System.out.println("Finishing a way...");
            //ArrayList<Long> ref = way.ref;
            if (isWayValid) {
                for (int i = 1; i < way.ref.size(); i++) {
                    //connect the nearby nodes together
                    g.addEdge(way.ref.get(i - 1), way.ref.get(i));
                }
                isWayValid = false;
            }
            way = null; //reset the way
        }
    }

}
