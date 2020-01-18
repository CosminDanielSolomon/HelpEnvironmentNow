package it.polito.helpenvironmentnow.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import it.polito.helpenvironmentnow.Storage.Measure;

public class JsonBuilder {

    public JSONObject buildDataBlock(List<Measure> measures) {
        JSONObject dataBlock = new JSONObject();

        JSONArray array = new JSONArray();
        try {
            for(Measure measure : measures) {
                JSONObject o = new JSONObject();
                o.put("s", measure.sensorId);
                o.put("t", measure.timestamp);
                o.put("d", measure.data);
                o.put("g", measure.geoHash);
                o.put("a", measure.altitude);
                array.put(o);
            }
            dataBlock.put("data", array);
        } catch (JSONException e) {
            return null;
        }

        return dataBlock;
    }
}
