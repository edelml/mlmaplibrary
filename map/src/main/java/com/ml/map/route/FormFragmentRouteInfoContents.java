package com.ml.map.route;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ml.map.CommandEvents;
import com.ml.map.EventBusHook;
import com.ml.map.GoblobLocationManager;

import de.greenrobot.event.EventBus;

/**
 * Created by nmlemus on 21/12/17.
 */

public class FormFragmentRouteInfoContents extends Fragment {
    private TextView distanceValue;
    private TextView durationValue;
    private long distance = -1;
    private long duration = -1;
    private View travelBicycling;
    private View travelWalking;
    private View travelDriving;
    private AbstractRouting.TravelMode travelMode = null;
    private View followRoute;
    private View unfollowRoute;
    private View simulateRoute;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = null;// = inflater.inflate(R.layout.routeinfowindowlayout, container, false);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
    }

    private void registerEventBus() {
        EventBus.getDefault().registerSticky(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*distanceValue = view.findViewById(R.id.distanceValue);
        durationValue = view.findViewById(R.id.durationValue);
        distanceValue.setText(distanceKm(distance));
        durationValue.setText(duration(duration));

        travelBicycling = view.findViewById(R.id.travelBicycling);
        travelWalking = view.findViewById(R.id.travelWalking);
        travelDriving = view.findViewById(R.id.travelDriving);

        followRoute = view.findViewById(R.id.followRoute);

        unfollowRoute = view.findViewById(R.id.unfollowRoute);

        simulateRoute = view.findViewById(R.id.simulateRoute);

        final View shareRoute = view.findViewById(R.id.shareRoute);

        shareRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mapsActivity2017.shareRoute();
            }
        });

        followRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new RouteEvents.FollowRoute());
            }
        });

        unfollowRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
            }
        });

        simulateRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoblobLocationManager.getInstance().setFollowRoute(true);
                GoblobLocationManager.getInstance().simulateRoute(true);
                followRoute.setVisibility(View.GONE);
                unfollowRoute.setVisibility(View.VISIBLE);
                simulateRoute.setVisibility(View.GONE);
            }
        });

        if (GoblobLocationManager.getInstance().isFollowRoute()) {
            followRoute.setVisibility(View.GONE);
            unfollowRoute.setVisibility(View.VISIBLE);
            simulateRoute.setVisibility(View.GONE);
        } else {
            followRoute.setVisibility(View.VISIBLE);
            unfollowRoute.setVisibility(View.GONE);
            simulateRoute.setVisibility(View.VISIBLE);
        }

        if (travelMode != null) {
            if (travelMode == AbstractRouting.TravelMode.DRIVING) {
                travelBicycling.setVisibility(View.GONE);
                travelWalking.setVisibility(View.GONE);
                travelDriving.setVisibility(View.VISIBLE);
            } else if (travelMode == AbstractRouting.TravelMode.WALKING) {
                travelBicycling.setVisibility(View.GONE);
                travelWalking.setVisibility(View.VISIBLE);
                travelDriving.setVisibility(View.GONE);
            } else if (travelMode == AbstractRouting.TravelMode.BIKING) {
                travelBicycling.setVisibility(View.VISIBLE);
                travelWalking.setVisibility(View.GONE);
                travelDriving.setVisibility(View.GONE);
            }
        }*/
    }

    @EventBusHook
    public void onEventMainThread(CommandEvents.RequestStartStop startStop){
        if(startStop.start){
            GoblobLocationManager.getInstance().setFollowRoute(true);
            GoblobLocationManager.getInstance().simulateRoute(false);
            GoblobLocationManager.getInstance().saveCurrentRoute();
            followRoute.setVisibility(View.GONE);
            unfollowRoute.setVisibility(View.VISIBLE);
            simulateRoute.setVisibility(View.GONE);
        } else {
            GoblobLocationManager.getInstance().setFollowRoute(false);
            GoblobLocationManager.getInstance().simulateRoute(false);
            GoblobLocationManager.getInstance().deleteCurrentRoute();
            followRoute.setVisibility(View.VISIBLE);
            unfollowRoute.setVisibility(View.GONE);
            simulateRoute.setVisibility(View.VISIBLE);
        }
    }

    @EventBusHook
    public void onEventMainThread(RouteEvents.SimulateRouteStartStop simulateRouteStartStop) {
        if (!simulateRouteStartStop.start) {
            GoblobLocationManager.getInstance().setFollowRoute(false);
            GoblobLocationManager.getInstance().simulateRoute(false);
            followRoute.setVisibility(View.VISIBLE);
            unfollowRoute.setVisibility(View.GONE);
            simulateRoute.setVisibility(View.VISIBLE);
        }
    }

    public void setValues(long distance, long duration, AbstractRouting.TravelMode travelMode) {
        this.distance = distance;
        this.duration = duration;
        this.travelMode = travelMode;

        if (distanceValue != null) {
            distanceValue.setText(distanceKm(distance));
            durationValue.setText(duration(duration));

            if (travelMode != null) {
                if (travelMode == AbstractRouting.TravelMode.DRIVING) {
                    travelBicycling.setVisibility(View.GONE);
                    travelWalking.setVisibility(View.GONE);
                    travelDriving.setVisibility(View.VISIBLE);
                } else if (travelMode == AbstractRouting.TravelMode.WALKING) {
                    travelBicycling.setVisibility(View.GONE);
                    travelWalking.setVisibility(View.VISIBLE);
                    travelDriving.setVisibility(View.GONE);
                } else if (travelMode == AbstractRouting.TravelMode.BIKING) {
                    travelBicycling.setVisibility(View.VISIBLE);
                    travelWalking.setVisibility(View.GONE);
                    travelDriving.setVisibility(View.GONE);
                }
            }
        }
    }

    private String duration(long duration) {
        if (duration < 60) {
            return duration + " seconds";
        } else if (duration < 3600) {
            return ((int) duration / 60) + " minutes";
        } else {
            return ((int) duration / 3600) + " hours " + ((int) duration % 3600) / 60 + " minutes";
        }
    }

    public static String distanceKm(double distance) {
        if (distance < 1000) {
            return (Math.round(distance * 100) / 100) + " m";
        }

        return (Math.round((distance / 1000) * 100) / 100) + " km";
    }
}