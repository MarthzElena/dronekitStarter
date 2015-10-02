package com.skycatch.android.commanderproto.data;

/**
 * Created by marthaelena on 9/25/15.
 */
public class CommanderZone {

    public String id;
    public String type;
    public ZoneRoutes[] routes;
    //public ZonesData data;
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

}
