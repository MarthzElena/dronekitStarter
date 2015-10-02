package com.skycatch.android.commanderproto.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.skycatch.android.commanderproto.R;

/**
 * Created by marthaelena on 10/2/15.
 */
public class MapboxFragment  extends Fragment{

    public MapboxFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapbox, container, false);



        return view;
    }

}
