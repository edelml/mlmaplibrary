package com.google.maps.android;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by edel on 28/12/17.
 */

public class RoutePoint {

  private LatLng nextPoint;
  private boolean increment;
  private int pointPos;
  private LatLng latLng;
  private int segmentPos;
  private boolean a100Meters;
  private boolean a500Meters;

  public RoutePoint(LatLng latLng, int pointPos, boolean increment, LatLng nextPoint) {
    this.latLng = latLng;
    this.pointPos = pointPos;
    this.increment = increment;
    this.nextPoint = nextPoint;
  }

  public void setNextPoint(LatLng nextPoint) {
    this.nextPoint = nextPoint;
  }

  public LatLng getNextPoint() {
    return nextPoint;
  }

  public RoutePoint(JSONObject jsonObject) {
    try {
      if (jsonObject.has("increment")) {
        increment = jsonObject.getBoolean("increment");
      }
      if (jsonObject.has("pointPos")) {
        pointPos = jsonObject.getInt("pointPos");
      }
      if (jsonObject.has("segmentPos")) {
        segmentPos = jsonObject.getInt("segmentPos");
      }
      if (jsonObject.has("latitude")) {
        latLng = new LatLng(jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude"));
      }
      if (jsonObject.has("nextPointLatitude")) {
        nextPoint = new LatLng(jsonObject.getDouble("nextPointLatitude"), jsonObject.getDouble("nextPointLongitude"));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override public String toString() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("increment", increment);
      jsonObject.put("pointPos", pointPos);
      jsonObject.put("segmentPos", segmentPos);
      if (latLng != null) {
        jsonObject.put("latitude", latLng.latitude);
        jsonObject.put("longitude", latLng.longitude);
      }
      if (nextPoint != null) {
        jsonObject.put("nextPointLatitude", nextPoint.latitude);
        jsonObject.put("nextPointLongitude", nextPoint.longitude);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return jsonObject.toString();
  }

  public boolean isIncrement() {
    return increment;
  }

  public void setIncrement(boolean increment) {
    this.increment = increment;
  }

  public int getPointPos() {
    return pointPos;
  }

  public void setPointPos(int pointPos) {
    this.pointPos = pointPos;
  }

  public LatLng getLatLng() {
    return latLng;
  }

  public void setLatLng(LatLng latLng) {
    this.latLng = latLng;
  }

  public void setSegmentPos(int segmentPos) {
    this.segmentPos = segmentPos;
  }

  public int getSegmentPos() {
    return segmentPos;
  }

  public void set100Meters(boolean a100Meters) {
    this.a100Meters = a100Meters;
  }

  public boolean isA100Meters() {
    return a100Meters;
  }

  public void setA100Meters(boolean a100Meters) {
    this.a100Meters = a100Meters;
  }

  public void set500Meters(boolean a500Meters) {
    this.a500Meters = a500Meters;
  }

  public boolean isA500Meters() {
    return a500Meters;
  }

  public void setA500Meters(boolean a500Meters) {
    this.a500Meters = a500Meters;
  }
}
