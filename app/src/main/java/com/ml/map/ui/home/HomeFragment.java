package com.ml.map.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.dd.processbutton.iml.ActionProcessButton;
import com.ml.map.GoblobLocationManager;
import com.ml.map.R;
import com.ml.map.Strings;
import com.ml.map.databinding.FragmentHomeBinding;
import com.ml.map.fragment.MLFragment;

public class HomeFragment extends MLFragment {

    private static final String TAG = HomeFragment.class.getCanonicalName();
    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private Context context;
    private ActionProcessButton actionButton;
    private ScrollView rootView;
    private GoblobLocationManager session = GoblobLocationManager.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void init() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        if (getActivity() != null) {
            this.context = getActivity().getApplicationContext();
        }

        actionButton = (ActionProcessButton)rootView.findViewById(R.id.btnActionProcess);
        actionButton.setMode(ActionProcessButton.Mode.ENDLESS);
        actionButton.setBackgroundColor(ContextCompat.getColor(context, (R.color.accentColor)));

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (session.isStarted()){
                    session.stopLocationService();
                } else {
                    session.startLocationService();
                }
            }
        });

        if(session.isStarted() && session.hasValidLocation()){
            displayLocationInfo(session.getCurrentLocationInfo());
            setSatelliteCount(session.getVisibleSatelliteCount());
            setActionButtonStop();
        }

        return rootView;
    }

    private void showPreferencesSummary() {
        //showCurrentFileName(Strings.getFormattedFileName());

        ImageView imgGpx = (ImageView) rootView.findViewById(R.id.simpleview_imgGpx);
        ImageView imgKml = (ImageView) rootView.findViewById(R.id.simpleview_imgKml);
        ImageView imgCsv = (ImageView) rootView.findViewById(R.id.simpleview_imgCsv);
        ImageView imgNmea = (ImageView) rootView.findViewById(R.id.simpleview_imgNmea);
        ImageView imgLink = (ImageView) rootView.findViewById(R.id.simpleview_imgLink);
        ImageView imgJson = (ImageView)rootView.findViewById(R.id.simpleview_imgjson);

        //if (preferenceHelper.shouldLogToGpx()) {

            imgGpx.setVisibility(View.VISIBLE);
        //} else {
       //     imgGpx.setVisibility(View.GONE);
       // }

      //  if (preferenceHelper.shouldLogToKml()) {

            imgKml.setVisibility(View.VISIBLE);
      //  } else {
      //      imgKml.setVisibility(View.GONE);
      //  }

      //  if (preferenceHelper.shouldLogToNmea()) {
            imgNmea.setVisibility(View.VISIBLE);
      //  } else {
       //     imgNmea.setVisibility(View.GONE);
       // }

      //  if (preferenceHelper.shouldLogToCSV()) {

            imgCsv.setVisibility(View.VISIBLE);
      //  } else {
      //      imgCsv.setVisibility(View.GONE);
      //  }

       // if (preferenceHelper.shouldLogToCustomUrl()) {
            imgLink.setVisibility(View.VISIBLE);
       // } else {
       //     imgLink.setVisibility(View.GONE);
       // }

       // if(preferenceHelper.shouldLogToGeoJSON()){
            imgJson.setVisibility(View.VISIBLE);
       // }
       // else {
       //     imgJson.setVisibility(View.GONE);
       // }

    }

    public void displayLocationInfo(Location locationInfo){
        showPreferencesSummary();

        binding.simpleLatText.setText(Strings.getFormattedLatitude(locationInfo.getLatitude()));

        binding.simpleLonText.setText(Strings.getFormattedLongitude(locationInfo.getLongitude()));

        clearColor(binding.simpleviewImgAccuracy);

        if (locationInfo.hasAccuracy()) {

            TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
            float accuracy = locationInfo.getAccuracy();
            txtAccuracy.setText(Strings.getDistanceDisplay(getActivity(), accuracy, session.shouldDisplayImperialUnits(), true));

            if (accuracy > 500) {
                setColor(binding.simpleviewImgAccuracy, IconColorIndicator.Warning);
            }

            if (accuracy > 900) {
                setColor(binding.simpleviewImgAccuracy, IconColorIndicator.Bad);
            } else {
                setColor(binding.simpleviewImgAccuracy, IconColorIndicator.Good);
            }
        }

        ImageView imgAltitude = (ImageView)rootView.findViewById(R.id.simpleview_imgAltitude);
        clearColor(imgAltitude);

        if (locationInfo.hasAltitude()) {
            setColor(imgAltitude, IconColorIndicator.Good);

            binding.simpleviewTxtAltitude.setText(Strings.getDistanceDisplay(getActivity(), locationInfo.getAltitude(), session.shouldDisplayImperialUnits(), false));
        }

        ImageView imgSpeed = (ImageView)rootView.findViewById(R.id.simpleview_imgSpeed);
        clearColor(imgSpeed);

        if (locationInfo.hasSpeed()) {

            setColor(imgSpeed, IconColorIndicator.Good);

            TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
            txtSpeed.setText(Strings.getSpeedDisplay(getActivity(), locationInfo.getSpeed(), session.shouldDisplayImperialUnits()));
        }

        ImageView imgDirection = (ImageView) rootView.findViewById(R.id.simpleview_imgDirection);
        clearColor(imgDirection);

        if (locationInfo.hasBearing()) {
            setColor(imgDirection, IconColorIndicator.Good);
            imgDirection.setRotation(locationInfo.getBearing());

            TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
            txtDirection.setText(String.valueOf(Math.round(locationInfo.getBearing())) + getString(R.string.degree_symbol));
        }

        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);

        long startTime = session.getStartTimeStamp();
        long currentTime = System.currentTimeMillis();

        txtDuration.setText(Strings.getTimeDisplay(getActivity(), currentTime - startTime));

        double distanceValue = session.getTotalTravelled();

        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        txtTravelled.setText(Strings.getDistanceDisplay(getActivity(), distanceValue, session.shouldDisplayImperialUnits(), true));
        txtPoints.setText(session.getNumLegs() + " " + getString(R.string.points));

        String providerName = locationInfo.getProvider();

        ((TextView) rootView.findViewById(R.id.providername)).setText(providerName);

        if (!providerName.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            setSatelliteCount(-1);
        }
    }

    public void setSatelliteCount(int count) {
        ImageView imgSatelliteCount = (ImageView) rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        TextView txtSatelliteCount = (TextView) rootView.findViewById(R.id.simpleview_txtSatelliteCount);

        if(count > -1) {
            setColor(imgSatelliteCount, IconColorIndicator.Good);

            AlphaAnimation fadeIn = new AlphaAnimation(0.6f, 1.0f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);
            txtSatelliteCount.startAnimation(fadeIn);
            txtSatelliteCount.setText(String.valueOf(count));
        }
        else {
            clearColor(imgSatelliteCount);
            txtSatelliteCount.setText("");
        }

    }

    private enum IconColorIndicator {
        Good,
        Warning,
        Bad,
        Inactive
    }

    private void clearColor(ImageView imgView){
        setColor(imgView, IconColorIndicator.Inactive);
    }

    private void setColor(ImageView imgView, IconColorIndicator colorIndicator){
        imgView.clearColorFilter();

        if(colorIndicator == IconColorIndicator.Inactive){
            return;
        }

        int color = -1;
        switch(colorIndicator){
            case Bad:
                color = Color.parseColor("#FFEEEE");
                break;
            case Good:
                color = ContextCompat.getColor(context, R.color.accentColor);
                break;
            case Warning:
                color = Color.parseColor("#D4FFA300");
                break;
        }

        imgView.setColorFilter(color);

    }

    public void onWaitingForLocation(boolean inProgress) {

        Log.d(TAG, inProgress + "");

        if(!session.isStarted()){
            actionButton.setProgress(0);
            setActionButtonStart();
            return;
        }

        if(inProgress){
            actionButton.setProgress(1);
            setActionButtonStop();
        }
        else {
            actionButton.setProgress(0);
            setActionButtonStop();
        }
    }

    private void setActionButtonStart(){
        actionButton.setText(R.string.btn_start_logging);
        actionButton.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));
        actionButton.setAlpha(0.8f);
    }

    private void setActionButtonStop(){
        actionButton.setText(R.string.btn_stop_logging);
        actionButton.setBackgroundColor( ContextCompat.getColor(context, R.color.accentColorComplementary));
        actionButton.setAlpha(0.8f);
    }

    @Override
    protected void setLoggingStatus(boolean loggingStarted) {
        if(loggingStarted){
            showPreferencesSummary();
            clearLocationDisplay();
            setActionButtonStop();
        }
        else {
            setSatelliteCount(-1);
            clearLocationDisplay();
            setActionButtonStart();
        }
    }

    private void clearLocationDisplay() {

        TextView txtLatitude = (TextView) rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText("");

        TextView txtLongitude = (TextView) rootView.findViewById(R.id.simple_lon_text);
        txtLongitude.setText("");

        ImageView imgAccuracy = (ImageView)rootView.findViewById(R.id.simpleview_imgAccuracy);
        clearColor(imgAccuracy);

        TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
        txtAccuracy.setText("");
        txtAccuracy.setTextColor(ContextCompat.getColor(context, android.R.color.black));

        ImageView imgAltitude = (ImageView)rootView.findViewById(R.id.simpleview_imgAltitude);
        clearColor(imgAltitude);

        TextView txtAltitude = (TextView) rootView.findViewById(R.id.simpleview_txtAltitude);
        txtAltitude.setText("");

        ImageView imgDirection = (ImageView)rootView.findViewById(R.id.simpleview_imgDirection);
        clearColor(imgDirection);

        TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
        txtDirection.setText("");

        ImageView imgSpeed = (ImageView)rootView.findViewById(R.id.simpleview_imgSpeed);
        clearColor(imgSpeed);

        TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
        txtSpeed.setText("");


        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);
        txtDuration.setText("");

        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        txtPoints.setText("");
        txtTravelled.setText("");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}