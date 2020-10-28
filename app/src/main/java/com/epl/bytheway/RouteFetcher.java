package com.epl.bytheway;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class RouteFetcher  {
    public RouteFetcher(GoogleMap map){
        Cache cache = new NoCache();
       //         new DiskBasedCache(, 1024 * 1024); // 1MB cap
        Network network = new BasicNetwork(new HurlStack());

        mQueue = new RequestQueue(cache, network);
        mQueue.start();
        mMap = map;

    }
    public void getRoute(String url){

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //textView.setText("Response is: "+ response.substring(0,500));
                        //Log.i(TAG, response);
                        try {
                            JSONObject respObject = new JSONObject(response);
                            mDirectionsStr = response;
                            mPolyLine = respObject.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                            Log.i(TAG, mPolyLine);
                            if (mPolyLine != null) {
                                List<LatLng> decodedPolyline = PolyUtil.decode(mPolyLine);
                                mLatLngs = decodedPolyline;
                                Log.i(TAG, "num points = " + decodedPolyline.size());
                                if(mLine != null){
                                    mLine.remove();
                                }
                                mLine = mMap.addPolyline(new PolylineOptions()
                                        .addAll(decodedPolyline)
                                        .width(10)
                                        .color(Color.RED));

                            }
                            else{
                                Log.i(TAG, "polyline is null");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //textView.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        mQueue.add(stringRequest);

    }
    public String getDirections(){
        return mDirectionsStr;
    }
    public List<LatLng> getLatLngs(){
        return mLatLngs;
    }
    RequestQueue mQueue;
    String TAG = "RouteFetcher";
    String mPolyLine;
    Polyline mLine;
    GoogleMap mMap;
    String mDirectionsStr;
    List<LatLng> mLatLngs;
}
