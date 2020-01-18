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

import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

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
