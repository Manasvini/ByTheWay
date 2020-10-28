package com.epl.bytheway;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import static androidx.core.content.ContextCompat.startActivity;
import static java.lang.Thread.sleep;

class RoutePlaceSelectionListener implements PlaceSelectionListener {
    RoutePlaceSelectionListener(GoogleMap gmap){
        mMap = gmap;
    }

    @Override
    public void onPlaceSelected(@NonNull Place place) {
        Log.w(TAG, "Place: " + place.getName() + ", " + place.getLatLng());
        if(mMarker != null){
            mMarker.remove();
            mMarker = null;
        }
        if(place == null){
            return;
        }
        mLatLng = place.getLatLng();
        mId = place.getId();
        if(mLatLng == null){
            return;
        }
        Marker m = mMap.addMarker(new MarkerOptions().position(mLatLng).title(place.getName()));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mLatLng));
        mMarker = m;
    }
    @Override
    public void onError(@NotNull Status status) {
        // TODO: Handle the error.
        Log.w(TAG, "An error occurred: " + status);
    }

    public String getPlaceId(){
        return mId;
    }

    Marker mMarker;
    GoogleMap mMap;
    LatLng mLatLng;
    String mId;
    String TAG = "RoutePlaceSelectionListener";
}

class GetRouteListener implements  View.OnClickListener{
    RoutePlaceSelectionListener mSrc, mDst;
    String mKey;
    String TAG = "GetRouteListener";
    RouteFetcher mRouteFetcher;
    public GetRouteListener(RoutePlaceSelectionListener src, RoutePlaceSelectionListener dst, String key, RouteFetcher routeFetcher){
        mSrc = src;
        mDst = dst;
        mKey = key;
        mRouteFetcher = routeFetcher;
    }
    @Override
    public  void onClick(View v) {
        String origin = mSrc.getPlaceId();
        String destination = mDst.getPlaceId();
        if (origin == null || destination == null) {
            return;

        }
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=place_id:" + origin + "&destination=place_id:" + destination + "&key=" + mKey;
        Log.i(TAG, url);
        mRouteFetcher.getRoute(url);
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    final String TAG = "MapActivity";
    private GoogleMap mMap;
    private RouteFetcher mRf;
    RoutePlaceSelectionListener mSrcListener;
    RoutePlaceSelectionListener mDstListener;

    void initAutocomplete(AutocompleteSupportFragment autocompleteSupportFragment, RoutePlaceSelectionListener listener){
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ID));
        autocompleteSupportFragment.setOnPlaceSelectedListener(listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getPermissions();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Log.i(TAG, getResources().getString(R.string.google_maps_key));
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));


    }

    void getPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_REQUEST);
        }
        else {
            Log.i(TAG, "Got network perm");
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }

    }
    class UploadListener implements  View.OnClickListener{
        RouteFetcher mRf;
        String TAG = "UploadListener";
        UploadListener(RouteFetcher rf){

            mRf = rf;
        }

        private class LatLong{
            private double latitude;
            private double longitude;

            public double getLongitude() {
                return longitude;
            }

            public double getLatitude() {
                return latitude;
            }

            public void setLatitude(double latitude) {
                this.latitude = latitude;
            }

            public void setLongitude(double longitude) {
                this.longitude = longitude;
            }
        }

        private class Route{
            private List<LatLong> points;

            public List<LatLong> getPoints() {
                return points;
            }

            public void setPoints(List<LatLong> points) {
                this.points = points;
            }
        }


        @Override
        public void  onClick(View v){
            List<LatLng> latlngs = mRf.getLatLngs();
            Log.i(TAG, "got " + latlngs.size() + " points");

            List<LatLong> points = new ArrayList<>();
            for(LatLng l : latlngs){
                LatLong point = new LatLong();
                point.setLatitude(l.latitude);
                point.setLongitude(l.longitude);
                points.add(point);
            }
            Route r = new Route();
            r.setPoints(points);
            ObjectMapper mapper = new ObjectMapper();
            try {
                String routeStr = mapper.writeValueAsString(r);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, routeStr);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragmentSource = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_source);

        mSrcListener = new RoutePlaceSelectionListener(mMap);
        mDstListener = new RoutePlaceSelectionListener(mMap);
        initAutocomplete(autocompleteFragmentSource, mSrcListener);

        AutocompleteSupportFragment autocompleteFragmentDest = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_dest);
        initAutocomplete(autocompleteFragmentDest, mDstListener);

        Button getRouteButton = (Button) findViewById(R.id.getRouteButton);
        mRf = new RouteFetcher(mMap);
        getRouteButton.setOnClickListener(new GetRouteListener(mSrcListener, mDstListener,  getResources().getString(R.string.google_maps_key), mRf));

        Button uploadButton = (Button) findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new UploadListener(mRf));
    }

    private static final int INTERNET_PERMISSION_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

}
