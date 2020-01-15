package it.polito.helpenvironmentnow.Helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// This class is used to parse the stream of bytes received from the Raspberry Pi
public class Parser {
    public ParsedData parseEnvironmentalData(DhtMetaData dhtMetaData, byte[] fixedSensorsData,
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

        int sensorIdTemperature = parseSensorIdTemperature(fixedSensorsData, dhtSensorIdLength);
        int sensorIdHumidity = parseSensorIdHumidity(fixedSensorsData, dhtSensorIdLength);
        List<DhtMeasure> dhtMeasures = parseDhtData(dhtVariableData, dhtReads, dhtReadLength, dhtTimestampLength, temperatureLength, humidityLength);
        List<PmMeasure> pmMeasures = parsePmData(pmVariableData, pmReads, pmReadLength, pmTimestampLength, pmValueLength, pmSensorIdLength);

        return new ParsedData(sensorIdTemperature, sensorIdHumidity, dhtMeasures, pmMeasures);
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

    private List<DhtMeasure> parseDhtData(byte[] dhtData, int dhtReads, int dhtReadLength, int timestampLength,
                                          int temperatureLength, int humidityLength) {
        String strMessage;
        int offset;
        List<DhtMeasure> dhtMeasures = new ArrayList<>(dhtReads);

        for(int readCount = 0; readCount < dhtReads; readCount++) {
            offset = readCount * dhtReadLength;
            strMessage = new String(dhtData, offset, dhtReadLength, StandardCharsets.UTF_8);
            int timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
            double temperature = Double.parseDouble(strMessage.substring(timestampLength,
                    timestampLength + temperatureLength));
            double humidity = Double.parseDouble(strMessage.substring(timestampLength + temperatureLength,
                    timestampLength + temperatureLength + humidityLength));
            DhtMeasure dhtMeasure = new DhtMeasure(timestamp, temperature, humidity);
            dhtMeasures.add(dhtMeasure);
        }

        return dhtMeasures;
    }

    private List<PmMeasure> parsePmData(byte[] pmData, int pmReads, int pmReadLength, int timestampLength,
                                        int pmValueLength, int sensorIdLength) {
        String strMessage;
        int offset;
        List<PmMeasure> pmMeasures = new ArrayList<>(pmReads);

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
            PmMeasure pmMeasure = new PmMeasure(timestamp, sensorId25, pm25, sensorId10, pm10);
            pmMeasures.add(pmMeasure);
        }

        return pmMeasures;
    }
}
