package it.polito.helpenvironmentnow.Helper;

import android.location.Location;
import android.util.Log;

import com.fonfon.geohash.GeoHash;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.Position;

public class Matcher {

    public MatchedData matchMeasuresAndPositions(ParsedData parsedData, Map<Integer, Position> positionsMap) {
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
    }

    private String encodeLocation(double latitude, double longitude) {
        final int numberOfChars = 12; // the same size as the corresponding remote database field - varchar(12)

        Location location = new Location("geohash");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        GeoHash hash = GeoHash.fromLocation(location, numberOfChars);
        return hash.toString();
    }

    private int searchMatch(Map<Integer, Position> positionsMap, int currentTimestamp) {
        int matchedTimestamp = 0;

        if (positionsMap.containsKey(currentTimestamp)) {

            // EXACT MATCH
            matchedTimestamp = currentTimestamp;
            Log.d("MATCHER", "exact MATCH");

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
