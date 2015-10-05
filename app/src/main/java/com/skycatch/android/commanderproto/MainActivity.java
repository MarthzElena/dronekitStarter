package com.skycatch.android.commanderproto;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.DroneApiListener;
import com.o3dr.android.client.apis.drone.DroneStateApi;
import com.o3dr.android.client.apis.mission.MissionApi;
import com.o3dr.android.client.apis.drone.GuidedApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.complex.StructureScanner;
import com.o3dr.services.android.lib.drone.mission.item.complex.Survey;
import com.o3dr.services.android.lib.drone.mission.item.spatial.BaseSpatialItem;
import com.o3dr.services.android.lib.drone.mission.item.spatial.SplineWaypoint;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.action.Action;
import com.o3dr.services.android.lib.util.ParcelableUtils;
import com.skycatch.android.commanderproto.data.CommanderMission;
import com.skycatch.android.commanderproto.data.CommanderWaypoints;
import com.skycatch.android.commanderproto.data.CommanderZone;
import com.skycatch.android.commanderproto.fragments.MapboxFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends FragmentActivity implements TowerListener, DroneListener{

    private Button upload;
    private Button connect;
    private Button modeGuided;
    private Button modeAuto;
    private Spinner waypointSpinner;

    private CharSequence[] chars;
    private String path = "";
    List<MissionItem> missionItems = new ArrayList<>();
    private CommanderMission commanderMission;
    private List<CommanderZone> zones;
    List<CommanderZone.ZoneRoutes> routes;

    private final Handler handler = new Handler();
    private ControlTower controlTower;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN; //define drone type (copter, plane, rover)
    private Mission mission;

    MapboxFragment mapboxFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();

        //Initialize service manager
        controlTower = new ControlTower(getApplicationContext());
        drone = new Drone(getApplicationContext());

        //Create app folder
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getApplicationContext(), "No SDCard", Toast.LENGTH_LONG).show();
        } else {
            File directory = new File(Environment.getExternalStorageDirectory(), "Commander");
            path = directory.getPath();
            if (!directory.exists()) {
                boolean create = directory.mkdirs();
                Toast.makeText(getApplicationContext(), String.valueOf(create), Toast.LENGTH_LONG).show();
            }
            File[] filesArray = directory.listFiles();
            List<String> filesList = new ArrayList<>();
            for (int i=0; i < filesArray.length; i++) {
                String current = filesArray[i].getName();
                String[] fileName = current.split("\\.");
                if (fileName[fileName.length-1].equals("cmdr")) {
                    filesList.add(current);
                }
            }
            chars = new String[filesList.size()];
            filesList.toArray(chars);
        }

        mapboxFragment = new MapboxFragment();
        getSupportFragmentManager().popBackStack();
        FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
        fm.add(R.id.fragment_container, mapboxFragment).commit();


        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Select file to upload:");
                if (chars != null) {
                    dialog.setItems(chars, dialoglistener);
                }
                AlertDialog box = dialog.create();
                box.show();
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectOnTap();
            }
        });

        waypointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Waypoint selected = (Waypoint) missionItems.get(position);
                LatLong latLong = new LatLong(selected.getCoordinate().getLatitude(), selected.getCoordinate().getLongitude());
                GuidedApi.sendGuidedPoint(drone, latLong, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        modeGuided.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_GUIDED);
                GuidedApi.setGuidedAltitude(drone, 40.0);
            }
        });

        modeAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_AUTO);
            }
        });

    }

    DialogInterface.OnClickListener dialoglistener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            missionItems = new ArrayList<>();
            String file = getCommanderJsonFile(path + "/" + chars[which]);
            boolean data = getData(file);
            if (data) {
                mission = new Mission();
                List<CharSequence> charWaypoints = new ArrayList<>();

                for (int i = 0; i < missionItems.size(); i++) {
                    mission.addMissionItem(missionItems.get(i));
                    charWaypoints.add("waypoint "+i);
                }

                setWaypointUI(charWaypoints);

                //move map to zone
                if (!zones.isEmpty()) {
                    LatLng baseCenter = new LatLng(zones.get(0).base.data.geometry.getLat(), zones.get(0).base.data.geometry.getLng());

//                    mapboxFragment.moveMaptoZone(baseCenter);
                }
            }
        }
    };

    public void setWaypointUI(List<CharSequence> charWaypoints){
        ArrayAdapter<CharSequence> waypointAdapter = new ArrayAdapter<CharSequence>(
                getApplicationContext(),
                R.layout.waypoint_spinner_item, charWaypoints);
        waypointAdapter.setDropDownViewResource(R.layout.waypoint_spinner_item);
        waypointSpinner.setAdapter(waypointAdapter);
    }

    public void setupUI() {
        upload = (Button) findViewById(R.id.upload_btn);
        connect = (Button) findViewById(R.id.connect_btn);
        modeGuided = (Button) findViewById(R.id.guided_mode);
        modeAuto = (Button) findViewById(R.id.auto_mode);
        waypointSpinner = (Spinner) findViewById(R.id.waypoint_dropdown);
    }

    public String getCommanderJsonFile(String filename) {
        String json = "";
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filename));
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    public boolean getData(String json) {
        try {
            JSONObject jsonObjectMission = (new JSONObject(json)).getJSONObject("mission");
            Gson gson = new Gson();
            commanderMission = gson.fromJson(String.valueOf(jsonObjectMission), CommanderMission.class);
            zones = Arrays.asList(commanderMission.zones);
            JSONArray coordinates = jsonObjectMission.getJSONArray("zones").getJSONObject(0).getJSONObject("data").getJSONObject("geometry").getJSONArray("coordinates");
            for (int i = 0; i < coordinates.length(); i++) {

            }
            routes = new ArrayList<>();
            for (CommanderZone commanderZone : zones) {
                routes = Arrays.asList(commanderZone.routes);
            }

            for (CommanderZone.ZoneRoutes zoneRoutes : routes) {
                List<LatLong> points = new ArrayList<>();
                for (int i=0; i < zoneRoutes.waypoints.length; i++) {
                    CommanderWaypoints currentWaypoint = zoneRoutes.waypoints[i];
                    points.add(new LatLong(currentWaypoint.data.geometry.getLat(), currentWaypoint.data.geometry.getLng()));
                    Waypoint waypoint = new Waypoint();
                    waypoint.setCoordinate(new LatLongAlt(currentWaypoint.data.geometry.getLat(), currentWaypoint.data.geometry.getLng(), currentWaypoint.altitude));
                    missionItems.add(waypoint);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        controlTower.connect(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (drone.isConnected()) {
            drone.disconnect();
            updateButton(true);
        }
        controlTower.unregisterDrone(drone);
        controlTower.disconnect();
    }

    /********** Tower listener ***********/
    @Override
    public void onTowerConnected() {
        controlTower.registerDrone(drone, handler);
        drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        controlTower.unregisterDrone(drone);
        drone.unregisterDroneListener(this);
    }

    public void updateButton(boolean connectedText) {
        CharSequence text = connectedText ? getText(R.string.disconnect) : getText(R.string.connect);
        connect.setText(text);
    }

    public void connectOnTap() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {

            Bundle bundleParams = new Bundle();
            bundleParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14551);
            ConnectionParameter connectionParameter = new ConnectionParameter(ConnectionType.TYPE_UDP, bundleParams, null);

            this.drone.connect(connectionParameter);

            if (mission != null){
                MissionApi.setMission(drone, new Mission(), true);
            }
        }
    }

    /********** Drone listener ***********/
    @Override
    public void onDroneConnectionFailed(ConnectionResult connectionResult) {
        String error = connectionResult.getErrorMessage();
        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDroneEvent(String s, Bundle bundle) {
        switch (s) {
            case AttributeEvent.STATE_CONNECTED:
                updateButton(true);
                break;
            case AttributeEvent.STATE_DISCONNECTED:
                updateButton(false);
                break;
            case AttributeEvent.STATE_ARMING:
                Toast.makeText(getApplicationContext(), "ARMING!!", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String s) {

    }
}
