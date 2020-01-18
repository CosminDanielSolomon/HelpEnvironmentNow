package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import com.fonfon.geohash.GeoHash;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.Position;

public class Matcher {

    /*public MatchedData matchMeasuresAndPositions(ParsedData parsedData, Map<Integer, Position> positionsMap) {
        List<MatchedDhtMeasure> matchedDhtMeasures = new ArrayList<>();
        List<MatchedPmMeasure> matchedPmMeasures = new ArrayList<>();

        if(positionsMap.size() == 0) {
            // No positions available for the matching. So I put 0.0 for latitude, longitude and altitude
            // for all the measures received from the Raspberry Pi
            for (DhtMeasure dhtMeasure : parsedData.getDhtMeasures()) {
                MatchedDhtMeasure matchedDhtMeasure = new MatchedDhtMeasure(dhtMeasure.getTimestamp(),
                        dhtMeasure.getTemperature(), dhtMeasure.getHumidity(),
                        encodeLocation(0.0, 0.0), 0.0);
                matchedDhtMeasures.add(matchedDhtMeasure);
            }
            for(PmMeasure pmMeasure : parsedData.getPmMeasures()) {
                MatchedPmMeasure matchedPmMeasure = new MatchedPmMeasure(pmMeasure.getTimestamp(),
                        pmMeasure.getSensorId25(), pmMeasure.getPm25(), pmMeasure.getSensorId10(),
                        pmMeasure.getPm10(), encodeLocation(0.0, 0.0), 0.0);
                matchedPmMeasures.add(matchedPmMeasure);
            }
        } else {
            // Matching fot DHT measures
            for (DhtMeasure dhtMeasure : parsedData.getDhtMeasures()) {
                int currentTimestamp = dhtMeasure.getTimestamp();
                int matchedTimestamp = searchMatch(positionsMap, currentTimestamp);

                Position pos = positionsMap.get(matchedTimestamp);
                MatchedDhtMeasure matchedDhtMeasure = new MatchedDhtMeasure(currentTimestamp,
                        dhtMeasure.getTemperature(), dhtMeasure.getHumidity(),
                        encodeLocation(pos.latitude, pos.longitude), pos.altitude);
                matchedDhtMeasures.add(matchedDhtMeasure);
            }

            // Matching fot PM measures
            for (PmMeasure pmMeasure : parsedData.getPmMeasures()) {
                int currentTimestamp = pmMeasure.getTimestamp();
                int matchedTimestamp = searchMatch(positionsMap, currentTimestamp);

                Position pos = positionsMap.get(matchedTimestamp);
                MatchedPmMeasure matchedPmMeasure = new MatchedPmMeasure(pmMeasure.getTimestamp(),
                        pmMeasure.getSensorId25(), pmMeasure.getPm25(), pmMeasure.getSensorId10(),
                        pmMeasure.getPm10(), encodeLocation(pos.latitude, pos.longitude), pos.altitude);
                matchedPmMeasures.add(matchedPmMeasure);
            }
        }

        return new MatchedData(parsedData.getSensorIdTemperature(),
                parsedData.getSensorIdHumidity(), matchedDhtMeasures, matchedPmMeasures);
    }*/

    //    private Map<Integer, Position> getMapOfPositions(ParsedData parsedData) {
//        int[] timestamps = getMinMaxTimestamp(parsedData);
//        MyDb myDb = new MyDb(getApplicationContext());
//        Position[] positions = myDb.selectPositions(timestamps[0], timestamps[1]);
//        Map<Integer, Position> positionsMap = new HashMap<>();
//        for(Position p : positions)
//            positionsMap.put(p.timestamp, p);
//        // I delete all the positions that I extract because I don't need them in the future
//        myDb.deletePositions(timestamps[1]);
//        myDb.closeDb();
//
//        return positionsMap;
//    }

    //    // Gets the min and max timestamp from the received environmental data
//    // min and max timestamps are then used to extract locations from local db to match them together
//    private int[] getMinMaxTimestamp(ParsedData parsedData) {
//        int[] result = new int[2]; // index 0 for min and 1 for max
//
//        int minDht = (Collections.min(parsedData.getDhtMeasures())).getTimestamp();
//        int maxDht = (Collections.max(parsedData.getDhtMeasures())).getTimestamp();
//        int minPm = (Collections.min(parsedData.getPmMeasures())).getTimestamp();
//        int maxPm = (Collections.max(parsedData.getPmMeasures())).getTimestamp();
//
//        result[0] = minDht <= minPm ? minDht : minPm;
//        result[1] = maxDht >= maxPm ? maxDht : maxPm;
//
//        return result;
//    }

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
