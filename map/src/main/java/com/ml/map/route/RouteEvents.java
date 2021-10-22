package com.ml.map.route;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class RouteEvents {
    public static class ProcessRoute {
        public List<Route> routeList;
        public int shortestRouteIndex;
        public boolean update;
        public int routeAttempt;

        public ProcessRoute(List<Route> routeList, int shortestRouteIndex, boolean update, int routeAttempt) {
            this.routeList = routeList;
            this.shortestRouteIndex = shortestRouteIndex;
            this.update = update;
            this.routeAttempt = routeAttempt;
        }
    }

    public static class Routing {
        public LatLng origin;
        public LatLng destination;
        public boolean update;

        public Routing(boolean update, LatLng origin, LatLng destination) {
            this.update = update;
            this.origin = origin;
            this.destination = destination;
        }
    }

    public static class FollowRoute {
    }

    public static class SimulateRouteStartStop {
        public boolean start;

        public SimulateRouteStartStop(boolean b) {
            this.start = b;
        }
    }

    public static class RouteDescription {
        public String instruction;

        public RouteDescription(String instruction) {
            this.instruction = instruction;
        }
    }
}
