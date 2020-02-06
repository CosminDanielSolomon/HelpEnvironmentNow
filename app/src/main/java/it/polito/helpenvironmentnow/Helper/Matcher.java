package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

// This class is used to associate a geographical position to the measures received from the Raspberry Pi
public class Matcher {
    private final double defaultAltitude = 0.0;
    private final double defaultLatitude = 0.0;
    private final double defaultLongitude = 0.0;

    private final int MAX_GAP = 30; // the max number of seconds acceptable for the match between measure timestamp and position timestamp
    private final int NO_MATCH = -1; // value that means that it was not possible to find a match with a position for the measure
    // This field is only used for the MOVEMENT mode. It is used to save the maximum timestamp that
    // is encountered during the reading of data from the Raspberry Pi device.
    // At the end of the reading and matching phase all positions saved in the local database('Position' table)
    // are deleted because they will not be used anymore in the future.
    private int maxOverallTimestamp = 0;

    // This method receives the list of measures that need to be associated to a location.
    // If this method is called by the CLASSIC service the first parameter is NOT null, so all the
    // measures will be associated to that position.
    // Otherwise if the method is called by the MOVEMENT service the first parameter is null, so each
    // measure will be associated to one of the positions that are extracted from the local database.
    public void matchMeasuresAndPositions(Location location, List<Measure> parsedMeasures, MyDb myDb) {

        if(location != null) {
            // CLASSIC MODE. I put the received location for all the measures received
            // from the Raspberry Pi

            String geoHash = LocationInfo.encodeLocation(location);
            double altitude = location.getAltitude();
            for(Measure measure : parsedMeasures) {
                measure.geoHash = geoHash;
                measure.altitude = altitude;
            }
        } else {
            // MOVEMENT MODE. I have to match the measures with the recorded positions(that have been
            // previously inserted in the database by the LocationService)

            List<Integer> timestamps = new ArrayList<>(parsedMeasures.size());
            for(Measure measure : parsedMeasures) {
                timestamps.add(measure.timestamp);
            }
            Map<Integer, Position> positionsMap = getMapOfPositions(timestamps, myDb);
            if(positionsMap.size() > 0) {
                int matchedTimestamp;
                Position matchedPos;
                for (Measure measure : parsedMeasures) {
                    matchedTimestamp = searchMatch(positionsMap, measure.timestamp);
                    if(matchedTimestamp != NO_MATCH) {
                        matchedPos = positionsMap.get(matchedTimestamp);
                        if (matchedPos != null) {
                            measure.geoHash = LocationInfo.encodeLocation(matchedPos.latitude, matchedPos.longitude);
                            measure.altitude = matchedPos.altitude;
                        } else {
                            // normally this should not happen because the matchedTimestamp is always included in positionMap
                            // but AndroidStudio complains about it could be a null pointer
                            measure.geoHash = LocationInfo.encodeLocation(defaultLatitude, defaultLongitude);
                            measure.altitude = defaultAltitude;
                        }
                    } else {
                        measure.geoHash = LocationInfo.encodeLocation(defaultLatitude, defaultLongitude);
                        measure.altitude = defaultAltitude;
                    }
                }
            } else {

                // This case happens if the Position table doesn't contain any position for the
                // the current measures. Normally this should not happen but it occurs if the user
                // has started the Raspberry Pi a lot of time before to enable the MOVEMENT
                // mode on the Android device(so no location have been registered) or if the
                // Raspberry Pi and the Android device have very different datetime(big de-synchronization)
                for (Measure measure : parsedMeasures) {
                    measure.geoHash = LocationInfo.encodeLocation(defaultLatitude, defaultLongitude);
                    measure.altitude = defaultAltitude;
                }
            }
        }

    }

    public int getMaxOverallTimestamp() {
        return maxOverallTimestamp;
    }

    // Gets from the local db all the positions that have been registered
    private Map<Integer, Position> getMapOfPositions(List<Integer> timestamps, MyDb myDb) {
        int minT = Collections.min(timestamps);
        int maxT = Collections.max(timestamps);

        Position[] positions = myDb.selectPositions(minT, maxT);
        Map<Integer, Position> positionsMap = new HashMap<>();
        for(Position p : positions)
            positionsMap.put(p.timestamp, p);

        if (maxT > maxOverallTimestamp)
            maxOverallTimestamp = maxT;

        return positionsMap;
    }

    // returns the timestamp of the matched position or NO_MATCH if no position available for the measure
    private int searchMatch(Map<Integer, Position> positionsMap, int currentTimestamp) {
        int matchedTimestamp = NO_MATCH;

        if (positionsMap.containsKey(currentTimestamp)) {

            // EXACT MATCH - if there is a Position with the same timestamp
            matchedTimestamp = currentTimestamp;

        } else {

            // APPROXIMATE MATCH - search for nearest position based on timestamp
            int lowerTimestamp, upperTimestamp, gap = 1;
            while (matchedTimestamp == NO_MATCH && gap <= MAX_GAP) {
                lowerTimestamp = currentTimestamp - gap;
                upperTimestamp = currentTimestamp + gap;
                if (positionsMap.containsKey(lowerTimestamp)) {
                    matchedTimestamp = lowerTimestamp;
                } else if (positionsMap.containsKey(upperTimestamp)) {
                    matchedTimestamp = upperTimestamp;
                } else {
                    gap++;
                }
            }

        }

        return matchedTimestamp;
    }
}
