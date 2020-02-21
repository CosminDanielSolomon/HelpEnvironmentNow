package it.polito.helpenvironmentnow.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

// This class is used to associate a geographical position to the measures received from the
// Raspberry Pi during the DYNAMIC mode
public class Matcher {
    private final double defaultAltitude = 0.0;
    private final double defaultLatitude = 0.0;
    private final double defaultLongitude = 0.0;

    private final int MAX_GAP = 30; // the max number of seconds acceptable for the match between measure timestamp and position timestamp
    private final int NO_MATCH = -1; // value that means that it was not possible to find a match with a position for the measure

    // This method receives the list of measures that need to be associated to a location.
    public void matchMeasuresAndPositions(List<Measure> measures, MyDb myDb) {

        // I have to match the measures with the recorded positions(that have been previously
        // inserted in the database by the DynamicService)
        List<Integer> timestamps = new ArrayList<>(measures.size());
        for(Measure measure : measures) {
            timestamps.add(measure.timestamp);
        }
        Map<Integer, Position> positionsMap = getMapOfPositions(timestamps, myDb);
        if(positionsMap.size() > 0) {
            int matchedTimestamp;
            Position matchedPos;
            for (Measure measure : measures) {
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
            // the current measures. Normally this should not happen but it could occur if the
            // Raspberry Pi and the Android device have very different datetime(big de-synchronization)
            for (Measure measure : measures) {
                measure.geoHash = LocationInfo.encodeLocation(defaultLatitude, defaultLongitude);
                measure.altitude = defaultAltitude;
            }
        }

    }

    // Gets from the local db all the positions that have been registered
    private Map<Integer, Position> getMapOfPositions(List<Integer> timestamps, MyDb myDb) {
        int minT = Collections.min(timestamps);
        int maxT = Collections.max(timestamps);

        Position[] positions = myDb.selectPositions(minT, maxT);
        Map<Integer, Position> positionsMap = new HashMap<>();
        for(Position p : positions)
            positionsMap.put(p.timestamp, p);

        myDb.deletePositions(maxT - MAX_GAP);

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
