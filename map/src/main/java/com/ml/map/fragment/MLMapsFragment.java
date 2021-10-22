package com.ml.map.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ml.map.EventBusHook;
import com.ml.map.GoblobLocationManager;
import com.ml.map.R;
import com.ml.map.ServiceEvents;

public abstract class MLMapsFragment extends MLFragment {

    private GoblobLocationManager session = GoblobLocationManager.getInstance();
    private GoogleMap googleMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void setSatelliteCount(int satelliteCount) {

    }

    @Override
    protected void onWaitingForLocation(boolean waiting) {

    }

    @Override
    protected void setLoggingStatus(boolean loggingStarted) {

    }

    private Marker marker;
    private OnMapReadyCallback callback = new OnMapReadyCallback() {


        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap gm) {
            googleMap = gm;
            if(session.getCurrentLocationInfo() != null) {
                LatLng sydney = new LatLng(session.getCurrentLocationInfo().getLatitude(), session.getCurrentLocationInfo().getLongitude());
                marker = googleMap.addMarker(new MarkerOptions().position(sydney).title("Me"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16));
            }
        }
    };

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationUpdate){
        LatLng sydney = new LatLng(session.getCurrentLocationInfo().getLatitude(), session.getCurrentLocationInfo().getLongitude());
        if(marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(sydney).title("Me"));
        } else {
            marker.setPosition(sydney);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16));
    }

    @Override
    protected void displayLocationInfo(Location location) {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}