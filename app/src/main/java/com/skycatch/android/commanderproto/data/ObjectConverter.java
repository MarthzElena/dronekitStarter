package com.skycatch.android.commanderproto.data;

import com.cocoahero.android.geojson.Feature;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marthaelena on 10/5/15.
 *
 * Module to convert String read from file into CommanderMission object
 *
 */
public class ObjectConverter {

    private CommanderMission commanderMission;
    private JSONArray coordinatesArray;


    /**
    * This methods create the converted object as it handles JSONObject
     * returns the converted object if everything went smooth and null if exception occurred
    */
    public static CommanderMission createObjectConverter(String missionJSON) {
        CommanderMission commanderMission = null;

        try {
            JSONObject jsonObject = new JSONObject(missionJSON);
            JSONObject jsonMission = jsonObject.getJSONObject("mission");
            JSONObject jsonPlanMenu = jsonObject.getJSONObject("planMenu");

            Gson gson = new Gson();
            commanderMission = gson.fromJson(String.valueOf(jsonMission), CommanderMission.class);

            JSONArray JSONzonesData = jsonMission.getJSONArray("zones");
            for (int i = 0; i < JSONzonesData.length(); i++) {
                JSONObject currentZone = JSONzonesData.getJSONObject(i);
                commanderMission.zones[i].data = new Feature(currentZone.getJSONObject("data"));

                JSONArray jsonRoutes = currentZone.getJSONArray("routes");
                for (int j = 0; j < jsonRoutes.length(); j++) {
                    JSONObject currentRoute = jsonRoutes.getJSONObject(j);

                    JSONArray jsonWaypoints = currentRoute.getJSONArray("waypoints");
                    for (int k = 0; k < jsonWaypoints.length(); k++) {
                        JSONObject currentWaypoint = jsonWaypoints.getJSONObject(k);
                        commanderMission.zones[i].routes[j].waypoints[k].data = new Feature(currentWaypoint.getJSONObject("data"));
                    }
                }

                //set the base of the zone
                commanderMission.zones[i].base.data = new Feature(currentZone.getJSONObject("base").getJSONObject("data"));
            }

            // Check for obstacles in the mission
            JSONArray jsonObstacles = jsonMission.getJSONArray("obstacles");
            if (jsonObstacles.length() != 0) {
                for (int i = 0; i < jsonObstacles.length(); i++) {
                    JSONObject currentObstacle = jsonObstacles.getJSONObject(i);
                    commanderMission.obstacles[i].data = new Feature(currentObstacle.getJSONObject("data"));
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        return commanderMission;
    }

    public ObjectConverter(String mission, String planMenu) {

    }

    public void setPlanMenu(String planMenu) {

    }

}