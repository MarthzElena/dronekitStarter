package com.skycatch.android.commanderproto.data;

import com.o3dr.services.android.lib.drone.mission.item.MissionItem;

import org.json.JSONArray;

import java.util.List;

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
    public List<MissionItem> missionItems;
    //TODO: public UAVProfile uavProfile;

    public static class CommanderMissionLocation {
        public double lat;
        public double lng;
    }

}
