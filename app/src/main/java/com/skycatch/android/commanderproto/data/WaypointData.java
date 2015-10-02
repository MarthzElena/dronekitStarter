package com.skycatch.android.commanderproto.data;



/**
 * Created by marthaelena on 9/29/15.
 */
public class WaypointData {

    public String type;
    public DataProperties properties;
    public DataGeometry geometry;


    public static class DataGeometry {
        public String type;
        public double[] coordinates;

        public double getLat() {
            return coordinates[1];
        }

        public double getLng() {
            return coordinates[0];
        }
    }

    public static class DataProperties {
        public WaypointMarker marker;
    }

    public static class WaypointMarker {
        public String icon;
    }

}
