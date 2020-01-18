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

public class Matcher {

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
            // MOVEMENT MODE. I have to match the measures with the recorded locations
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
                    matchedPos = positionsMap.get(matchedTimestamp);
                    measure.geoHash = LocationInfo.encodeLocation(matchedPos.latitude, matchedPos.longitude);
                    measure.altitude = matchedPos.altitude;
                }
            } else {
                for (Measure measure : parsedMeasures) {
                    measure.geoHash = LocationInfo.encodeLocation(0.0, 0.0);
                    measure.altitude = 0.0;
                }
            }
        }

    }

    private Map<Integer, Position> getMapOfPositions(List<Integer> timestamps, MyDb myDb) {
        int minT = Collections.min(timestamps);
        int maxT = Collections.max(timestamps);

        Position[] positions = myDb.selectPositions(minT, maxT);
        Map<Integer, Position> positionsMap = new HashMap<>();
        for(Position p : positions)
            positionsMap.put(p.timestamp, p);
        // I delete all the positions that I extract because I don't need them in the future
        myDb.deletePositions(maxT);

        return positionsMap;
    }

    private int searchMatch(Map<Integer, Position> positionsMap, int currentTimestamp) {
        int matchedTimestamp = 0;

        if (positionsMap.containsKey(currentTimestamp)) {

            // EXACT MATCH
            matchedTimestamp = currentTimestamp;

        } else {

            // APPROXIMATE MATCH - search for nearest position based on timestamp
            boolean match = false;
            int lowerTimestamp, upperTimestamp, gap = 1;
            while (!match) {
                lowerTimestamp = currentTimestamp - gap;
                upperTimestamp = currentTimestamp + gap;
                if (positionsMap.containsKey(lowerTimestamp)) {
                    matchedTimestamp = lowerTimestamp;
                    match = true;
                } else if (positionsMap.containsKey(upperTimestamp)) {
                    matchedTimestamp = upperTimestamp;
                    match = true;
                } else {
                    gap++;
                }
            }

        }

        return matchedTimestamp;
    }
}
