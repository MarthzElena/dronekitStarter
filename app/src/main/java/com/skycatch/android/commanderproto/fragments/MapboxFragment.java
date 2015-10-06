package com.skycatch.android.commanderproto.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.skycatch.android.commanderproto.R;
import com.skycatch.android.commanderproto.data.CommanderMission;
import com.skycatch.android.commanderproto.data.CommanderZone;

/**
 * Created by marthaelena on 10/2/15.
 */
public class MapboxFragment  extends Fragment{

    private MapView mapView;

    public MapboxFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapbox, container, false);

        mapView = (MapView) view.findViewById(R.id.mapbox_view);
        mapView.setZoom(1);


        return view;
    }

    public void moveMaptoZone(LatLng center){
        mapView.setCenter(center);
        mapView.setZoom(13);


    }

    public void drawMission(CommanderMission mission){

    }

}
