package com.skycatch.android.commanderproto;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.drone.DroneStateApi;
import com.o3dr.android.client.apis.mission.MissionApi;
import com.o3dr.android.client.apis.drone.GuidedApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.skycatch.android.commanderproto.data.CommanderMission;
import com.skycatch.android.commanderproto.data.CommanderZone;
import com.skycatch.android.commanderproto.data.ObjectConverter;
import com.skycatch.android.commanderproto.fragments.MapboxFragment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements TowerListener, DroneListener{

    final static int MODE_STABILIZE = 0;
    final static int MODE_GUIDED = 1;
    final static int MODE_AUTO = 2;
    final static int MODE_LAND = 3;
    final static String GROUND_COLLITION_IMMINENT = "com.o3dr.android.client.Drone.ACTION_GROUND_COLLISION_IMMINENT";

    private Button importMission;
    private Button connect;
    private Button uploadMission;
    private Button arm;
    private Button takeOff;
    private Spinner modesDropdown;

    private CharSequence[] chars;
    private String path = "";
    private CommanderMission commanderMission;

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


        importMission.setOnClickListener(new View.OnClickListener() {
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

        modesDropdown.setOnItemSelectedListener(modesSelector);

        uploadMission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drone.isConnected()) {
                    Mission mission = new Mission();

                    for (MissionItem item : commanderMission.missionItems) {
                        mission.addMissionItem(item);
                    }
                    MissionApi.setMission(drone, mission, true);

                } else {
                    Toast.makeText(getApplicationContext(), R.string.connect_to_vehicle, Toast.LENGTH_LONG).show();
                }
            }
        });

        arm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drone.arm(true);
            }
        });

        takeOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drone.isConnected()) {
                    if (((Button) v).getText().equals("Take off")) {
                        GuidedApi.takeoff(drone, commanderMission.altAboveGrnd);
                        takeOff.setText("Land");
                        Toast.makeText(getApplicationContext(), "Taking off", Toast.LENGTH_SHORT).show();
                    } else {
                        DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_LAND);
                        takeOff.setText("Take off");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.connect_to_vehicle, Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    AdapterView.OnItemSelectedListener modesSelector = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case MODE_STABILIZE:
                    DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_STABILIZE);
                    Toast.makeText(getApplicationContext(), "Mode STABILIZE", Toast.LENGTH_LONG).show();
                    break;
                case MODE_GUIDED:
                    DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_GUIDED);
                    GuidedApi.setGuidedAltitude(drone, commanderMission.altAboveGrnd);
                    Toast.makeText(getApplicationContext(), "Mode GUIDED", Toast.LENGTH_LONG).show();
                    break;
                case MODE_AUTO:
                    DroneStateApi.setVehicleMode(drone, VehicleMode.COPTER_AUTO);
                    Toast.makeText(getApplicationContext(), "Mode AUTO", Toast.LENGTH_LONG).show();
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    DialogInterface.OnClickListener dialoglistener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String file = getCommanderJsonFile(path + "/" + chars[which]);
            commanderMission = ObjectConverter.createObjectConverter(file);

            if (commanderMission != null) {
                mapboxFragment.drawMission(commanderMission);
            }

        }
    };

    public void setupUI() {
        importMission = (Button) findViewById(R.id.upload_btn);
        connect = (Button) findViewById(R.id.connect_btn);
        uploadMission = (Button) findViewById(R.id.upload_mission);
        arm = (Button) findViewById(R.id.arm_btn);
        takeOff = (Button) findViewById(R.id.take_off);

        modesDropdown = (Spinner) findViewById(R.id.mode_dropdown);
        ArrayAdapter<CharSequence> modesAdapter = ArrayAdapter.createFromResource(this, R.array.modes_array, R.layout.waypoint_spinner_item);
        modesAdapter.setDropDownViewResource(R.layout.waypoint_spinner_item);
        modesDropdown.setAdapter(modesAdapter);
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
                mapboxFragment.setUAVmarker(((Gps)drone.getAttribute(AttributeType.GPS)).getPosition());
                break;
            case AttributeEvent.STATE_DISCONNECTED:
                updateButton(false);
                break;
            case AttributeEvent.GPS_POSITION:
                Gps dronePosition = drone.getAttribute(AttributeType.GPS);
                LatLong currentPosition = dronePosition.getPosition();
                mapboxFragment.moveUAVmarker(currentPosition);
                break;
            case GROUND_COLLITION_IMMINENT:
                boolean groundCollition = bundle.getBoolean("extra_is_ground_collision_imminent");
                break;
            case AttributeEvent.AUTOPILOT_MESSAGE:
                String message = bundle.getString(AttributeEventExtra.EXTRA_AUTOPILOT_MESSAGE);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String s) {

    }
}
