package com.skycatch.android.commanderproto.data;

import android.graphics.Color;

import com.cocoahero.android.geojson.Feature;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marthaelena on 9/25/15.
 */
public class CommanderZone {

    public String id;
    public String type;
    public ZoneRoutes[] routes;
    public Feature data;
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
        public Feature data;
    }

    public int getColorProperty(){
        int color;
        try {
            color = Color.parseColor(data.getProperties().getJSONObject("style").getString("color"));
        } catch (JSONException e) {
            e.printStackTrace();
            //set default color to black if something goes wrong
            color = Color.BLACK;
        }
        return color;
    }

}
