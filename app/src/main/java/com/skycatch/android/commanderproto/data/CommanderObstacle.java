package com.skycatch.android.commanderproto.data;

import android.graphics.Color;

import com.cocoahero.android.geojson.Feature;

import org.json.JSONException;

/**
 * Created by marthaelena on 10/6/15.
 */
public class CommanderObstacle {

    public String id;
    public String type;
    public Feature data;
    public int height;
    public int buffer;

    public int getColorProperty() {
        int color;
        try {
            color = Color.parseColor(data.getProperties().getJSONObject("style").getString("color"));
        } catch (JSONException e) {
            e.printStackTrace();
            //set default color in case something goes wrong
            color = Color.rgb(228, 169, 59);
        }

        return color;
    }

}
