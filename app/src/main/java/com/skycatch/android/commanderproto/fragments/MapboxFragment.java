package com.skycatch.android.commanderproto.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.skycatch.android.commanderproto.R;
import com.skycatch.android.commanderproto.data.CommanderMission;
import com.skycatch.android.commanderproto.data.CommanderObstacle;
import com.skycatch.android.commanderproto.data.CommanderWaypoints;
import com.skycatch.android.commanderproto.data.CommanderZone;

import java.util.List;

/**
 * Created by marthaelena on 10/2/15.
 */
public class MapboxFragment  extends Fragment{

    private MapView mapView;
    private Marker uav;

    public MapboxFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapbox, container, false);

        mapView = (MapView) view.findViewById(R.id.mapbox_view);
        mapView.setZoom(1);

        return view;
    }

    public void moveMaptoZone(LatLng center){
        mapView.setZoom(17);
        mapView.setCenter(center);
    }

    public void drawMission(CommanderMission mission) {
        moveMaptoZone(new LatLng(mission.location.lat, mission.location.lng));

        //go through zones
        for (int i = 0; i < mission.zones.length; i++) {
            CommanderZone currentZone = mission.zones[i];

            PathOverlay polygonFill = new PathOverlay();
            PathOverlay polygonStroke = new PathOverlay();

            List<Ring> rings = ((Polygon) currentZone.data.getGeometry()).getRings();
            for (int j = 0; j < rings.size(); j++) {
                if (j == 0) { //outer ring
                    List<Position> polygonPositions = rings.get(j).getPositions();
                    for (int k = 0; k < polygonPositions.size(); k++) {
                        Position currentPosition = polygonPositions.get(k);
                        polygonFill.addPoint(new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude()));
                        polygonStroke.addPoint(new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude()));
                    }
                } else { //obstacles

                }
            }
            Paint fill = new Paint();
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(Color.argb(90, 0, 0, 0));
            polygonFill.setPaint(fill);

            Paint stroke = new Paint();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(currentZone.getColorProperty());
            stroke.setStrokeWidth(7);
            polygonStroke.setPaint(stroke);

            mapView.getOverlays().add(polygonFill);
            mapView.getOverlays().add(polygonStroke);

            //get routes
            CommanderZone.ZoneRoutes[] routes = currentZone.routes;

            for (int j = 0; j < routes.length; j++) {
                CommanderWaypoints[] zoneWaypoints = routes[j].waypoints;
                PathOverlay routeLine = new PathOverlay();

                for (int k = 0; k < zoneWaypoints.length; k++) {
                    CommanderWaypoints currentWaypoint = zoneWaypoints[k];

                    //draw markers
                    Point currentPoint = (Point) currentWaypoint.data.getGeometry();
                    Marker marker = new Marker("", "", new LatLng(currentPoint.getPosition().getLatitude(), currentPoint.getPosition().getLongitude()));
                    Drawable markerDrawable;
                    if (k == 0 || k == zoneWaypoints.length-1) {
                        markerDrawable = customizedMarker(R.drawable.green_marker, currentWaypoint.getMarker());
                        markerDrawable = markerDrawable == null ? getResources().getDrawable(R.drawable.green_marker) : markerDrawable;
                        marker.setMarker(markerDrawable);
                        PointF anchor = k == 0 ? new PointF(0.5f, 0) : new PointF(0.5f, 1);
                        marker.setAnchor(anchor);

                    } else {
                        markerDrawable = customizedMarker(R.drawable.gray_circle, currentWaypoint.getMarker());
                        markerDrawable = markerDrawable == null ? getResources().getDrawable(R.drawable.gray_circle) : markerDrawable;
                        marker.setMarker(markerDrawable);
                        marker.setAnchor(new PointF(0.5f, 0.5f));

                    }
                    mapView.addMarker(marker);
                    routeLine.addPoint(new LatLng(currentPoint.getPosition().getLatitude(), currentPoint.getPosition().getLongitude()));
                }

                //draw routes
                int color = j%2 == 0 ? Color.WHITE : Color.GRAY;
                Paint route = new Paint();
                route.setStyle(Paint.Style.STROKE);
                route.setColor(color);
                route.setStrokeWidth(3);
                routeLine.setPaint(route);

                mapView.getOverlays().add(routeLine);
            }

            //draw base
            Point basePoint = (Point) currentZone.base.data.getGeometry();
            Marker base = new Marker("", "", new LatLng(basePoint.getPosition().getLatitude(), basePoint.getPosition().getLongitude()));
            base.setMarker(getResources().getDrawable(R.drawable.baseicon));
            mapView.addMarker(base);
        }

        //draw obstacle
        for (int i = 0; i < mission.obstacles.length; i++) {
            CommanderObstacle currentObstacle = mission.obstacles[i];

            PathOverlay obstacleStroke = new PathOverlay();
            PathOverlay obstacleFill = new PathOverlay();


            List<Ring> rings = ((Polygon)currentObstacle.data.getGeometry()).getRings();
            for (int j = 0; j < rings.size(); j++) {
                if (j == 0){ //outer ring
                    List<Position> positions = rings.get(j).getPositions();
                    for (int k = 0; k < positions.size(); k++) {
                        Position currentPosition = positions.get(k);

                        obstacleStroke.addPoint(new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude()));
                        obstacleFill.addPoint(new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude()));
                    }
                }
            }

            Paint paintStroke = new Paint();
            paintStroke.setColor(currentObstacle.getColorProperty());
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(7);
            obstacleStroke.setPaint(paintStroke);

            Paint paintFill = new Paint();
            paintFill.setColor(currentObstacle.getColorProperty());
            paintFill.setAlpha(90);
            paintFill.setStyle(Paint.Style.FILL);
            obstacleFill.setPaint(paintFill);

            mapView.getOverlays().add(obstacleFill);
            mapView.getOverlays().add(obstacleStroke);
        }

    }

    public Drawable customizedMarker(int markerImageId, String number) {
        Drawable markerImage = getResources().getDrawable(markerImageId);
        if (markerImage != null) {
            Bitmap canvasBitmap = Bitmap.createBitmap(markerImage.getIntrinsicWidth(), markerImage.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas imageCanvas = new Canvas(canvasBitmap);

            Paint imagePaint = new Paint();
            imagePaint.setTextAlign(Paint.Align.CENTER);
            imagePaint.setTextSize(12f);

            markerImage.draw(imageCanvas);
            imageCanvas.drawText(number, markerImage.getIntrinsicWidth() / 2, markerImage.getMinimumHeight() / 2, imagePaint);

            return new LayerDrawable(new Drawable[]{markerImage, new BitmapDrawable(canvasBitmap)});
        } else {
            return null;
        }
    }

    public void setUAVmarker(LatLong position) {
        if (position != null) {
            uav = new Marker("", "", new LatLng(position.getLatitude(), position.getLongitude()));
            uav.setMarker(getResources().getDrawable(R.drawable.uav));
            mapView.addMarker(uav);
        }
    }

    public void moveUAVmarker(LatLong newPosition) {
        if (newPosition != null) {
            mapView.removeMarker(uav);
            uav = new Marker("", "", new LatLng(newPosition.getLatitude(), newPosition.getLongitude()));
            uav.setMarker(getResources().getDrawable(R.drawable.uav));
            mapView.addMarker(uav);
        }
    }

}
