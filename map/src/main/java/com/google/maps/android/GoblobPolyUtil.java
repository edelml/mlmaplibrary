//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.google.maps.android;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class GoblobPolyUtil {
  private static final double DEFAULT_TOLERANCE = 0.1D;

  private GoblobPolyUtil() {
  }

  private static double tanLatGC(double lat1, double lat2, double lng2, double lng3) {
    return (Math.tan(lat1) * Math.sin(lng2 - lng3) + Math.tan(lat2) * Math.sin(lng3)) / Math.sin(lng2);
  }

  private static double mercatorLatRhumb(double lat1, double lat2, double lng2, double lng3) {
    return (MathUtil.mercator(lat1) * (lng2 - lng3) + MathUtil.mercator(lat2) * lng3) / lng2;
  }

  private static boolean intersects(double lat1, double lat2, double lng2, double lat3, double lng3, boolean geodesic) {
    if((lng3 < 0.0D || lng3 < lng2) && (lng3 >= 0.0D || lng3 >= lng2)) {
      if(lat3 <= -1.5707963267948966D) {
        return false;
      } else if(lat1 > -1.5707963267948966D && lat2 > -1.5707963267948966D && lat1 < 1.5707963267948966D && lat2 < 1.5707963267948966D) {
        if(lng2 <= -3.141592653589793D) {
          return false;
        } else {
          double linearLat = (lat1 * (lng2 - lng3) + lat2 * lng3) / lng2;
          return lat1 >= 0.0D && lat2 >= 0.0D && lat3 < linearLat?false:(lat1 <= 0.0D && lat2 <= 0.0D && lat3 >= linearLat?true:(lat3 >= 1.5707963267948966D?true:(geodesic?Math.tan(lat3) >= tanLatGC(lat1, lat2, lng2, lng3): MathUtil.mercator(lat3) >= mercatorLatRhumb(lat1, lat2, lng2, lng3))));
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public static boolean containsLocation(LatLng point, List<LatLng> polygon, boolean geodesic) {
    int size = polygon.size();
    if(size == 0) {
      return false;
    } else {
      double lat3 = Math.toRadians(point.latitude);
      double lng3 = Math.toRadians(point.longitude);
      LatLng prev = (LatLng)polygon.get(size - 1);
      double lat1 = Math.toRadians(prev.latitude);
      double lng1 = Math.toRadians(prev.longitude);
      int nIntersect = 0;

      double lng2;
      for(Iterator var14 = polygon.iterator(); var14.hasNext(); lng1 = lng2) {
        LatLng point2 = (LatLng)var14.next();
        double dLng3 = MathUtil.wrap(lng3 - lng1, -3.141592653589793D, 3.141592653589793D);
        if(lat3 == lat1 && dLng3 == 0.0D) {
          return true;
        }

        double lat2 = Math.toRadians(point2.latitude);
        lng2 = Math.toRadians(point2.longitude);
        if(intersects(lat1, lat2, MathUtil.wrap(lng2 - lng1, -3.141592653589793D, 3.141592653589793D), lat3, dLng3, geodesic)) {
          ++nIntersect;
        }

        lat1 = lat2;
      }

      return (nIntersect & 1) != 0;
    }
  }

  public static boolean isLocationOnEdge(LatLng point, List<LatLng> polygon, boolean geodesic, double tolerance) {
    return isLocationOnEdgeOrPath(point, polygon, true, geodesic, tolerance);
  }

  public static boolean isLocationOnEdge(LatLng point, List<LatLng> polygon, boolean geodesic) {
    return isLocationOnEdge(point, polygon, geodesic, 0.1D);
  }

  public static boolean isLocationOnPath(LatLng point, List<LatLng> polyline, boolean geodesic, double tolerance) {
    return isLocationOnEdgeOrPath(point, polyline, false, geodesic, tolerance);
  }

  public static RoutePoint pointOnPath(LatLng point, List<LatLng> polyline, boolean geodesic, double tolerance, int startPos) {
    return pointOnPath(point, polyline, false, geodesic, tolerance, startPos);
  }

  public static boolean isLocationOnPath(LatLng point, List<LatLng> polyline, boolean geodesic) {
    return isLocationOnPath(point, polyline, geodesic, 0.1D);
  }

  private static RoutePoint pointOnPath(LatLng point, List<LatLng> poly, boolean closed, boolean geodesic, double toleranceEarth, int startPos) {
    Log.e("startPos", ""+startPos);
    int size = poly.size();
    if(size == 0) {
      return null;
    } else {
      double tolerance = toleranceEarth / 6371009.0D;
      double havTolerance = MathUtil.hav(tolerance);
      double lat3 = Math.toRadians(point.latitude);
      double lng3 = Math.toRadians(point.longitude);
      LatLng prev = (LatLng)poly.get(closed?size - 1:0);
      double lat1 = Math.toRadians(prev.latitude);
      double lng1 = Math.toRadians(prev.longitude);
      double lat2;
      double y1;
      if(geodesic) {
        LatLng resultPoint = null;
        double minDistance = -1;
        int pointPos = -1;

        boolean increment = true;
        if (startPos == 0) {
          increment = true;
        } else if (startPos == poly.size() - 1) {
          increment = false;
        } else if (startPos -1 < poly.size() && startPos -1 >= 0 && startPos + 1 < poly.size()) {
          LatLng point1 = poly.get(startPos - 1);
          LatLng point2 = poly.get(startPos + 1);

          double dist1 = SphericalUtil.computeDistanceBetween(point, point1);

          double dist2 = SphericalUtil.computeDistanceBetween(point, point2);

          if (dist1 < dist2) {
            increment = false;
          }
        }

        int i = startPos;

        if (i >= poly.size() || i < 0){
          i = 0;
          increment = true;
        }

        while (i < poly.size() && i >= 0) {
          LatLng point2 = poly.get(i);
          lat2 = Math.toRadians(point2.latitude);
          y1 = Math.toRadians(point2.longitude);

          double dist = SphericalUtil.computeDistanceBetween(point, point2);

          if (resultPoint != null && minDistance < dist) {
            LatLng point1, point3;
            if (pointPos == 0) {
              point1 = poly.get(pointPos);
              point3 = poly.get(pointPos + 1);
            } else if (pointPos == poly.size() - 1) {
              point1 = poly.get(pointPos);
              point3 = poly.get(pointPos - 1);
            } else {
              double distance1 = distanceToLine(point, poly.get(pointPos), poly.get(pointPos - 1));
              double distance2 = distanceToLine(point, poly.get(pointPos), poly.get(pointPos + 1));

              if (distance1 < distance2 || Double.isNaN(distance2)) {
                point1 = poly.get(pointPos);
                point3 = poly.get(pointPos - 1);
              } else {
                point1 = poly.get(pointPos);
                point3 = poly.get(pointPos + 1);
              }
            }

            LatLng nextPoint = null;
            if (increment && i + 1 < poly.size()) {
              nextPoint = poly.get(i+1);
            } else if (!increment && i - 1 >= 0){
              nextPoint = poly.get(i-1);
            }
            return new RoutePoint(pointIntersectionToLine(point, point1, point3), pointPos, increment, nextPoint);
          } else if (dist == 0){
            LatLng nextPoint = null;
            if (increment && i + 1 < poly.size()) {
              nextPoint = poly.get(i+1);
            } else if (!increment && i - 1 >= 0){
              nextPoint = poly.get(i-1);
            }
            return new RoutePoint(point2, i, increment, nextPoint);
          }

          if (isOnSegmentGC(lat1, lng1, lat2, y1, lat3, lng3, havTolerance)) {
            resultPoint = point2;
            minDistance = dist;
            pointPos = i;
          }

          lat1 = lat2;

          lng1 = y1;

          if (increment) {
            i++;
          } else {
            i--;
          }
        }
      } else {
        double minAcceptable = lat3 - tolerance;
        lat2 = lat3 + tolerance;
        y1 = MathUtil.mercator(lat1);
        double y3 = MathUtil.mercator(lat3);
        double[] xTry = new double[3];

        double y2;

        LatLng resultPoint = null;
        double minDistance = -1;

        for(Iterator var29 = poly.iterator(); var29.hasNext(); y1 = y2) {
          LatLng point2 = (LatLng)var29.next();
          lat2 = Math.toRadians(point2.latitude);
          y2 = MathUtil.mercator(lat2);
          double lng2 = Math.toRadians(point2.longitude);
          if(Math.max(lat1, lat2) >= minAcceptable && Math.min(lat1, lat2) <= lat2) {
            double x2 = MathUtil.wrap(lng2 - lng1, -3.141592653589793D, 3.141592653589793D);
            double x3Base = MathUtil.wrap(lng3 - lng1, -3.141592653589793D, 3.141592653589793D);
            xTry[0] = x3Base;
            xTry[1] = x3Base + 6.283185307179586D;
            xTry[2] = x3Base - 6.283185307179586D;
            double[] var41 = xTry;
            int var42 = xTry.length;

            for(int var43 = 0; var43 < var42; ++var43) {
              double x3 = var41[var43];
              double dy = y2 - y1;
              double len2 = x2 * x2 + dy * dy;
              double t = len2 <= 0.0D?0.0D: MathUtil.clamp((x3 * x2 + (y3 - y1) * dy) / len2, 0.0D, 1.0D);
              double xClosest = t * x2;
              double yClosest = y1 + t * dy;
              double latClosest = MathUtil.inverseMercator(yClosest);
              double havDist = MathUtil.havDistance(lat3, latClosest, x3 - xClosest);

              if (resultPoint != null && minDistance < havDist){
                return new RoutePoint(resultPoint, 0, true, null);
              }

              if(havDist <= havTolerance) {
                resultPoint = point2;
                minDistance = havDist;
              }
            }
          }

          lat1 = lat2;
          lng1 = lng2;
        }
      }

      return null;
    }
  }

  private static boolean isLocationOnEdgeOrPath(LatLng point, List<LatLng> poly, boolean closed, boolean geodesic, double toleranceEarth) {
    int size = poly.size();
    if(size == 0) {
      return false;
    } else {
      double tolerance = toleranceEarth / 6371009.0D;
      double havTolerance = MathUtil.hav(tolerance);
      double lat3 = Math.toRadians(point.latitude);
      double lng3 = Math.toRadians(point.longitude);
      LatLng prev = (LatLng)poly.get(closed?size - 1:0);
      double lat1 = Math.toRadians(prev.latitude);
      double lng1 = Math.toRadians(prev.longitude);
      double lat2;
      double y1;
      if(geodesic) {
        for(Iterator var20 = poly.iterator(); var20.hasNext(); lng1 = y1) {
          LatLng point2 = (LatLng)var20.next();
          lat2 = Math.toRadians(point2.latitude);
          y1 = Math.toRadians(point2.longitude);
          if(isOnSegmentGC(lat1, lng1, lat2, y1, lat3, lng3, havTolerance)) {
            return true;
          }

          lat1 = lat2;
        }
      } else {
        double minAcceptable = lat3 - tolerance;
        lat2 = lat3 + tolerance;
        y1 = MathUtil.mercator(lat1);
        double y3 = MathUtil.mercator(lat3);
        double[] xTry = new double[3];

        double y2;
        for(Iterator var29 = poly.iterator(); var29.hasNext(); y1 = y2) {
          LatLng point2 = (LatLng)var29.next();
          lat2 = Math.toRadians(point2.latitude);
          y2 = MathUtil.mercator(lat2);
          double lng2 = Math.toRadians(point2.longitude);
          if(Math.max(lat1, lat2) >= minAcceptable && Math.min(lat1, lat2) <= lat2) {
            double x2 = MathUtil.wrap(lng2 - lng1, -3.141592653589793D, 3.141592653589793D);
            double x3Base = MathUtil.wrap(lng3 - lng1, -3.141592653589793D, 3.141592653589793D);
            xTry[0] = x3Base;
            xTry[1] = x3Base + 6.283185307179586D;
            xTry[2] = x3Base - 6.283185307179586D;
            double[] var41 = xTry;
            int var42 = xTry.length;

            for(int var43 = 0; var43 < var42; ++var43) {
              double x3 = var41[var43];
              double dy = y2 - y1;
              double len2 = x2 * x2 + dy * dy;
              double t = len2 <= 0.0D?0.0D: MathUtil.clamp((x3 * x2 + (y3 - y1) * dy) / len2, 0.0D, 1.0D);
              double xClosest = t * x2;
              double yClosest = y1 + t * dy;
              double latClosest = MathUtil.inverseMercator(yClosest);
              double havDist = MathUtil.havDistance(lat3, latClosest, x3 - xClosest);
              if(havDist < havTolerance) {
                return true;
              }
            }
          }

          lat1 = lat2;
          lng1 = lng2;
        }
      }

      return false;
    }
  }

  private static double sinDeltaBearing(double lat1, double lng1, double lat2, double lng2, double lat3, double lng3) {
    double sinLat1 = Math.sin(lat1);
    double cosLat2 = Math.cos(lat2);
    double cosLat3 = Math.cos(lat3);
    double lat31 = lat3 - lat1;
    double lng31 = lng3 - lng1;
    double lat21 = lat2 - lat1;
    double lng21 = lng2 - lng1;
    double a = Math.sin(lng31) * cosLat3;
    double c = Math.sin(lng21) * cosLat2;
    double b = Math.sin(lat31) + 2.0D * sinLat1 * cosLat3 * MathUtil.hav(lng31);
    double d = Math.sin(lat21) + 2.0D * sinLat1 * cosLat2 * MathUtil.hav(lng21);
    double denom = (a * a + b * b) * (c * c + d * d);
    return denom <= 0.0D?1.0D:(a * d - b * c) / Math.sqrt(denom);
  }

  private static boolean isOnSegmentGC(double lat1, double lng1, double lat2, double lng2, double lat3, double lng3, double havTolerance) {
    double havDist13 = MathUtil.havDistance(lat1, lat3, lng1 - lng3);
    if(havDist13 <= havTolerance) {
      return true;
    } else {
      double havDist23 = MathUtil.havDistance(lat2, lat3, lng2 - lng3);
      if(havDist23 <= havTolerance) {
        return true;
      } else {
        double sinBearing = sinDeltaBearing(lat1, lng1, lat2, lng2, lat3, lng3);
        double sinDist13 = MathUtil.sinFromHav(havDist13);
        double havCrossTrack = MathUtil.havFromSin(sinDist13 * sinBearing);
        if(havCrossTrack > havTolerance) {
          return false;
        } else {
          double havDist12 = MathUtil.havDistance(lat1, lat2, lng1 - lng2);
          double term = havDist12 + havCrossTrack * (1.0D - 2.0D * havDist12);
          if(havDist13 <= term && havDist23 <= term) {
            if(havDist12 < 0.74D) {
              return true;
            } else {
              double cosCrossTrack = 1.0D - 2.0D * havCrossTrack;
              double havAlongTrack13 = (havDist13 - havCrossTrack) / cosCrossTrack;
              double havAlongTrack23 = (havDist23 - havCrossTrack) / cosCrossTrack;
              double sinSumAlongTrack = MathUtil.sinSumFromHav(havAlongTrack13, havAlongTrack23);
              return sinSumAlongTrack > 0.0D;
            }
          } else {
            return false;
          }
        }
      }
    }
  }

  public static List<LatLng> simplify(List<LatLng> poly, double tolerance) {
    int n = poly.size();
    if(n < 1) {
      throw new IllegalArgumentException("Polyline must have at least 1 point");
    } else if(tolerance <= 0.0D) {
      throw new IllegalArgumentException("Tolerance must be greater than zero");
    } else {
      boolean closedPolygon = isClosedPolygon(poly);
      LatLng lastPoint = null;
      if(closedPolygon) {
        double OFFSET = 1.0E-11D;
        lastPoint = (LatLng)poly.get(poly.size() - 1);
        poly.remove(poly.size() - 1);
        poly.add(new LatLng(lastPoint.latitude + 1.0E-11D, lastPoint.longitude + 1.0E-11D));
      }

      int maxIdx = 0;
      Stack<int[]> stack = new Stack();
      double[] dists = new double[n];
      dists[0] = 1.0D;
      dists[n - 1] = 1.0D;
      double dist = 0.0D;
      int idx;
      if(n > 2) {
        int[] stackVal = new int[]{0, n - 1};
        stack.push(stackVal);

        while(stack.size() > 0) {
          int[] current = (int[])stack.pop();
          double maxDist = 0.0D;

          for(idx = current[0] + 1; idx < current[1]; ++idx) {
            dist = distanceToLine((LatLng)poly.get(idx), (LatLng)poly.get(current[0]), (LatLng)poly.get(current[1]));
            if(dist > maxDist) {
              maxDist = dist;
              maxIdx = idx;
            }
          }

          if(maxDist > tolerance) {
            dists[maxIdx] = maxDist;
            int[] stackValCurMax = new int[]{current[0], maxIdx};
            stack.push(stackValCurMax);
            int[] stackValMaxCur = new int[]{maxIdx, current[1]};
            stack.push(stackValMaxCur);
          }
        }
      }

      if(closedPolygon) {
        poly.remove(poly.size() - 1);
        poly.add(lastPoint);
      }

      idx = 0;
      ArrayList<LatLng> simplifiedLine = new ArrayList();

      for(Iterator var20 = poly.iterator(); var20.hasNext(); ++idx) {
        LatLng l = (LatLng)var20.next();
        if(dists[idx] != 0.0D) {
          simplifiedLine.add(l);
        }
      }

      return simplifiedLine;
    }
  }

  public static boolean isClosedPolygon(List<LatLng> poly) {
    LatLng firstPoint = (LatLng)poly.get(0);
    LatLng lastPoint = (LatLng)poly.get(poly.size() - 1);
    return firstPoint.equals(lastPoint);
  }

  public static LatLng pointIntersectionToLine(LatLng p, LatLng start, LatLng end) {
    if(start.equals(end)) {
      SphericalUtil.computeDistanceBetween(end, p);
    }

    double s0lat = Math.toRadians(p.latitude);
    double s0lng = Math.toRadians(p.longitude);
    double s1lat = Math.toRadians(start.latitude);
    double s1lng = Math.toRadians(start.longitude);
    double s2lat = Math.toRadians(end.latitude);
    double s2lng = Math.toRadians(end.longitude);
    double s2s1lat = s2lat - s1lat;
    double s2s1lng = s2lng - s1lng;
    double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng) / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
    if(u <= 0.0D) {
      return start;
    } else if(u >= 1.0D) {
      return end;
    } else {
      LatLng sa = new LatLng(p.latitude - start.latitude, p.longitude - start.longitude);
      LatLng sb = new LatLng(u * (end.latitude - start.latitude), u * (end.longitude - start.longitude));
      return new LatLng(start.latitude+sb.latitude, start.longitude+sb.longitude);
    }
  }

  public static double distanceToLine(LatLng p, LatLng start, LatLng end) {
    if(start.equals(end)) {
      SphericalUtil.computeDistanceBetween(end, p);
    }

    double s0lat = Math.toRadians(p.latitude);
    double s0lng = Math.toRadians(p.longitude);
    double s1lat = Math.toRadians(start.latitude);
    double s1lng = Math.toRadians(start.longitude);
    double s2lat = Math.toRadians(end.latitude);
    double s2lng = Math.toRadians(end.longitude);
    double s2s1lat = s2lat - s1lat;
    double s2s1lng = s2lng - s1lng;
    double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng) / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
    if(u <= 0.0D) {
      return SphericalUtil.computeDistanceBetween(p, start);
    } else if(u >= 1.0D) {
      return SphericalUtil.computeDistanceBetween(p, end);
    } else {
      LatLng sa = new LatLng(p.latitude - start.latitude, p.longitude - start.longitude);
      LatLng sb = new LatLng(u * (end.latitude - start.latitude), u * (end.longitude - start.longitude));
      return SphericalUtil.computeDistanceBetween(sa, sb);
    }
  }

  public static List<LatLng> decode(String encodedPath) {
    int len = encodedPath.length();
    List<LatLng> path = new ArrayList();
    int index = 0;
    int lat = 0;
    int lng = 0;

    while(index < len) {
      int result = 1;
      int shift = 0;

      int b;
      do {
        b = encodedPath.charAt(index++) - 63 - 1;
        result += b << shift;
        shift += 5;
      } while(b >= 31);

      lat += (result & 1) != 0?~(result >> 1):result >> 1;
      result = 1;
      shift = 0;

      do {
        b = encodedPath.charAt(index++) - 63 - 1;
        result += b << shift;
        shift += 5;
      } while(b >= 31);

      lng += (result & 1) != 0?~(result >> 1):result >> 1;
      path.add(new LatLng((double)lat * 1.0E-5D, (double)lng * 1.0E-5D));
    }

    return path;
  }

  public static String encode(List<LatLng> path) {
    long lastLat = 0L;
    long lastLng = 0L;
    StringBuffer result = new StringBuffer();

    long lng;
    for(Iterator var6 = path.iterator(); var6.hasNext(); lastLng = lng) {
      LatLng point = (LatLng)var6.next();
      long lat = Math.round(point.latitude * 100000.0D);
      lng = Math.round(point.longitude * 100000.0D);
      long dLat = lat - lastLat;
      long dLng = lng - lastLng;
      encode(dLat, result);
      encode(dLng, result);
      lastLat = lat;
    }

    return result.toString();
  }

  private static void encode(long v, StringBuffer result) {
    for(v = v < 0L?~(v << 1):v << 1; v >= 32L; v >>= 5) {
      result.append(Character.toChars((int)((32L | v & 31L) + 63L)));
    }

    result.append(Character.toChars((int)(v + 63L)));
  }
}
