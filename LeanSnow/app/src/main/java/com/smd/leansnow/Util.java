package com.smd.leansnow;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class Util {
    public static LatLng convertToLatLng(Feature feature) {
        Point symbolPoint = (Point) feature.geometry();
        return new LatLng(symbolPoint.latitude(), symbolPoint.longitude());
    }
}
