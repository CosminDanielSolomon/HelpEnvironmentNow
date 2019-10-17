package it.polito.helpenvironmentnow.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JsonBuilder {
    private int sensorIdTemperature, sensorIdHumidity, timestamp;
    private float temperature, humidity;

    public JSONObject parseAndBuildJson(TempHumMetaData tempHumMetaData, byte[] fixedSensorsData, byte[] variableSensorsData) {
        int numberOfMessages = tempHumMetaData.getNumberOfMessages();
        int messageLength = tempHumMetaData.getMessageLength();
        int timestampLength = tempHumMetaData.getTimestampLength();
        int temperatureLength = tempHumMetaData.getTemperatureLength();
        int humidityLength = tempHumMetaData.getHumidityLength();
        int sensorIdLength = tempHumMetaData.getSensorIdLength();
        JSONObject dataBlock = new JSONObject();
        try {
            // TODO put geohash dataBlock.put("geoHash",1);
            // TODO put altitude dataBlock.put("altitude",1);
            parseSensorIdTemperature(fixedSensorsData, sensorIdLength);
            parseSensorIdHumidity(fixedSensorsData, sensorIdLength);
            dataBlock.put("sensorIdTemperature", sensorIdTemperature);
            dataBlock.put("sensorIdHumidity", sensorIdHumidity);
            JSONArray messagesArray = new JSONArray();
            for(int messageCount = 0; messageCount < numberOfMessages; messageCount++) {
                parseMessage(variableSensorsData, messageCount, messageLength, timestampLength, temperatureLength, humidityLength);
                messagesArray.put(getJSONMessage());
            }
            dataBlock.put("sensorsData", messagesArray);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return dataBlock;
    }

    private JSONObject getJSONMessage() throws JSONException {
        JSONObject tempHumRead = new JSONObject();
        tempHumRead.put("timestamp", timestamp);
        tempHumRead.put("temperature", temperature);
        tempHumRead.put("humidity", humidity);

        return tempHumRead;
    }

    private void parseSensorIdTemperature(byte[] fixedSensorsData, int sensorIdLength) {
        String strTempId = new String(fixedSensorsData, 0, sensorIdLength, StandardCharsets.UTF_8);
        sensorIdTemperature = Integer.parseInt(strTempId);
    }

    private void parseSensorIdHumidity(byte[] fixedSensorsData, int sensorIdLength) {
        String strHumId = new String(fixedSensorsData, sensorIdLength, sensorIdLength, StandardCharsets.UTF_8);

        sensorIdHumidity = Integer.parseInt(strHumId);
    }

    private void parseMessage(byte[] variableSensorsData, int messageCount, int messageLength,
                              int timestampLength, int temperatureLength, int humidityLength) {
        int offset = messageCount * messageLength;
        String strMessage = new String(variableSensorsData, offset, messageLength, StandardCharsets.UTF_8);
        timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
        temperature = Float.parseFloat(strMessage.substring(timestampLength, timestampLength + temperatureLength));
        humidity = Float.parseFloat(strMessage.substring(timestampLength + temperatureLength, timestampLength + temperatureLength + humidityLength));
    }
}
