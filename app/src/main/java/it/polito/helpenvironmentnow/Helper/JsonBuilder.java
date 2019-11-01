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
            dataBlock.put("geoHash", encodeLocation(location));
            dataBlock.put("altitude", location.getAltitude());
            dataBlock.put("sensorIdTemperature", parseSensorIdTemperature(fixedSensorsData, dhtSensorIdLength));
            dataBlock.put("sensorIdHumidity", parseSensorIdHumidity(fixedSensorsData, dhtSensorIdLength));
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
            float temperature = Float.parseFloat(strMessage.substring(timestampLength,
                    timestampLength + temperatureLength));
            float humidity = Float.parseFloat(strMessage.substring(timestampLength + temperatureLength,
                    timestampLength + temperatureLength + humidityLength));
            JSONObject dhtRead = new JSONObject();
            dhtRead.put("timestamp", timestamp);
            dhtRead.put("temperature", temperature);
            dhtRead.put("humidity", humidity);
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
            jsonPmRead.put("timestamp", timestamp);
            jsonPmRead.put("sensorIdPm25", sensorId25);
            jsonPmRead.put("pm25Value", pm25);
            jsonPmRead.put("sensorIdPm10", sensorId10);
            jsonPmRead.put("pm10Value", pm10);
            pmArray.put(jsonPmRead);
        }

        return pmArray;
    }
}
