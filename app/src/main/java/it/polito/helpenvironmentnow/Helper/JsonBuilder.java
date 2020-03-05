package it.polito.helpenvironmentnow.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import it.polito.helpenvironmentnow.Storage.Measure;

public class JsonBuilder {

    public JSONObject buildDataBlock(List<Measure> measures) {
        JSONObject dataBlock = new JSONObject();

        JSONArray array = new JSONArray();
        try {
            for(Measure measure : measures) {
                BigDecimal bd = new BigDecimal( measure.altitude).setScale(1, RoundingMode.HALF_UP);
                double altitude = bd.doubleValue();
                JSONObject o = new JSONObject();
                o.put("s", measure.sensorId);
                o.put("t", measure.timestamp);
                o.put("d", measure.data);
                o.put("g", measure.geoHash);
                o.put("a", altitude);
                array.put(o);
            }
            dataBlock.put("data", array);
        } catch (JSONException e) {
            return null;
        }

        return dataBlock;
    }
}
