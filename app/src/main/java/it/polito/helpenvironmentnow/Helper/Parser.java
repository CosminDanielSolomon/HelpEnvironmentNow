package it.polito.helpenvironmentnow.Helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// This class is used to parse the stream of bytes received from the Raspberry Pi
public class Parser {

    public int parseSensorIdTemperature(byte[] fixedDhtData, int sensorIdLength) {
        String strTempId = new String(fixedDhtData, 0, sensorIdLength, StandardCharsets.UTF_8);
        return Integer.parseInt(strTempId);
    }

    public int parseSensorIdHumidity(byte[] fixedDhtData, int sensorIdLength) {
        String strHumId = new String(fixedDhtData, sensorIdLength, sensorIdLength, StandardCharsets.UTF_8);
        return Integer.parseInt(strHumId);
    }

    public List<DhtMeasure> parseDhtData(byte[] dhtData, int dhtReads, DhtMetaData dhtMetaData) {
        int dhtReadLength = dhtMetaData.getReadLength();
        int timestampLength = dhtMetaData.getTimestampLength();
        int temperatureLength = dhtMetaData.getTemperatureLength();
        int humidityLength = dhtMetaData.getHumidityLength();

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

    public List<PmMeasure> parsePmData(byte[] pmData, int pmReads, PmMetaData pmMetaData) {
        int pmReadLength = pmMetaData.getReadLength();
        int timestampLength = pmMetaData.getTimestampLength();
        int pmValueLength = pmMetaData.getPmValueLength();
        int sensorIdLength = pmMetaData.getSensorIdLength();

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
