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

    /*public JSONObject buildMovementJson(ParsedData parsedData) {
        //Map<Integer, Position> positions = new HashMap<>(myDb.selectPositions())
       JSONObject dataBlock = new JSONObject();
        try {
            dataBlock.put("idTemp", parsedData.getSensorIdTemperature());
            dataBlock.put("idHum", parsedData.getSensorIdHumidity());
            dataBlock.put("dhtData", buildMovementDht());
            dataBlock.put("pmData", );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return dataBlock;
    }*/

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

    private JSONArray buildMovementDht(List<DhtMeasure> dhtMeasures) throws JSONException {
        JSONArray dhtArray = new JSONArray();
        for(DhtMeasure dhtMeasure : dhtMeasures) {
            JSONObject dhtRead = new JSONObject();
            dhtRead.put("ts", dhtMeasure.getTimestamp());
            dhtRead.put("t", dhtMeasure.getTemperature());
            dhtRead.put("h", dhtMeasure.getHumidity());
            //dhtRead.put("h", geohash);
            //dhtRead.put("a", altitude);
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
}
