package com.skycatch.android.commanderproto.data;

import org.json.JSONArray;

/**
 * Created by marthaelena on 9/28/15.
 */
public class CommanderMission {

    public String id;
    public String name;
    public CommanderZone[] zones;
    public CommanderObstacle[] obstacles;
    public CommanderMissionLocation location;
    public String resolution;
    public double altAboveGrnd;
    //TODO: public UAVProfile uavProfile;

    public static class CommanderMissionLocation {
        public double lat;
        public double lng;
    }

}
