package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import com.fonfon.geohash.GeoHash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

public class JsonBuilder {

    public JSONObject buildClassicJson(Location location, ParsedData parsedData) {
        JSONObject dataBlock = new JSONObject();
        try {
            dataBlock.put("geo", encodeLocation(location));
            dataBlock.put("alt", location.getAltitude());
            dataBlock.put("idTemp", parsedData.getSensorIdTemperature());
            dataBlock.put("idHum", parsedData.getSensorIdHumidity());
            dataBlock.put("dhtData", buildClassicDht(parsedData.getDhtMeasures()));
            dataBlock.put("pmData", buildClassicPm(parsedData.getPmMeasures()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return dataBlock;
    }

    public JSONObject buildMovementJson(MatchedData matchedData) {
       JSONObject dataBlock = new JSONObject();
        try {
            dataBlock.put("idTemp", matchedData.getSensorIdTemperature());
            dataBlock.put("idHum", matchedData.getSensorIdHumidity());
            dataBlock.put("dhtData", buildMovementDht(matchedData.getMatchedDhtMeasures()));
            dataBlock.put("pmData", buildMovementPm(matchedData.getMatchedPmMeasures()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return dataBlock;
    }

    private String encodeLocation(Location location) {
        final int numberOfChars = 12; // the same size as the corresponding remote database field - varchar(12)
        GeoHash hash = GeoHash.fromLocation(location, numberOfChars);
        return hash.toString();
    }

    private JSONArray buildClassicDht(List<DhtMeasure> dhtMeasures) throws JSONException {
        JSONArray dhtArray = new JSONArray();
        for(DhtMeasure dhtMeasure : dhtMeasures) {
            JSONObject dhtRead = new JSONObject();
            dhtRead.put("ts", dhtMeasure.getTimestamp());
            dhtRead.put("t", dhtMeasure.getTemperature());
            dhtRead.put("h", dhtMeasure.getHumidity());
            dhtArray.put(dhtRead);
        }

        return dhtArray;
    }

    private JSONArray buildMovementDht(List<MatchedDhtMeasure> matchedDhtMeasures) throws JSONException {
        JSONArray dhtArray = new JSONArray();
        for(MatchedDhtMeasure matchedDhtMeasure : matchedDhtMeasures) {
            JSONObject dhtRead = new JSONObject();
            dhtRead.put("ts", matchedDhtMeasure.getTimestamp());
            dhtRead.put("t", matchedDhtMeasure.getTemperature());
            dhtRead.put("h", matchedDhtMeasure.getHumidity());
            dhtRead.put("g", matchedDhtMeasure.getGeohash());
            dhtRead.put("a", matchedDhtMeasure.getAltitude());
            dhtArray.put(dhtRead);
        }

        return dhtArray;
    }

    private JSONArray buildClassicPm(List<PmMeasure> pmMeasures) throws JSONException {
        JSONArray pmArray = new JSONArray();
        for(PmMeasure pmMeasure : pmMeasures) {
            JSONObject jsonPmRead = new JSONObject();
            jsonPmRead.put("ts", pmMeasure.getTimestamp());
            jsonPmRead.put("idP2", pmMeasure.getSensorId25());
            jsonPmRead.put("p2", pmMeasure.getPm25());
            jsonPmRead.put("idP1", pmMeasure.getSensorId10());
            jsonPmRead.put("p1", pmMeasure.getPm10());
            pmArray.put(jsonPmRead);
        }

        return pmArray;
    }

    private JSONArray buildMovementPm(List<MatchedPmMeasure> matchedPmMeasures) throws JSONException {
        JSONArray pmArray = new JSONArray();
        for(MatchedPmMeasure matchedPmMeasure : matchedPmMeasures) {
            JSONObject jsonPmRead = new JSONObject();
            jsonPmRead.put("ts", matchedPmMeasure.getTimestamp());
            jsonPmRead.put("idP2", matchedPmMeasure.getSensorId25());
            jsonPmRead.put("p2", matchedPmMeasure.getPm25());
            jsonPmRead.put("idP1", matchedPmMeasure.getSensorId10());
            jsonPmRead.put("p1", matchedPmMeasure.getPm10());
            jsonPmRead.put("g", matchedPmMeasure.getGeohash());
            jsonPmRead.put("a", matchedPmMeasure.getAltitude());
            pmArray.put(jsonPmRead);
        }

        return pmArray;
    }
}
