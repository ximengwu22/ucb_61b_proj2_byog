
import java.util.HashMap;
import java.util.Map;


/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {

    double xstart, xend, ystart, yend, blockx, blocky;
    int depth;
    public Rasterer() {
        // YOUR CODE HERE
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        System.out.println(params);
        params.get("w");
        Map<String, Object> results = new HashMap<>();
        //System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
        //                   + "your browser.");
        results.clear();
        //initialization;

        String[][] renderGrid = getGrid(params);

        double rasterUlLon = this.xstart * blockx + MapServer.ROOT_ULLON;
        double rasterUlLat = MapServer.ROOT_ULLAT - this.ystart * blocky;
        double rasterLrLon = MapServer.ROOT_LRLON - (Math.pow(2, this.depth)
                - 1 - this.xend) * blockx;
        double rasterLrLat = MapServer.ROOT_LRLAT + (Math.pow(2, this.depth)
                - 1 - this.yend) * blocky;
        //double width = params.get("w"), height = params.get("h");

        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");

        Boolean querySuccess;
        if (ullon < MapServer.ROOT_ULLON || ullon > lrlon || ullon > MapServer.ROOT_LRLON
                || ullat > MapServer.ROOT_ULLAT || ullat < MapServer.ROOT_LRLAT || ullat < lrlat) {
            querySuccess = false;
        } else {
            querySuccess = true;
        }

        results.put("render_grid", renderGrid);
        results.put("raster_ul_lon", rasterUlLon);
        results.put("raster_ul_lat", rasterUlLat);
        results.put("raster_lr_lon", rasterLrLon);
        results.put("raster_lr_lat", rasterLrLat);
        results.put("depth", depth);
        results.put("query_success", querySuccess);
        return results;
    }

    private String[][] getGrid(Map<String, Double> params) {
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");
        double width = params.get("w"), height = params.get("h");
        double lonDPP = (lrlon - ullon) / width;

        double d0 = (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON) / MapServer.TILE_SIZE;
        double depth0 = Math.log(d0 / lonDPP) / Math.log(2);
        this.depth = (int) Math.ceil(depth0);
        if (this.depth > 7) {
            this.depth = 7;
        }

        double blockY = Math.abs((MapServer.ROOT_LRLAT - MapServer.ROOT_ULLAT))
                / Math.pow(2.0, this.depth);
        double blockX = Math.abs((MapServer.ROOT_LRLON - MapServer.ROOT_ULLON))
                / Math.pow(2.0, this.depth);
        this.blocky = blockY;
        this.blockx = blockX;

        int xStart = 0; // = ul_lon / block;
        int xEnd = 0; // = (ul_lon + width) / block;
        int yStart = 0; // = ul_lat / block;
        int yEnd = 0; // = (lr_lat + height) / block;


        for (int i = 0; i < Math.pow(2, this.depth); i++) {
            if (ullon >= (MapServer.ROOT_ULLON + i * blockX)
                    && ullon < ((i + 1) * blockX + MapServer.ROOT_ULLON)) {
                xStart = i;
            }
            if (ullat >= (MapServer.ROOT_LRLAT + i * blockY)
                    && ullat < ((i + 1) * blockY + MapServer.ROOT_LRLAT)) {
                yStart = (int) Math.pow(2, this.depth) - 1 - i;
            }
            if (lrlat >= (MapServer.ROOT_LRLAT + i * blockY)
                    && (lrlat < (i + 1) * blockY + MapServer.ROOT_LRLAT)) {
                yEnd = (int) Math.pow(2, this.depth) - 1 - i;
            }
            if (lrlon >= (MapServer.ROOT_ULLON + i * blockX)
                    && (lrlon < (i + 1) * blockX + MapServer.ROOT_ULLON)) {
                xEnd = i;
            }
        }

        this.xstart = xStart;
        this.xend = xEnd;
        this.yend = yEnd;
        this.ystart = yStart;

        String[][] rendergrid = new String[(int) (yend - ystart) + 1][(int) (xEnd - xStart) + 1];

        for (int i = 0; i < xend - xStart + 1; i++) {
            for (int j = 0; j < yend - ystart + 1; j++) {
                rendergrid[j][i] = "d" + this.depth + "_x"
                        + (int) (i + xStart) + "_y" + (int) (j + yStart) + ".png";
            }
        }

        return rendergrid;
    }

//    private Boolean isQureySuccess() {
//        if ()
//        return true;
//    }

}
