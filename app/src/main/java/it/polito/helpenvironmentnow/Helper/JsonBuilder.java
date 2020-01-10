package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import com.fonfon.geohash.GeoHash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JsonBuilder {

    public JSONObject parseAndBuildJson(Location location, DhtMetaData dhtMetaData, byte[] fixedSensorsData,
                                        byte[] dhtVariableData, PmMetaData pmMetaData, byte[] pmVariableData) {
        int dhtReads = dhtMetaData.getNumberOfReads();
        int dhtReadLength = dhtMetaData.getReadLength();
        int dhtTimestampLength = dhtMetaData.getTimestampLength();
        int temperatureLength = dhtMetaData.getTemperatureLength();
        int humidityLength = dhtMetaData.getHumidityLength();
        int dhtSensorIdLength = dhtMetaData.getSensorIdLength();

        int pmReads = pmMetaData.getNumberOfReads();
        int pmReadLength = pmMetaData.getReadLength();
        int pmValueLength =  pmMetaData.getPmValueLength();
        int pmTimestampLength = pmMetaData.getTimestampLength();
        int pmSensorIdLength = pmMetaData.getSensorIdLength();

        JSONObject dataBlock = new JSONObject();
        try {
            dataBlock.put("geo", encodeLocation(location));
            dataBlock.put("alt", location.getAltitude());
            dataBlock.put("idTemp", parseSensorIdTemperature(fixedSensorsData, dhtSensorIdLength));
            dataBlock.put("idHum", parseSensorIdHumidity(fixedSensorsData, dhtSensorIdLength));
            dataBlock.put("dhtData", parseDhtData(dhtVariableData, dhtReads, dhtReadLength, dhtTimestampLength, temperatureLength, humidityLength));
            dataBlock.put("pmData", parsePmData(pmVariableData, pmReads, pmReadLength, pmTimestampLength, pmValueLength, pmSensorIdLength));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return dataBlock;
    }

    private String encodeLocation(Location location) {
        final int numberOfChars = 12; // the same size as the corresponding database field - varchar(12)
        GeoHash hash = GeoHash.fromLocation(location, numberOfChars);
        return hash.toString();
    }

    private int parseSensorIdTemperature(byte[] fixedSensorsData, int sensorIdLength) {
        String strTempId = new String(fixedSensorsData, 0, sensorIdLength, StandardCharsets.UTF_8);
        int sensorIdTemperature = Integer.parseInt(strTempId);
        return sensorIdTemperature;
    }

    private int parseSensorIdHumidity(byte[] fixedSensorsData, int sensorIdLength) {
        String strHumId = new String(fixedSensorsData, sensorIdLength, sensorIdLength, StandardCharsets.UTF_8);
        int sensorIdHumidity = Integer.parseInt(strHumId);
        return sensorIdHumidity;
    }

    private JSONArray parseDhtData(byte[] dhtData, int dhtReads, int dhtReadLength, int timestampLength,
                                   int temperatureLength, int humidityLength) throws JSONException {
        String strMessage;
        int offset;

        JSONArray dhtArray = new JSONArray();
        for(int readCount = 0; readCount < dhtReads; readCount++) {
            offset = readCount * dhtReadLength;
            strMessage = new String(dhtData, offset, dhtReadLength, StandardCharsets.UTF_8);
            int timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
            double temperature = Double.parseDouble(strMessage.substring(timestampLength,
                    timestampLength + temperatureLength));
            double humidity = Double.parseDouble(strMessage.substring(timestampLength + temperatureLength,
                    timestampLength + temperatureLength + humidityLength));
            JSONObject dhtRead = new JSONObject();
            dhtRead.put("ts", timestamp);
            dhtRead.put("t", temperature);
            dhtRead.put("h", humidity);
            dhtArray.put(dhtRead);
        }

        return dhtArray;
    }

    private JSONArray parsePmData(byte[] pmData, int pmReads, int pmReadLength, int timestampLength,
                                  int pmValueLength, int sensorIdLength) throws JSONException {
        String strMessage;
        int offset;

        JSONArray pmArray = new JSONArray();
        for(int readCount = 0; readCount < pmReads; readCount++) {
            offset = readCount * pmReadLength;
            strMessage = new String(pmData, offset, pmReadLength, StandardCharsets.UTF_8);
            int timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
            int pm25 = Integer.parseInt(strMessage.substring(timestampLength, timestampLength + pmValueLength));
            int pm10 = Integer.parseInt(strMessage.substring(timestampLength + pmValueLength,
                    timestampLength + pmValueLength + pmValueLength));
            int sensorId25 = Integer.parseInt(strMessage.substring(timestampLength + pmValueLength +
                    pmValueLength, timestampLength + pmValueLength + pmValueLength + sensorIdLength));
            int sensorId10 = Integer.parseInt(strMessage.substring(timestampLength + pmValueLength + pmValueLength + sensorIdLength,
                    timestampLength + pmValueLength + pmValueLength + sensorIdLength + sensorIdLength));
            JSONObject jsonPmRead = new JSONObject();
            jsonPmRead.put("ts", timestamp);
            jsonPmRead.put("idP2", sensorId25);
            jsonPmRead.put("p2", pm25);
            jsonPmRead.put("idP1", sensorId10);
            jsonPmRead.put("p1", pm10);
            pmArray.put(jsonPmRead);
        }

        return pmArray;
    }
}
