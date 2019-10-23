package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import com.fonfon.geohash.GeoHash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JsonBuilder {

    public JSONObject parseAndBuildJson(Location location, TempHumMetaData tempHumMetaData, byte[] fixedSensorsData, byte[] variableSensorsData) {
        int numberOfMessages = tempHumMetaData.getNumberOfMessages();
        int messageLength = tempHumMetaData.getMessageLength();
        int timestampLength = tempHumMetaData.getTimestampLength();
        int temperatureLength = tempHumMetaData.getTemperatureLength();
        int humidityLength = tempHumMetaData.getHumidityLength();
        int sensorIdLength = tempHumMetaData.getSensorIdLength();
        JSONObject dataBlock = new JSONObject();
        try {
            dataBlock.put("geoHash", encodeLocation(location));
            dataBlock.put("altitude", location.getAltitude());
            dataBlock.put("sensorIdTemperature", parseSensorIdTemperature(fixedSensorsData, sensorIdLength));
            dataBlock.put("sensorIdHumidity", parseSensorIdHumidity(fixedSensorsData, sensorIdLength));
            JSONArray messagesArray = new JSONArray();
            for(int messageCount = 0; messageCount < numberOfMessages; messageCount++)
                messagesArray.put(parseMessage(variableSensorsData, messageCount, messageLength, timestampLength, temperatureLength, humidityLength));
            dataBlock.put("sensorsData", messagesArray);
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

    private JSONObject parseMessage(byte[] variableSensorsData, int messageCount, int messageLength,
                              int timestampLength, int temperatureLength, int humidityLength) throws JSONException {
        int offset = messageCount * messageLength;
        String strMessage = new String(variableSensorsData, offset, messageLength, StandardCharsets.UTF_8);
        int timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
        float temperature = Float.parseFloat(strMessage.substring(timestampLength,
                timestampLength + temperatureLength));
        float humidity = Float.parseFloat(strMessage.substring(timestampLength + temperatureLength,
                timestampLength + temperatureLength + humidityLength));
        JSONObject tempHumRead = new JSONObject();
        tempHumRead.put("timestamp", timestamp);
        tempHumRead.put("temperature", temperature);
        tempHumRead.put("humidity", humidity);

        return tempHumRead;
    }
}
