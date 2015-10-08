package com.skycatch.android.commanderproto.data;

import com.cocoahero.android.geojson.Feature;

import org.json.JSONException;

/**
 * Created by marthaelena on 9/28/15.
 */
public class CommanderWaypoints {

    public String type;
    public double altitude;
    public double elevation;
    public Feature data;
    public String id;
    public double routeIndex;

    public String getMarker(){
        String marker;
        try {
            marker = data.getProperties().getJSONObject("marker").getString("icon");
        } catch(JSONException e) {
            e.printStackTrace();
            //set marker to empty in case something goes wrong
            marker = "";
        }
        return marker;
    }

}
