package com.skycatch.android.commanderproto.data;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marthaelena on 9/25/15.
 */
public class CommanderZone {

    public String id;
    public String type;
    public ZoneRoutes[] routes;
    public ZonesData data;
    public double area;
    public double routesDistance;
    public ZoneBase base;

    public static class ZoneRoutes {
        public String zoneId;
        public CommanderWaypoints[] waypoints;
        public String id;
    }

    public static class ZoneBase {
        public String id;
        public String type;
        public String zoneId;
        public double altitude;
        public WaypointData data;
    }

    public static class ZonesData {
        public String type;
        public ZoneProperties properties;
        public ZoneGeometry geometry;

        public static class ZoneProperties {
            public ZoneStyle style;
        }

        public static class ZoneStyle {
            public String color;
        }

        public static class ZoneGeometry {
            public List<double[]> coordinate2D; //coordinates: [ [lng, lat], [lng, lat], [lng, lat] ]
            public String type;
        }
    }

}
