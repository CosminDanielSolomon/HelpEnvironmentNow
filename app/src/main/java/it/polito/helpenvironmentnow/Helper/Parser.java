package it.polito.helpenvironmentnow.Helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import it.polito.helpenvironmentnow.Storage.Measure;

// This class is used to parse the stream of bytes received from the Raspberry Pi
// The methods of this class are called inside the methods of the RaspberryPi class
public class Parser {

    private static final int DHT_NUMBER_OF_READS_CHARS = 8; // the number of chars used to represent the length(in bytes) of number of dht reads
    private static final int DHT_READ_LENGTH_CHARS = 4; // the number of chars used to represent the length(in bytes) of a message
    private static final int SENSOR_ID_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of sensor id (Sensor_SN)
    private static final int TIMESTAMP_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of timestamp
    private static final int TEMPERATURE_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of temperature
    private static final int HUMIDITY_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of humidity
    public static final int DHT_META_DATA_CHARS = DHT_NUMBER_OF_READS_CHARS + DHT_READ_LENGTH_CHARS + SENSOR_ID_LENGTH_CHARS +
            TIMESTAMP_LENGTH_CHARS + TEMPERATURE_LENGTH_CHARS + HUMIDITY_LENGTH_CHARS;

    private static final int PM_NUMBER_OF_READS_CHARS = 8; // the number of chars used to represent the length(in bytes) of number of pm reads
    private static final int PM_READ_LENGTH_CHARS = 4; // the number of chars used to represent the length(in bytes) of a pm read
    private static final int PM_VALUE_LENGTH_CHARS = 1; // the number of chars used to represent the length(in bytes) of pm measure(both pm2.5 and pm10)
    public static final int PM_META_DATA_CHARS = PM_NUMBER_OF_READS_CHARS + PM_READ_LENGTH_CHARS +
            TIMESTAMP_LENGTH_CHARS + PM_VALUE_LENGTH_CHARS + SENSOR_ID_LENGTH_CHARS;

    public DhtMetaData parseDhtMetaData(String strMetaData) {
        int bIndex = 0, eIndex = DHT_NUMBER_OF_READS_CHARS;
        DhtMetaData dhtMetaData = new DhtMetaData();
        dhtMetaData.setNumberOfReads(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += DHT_READ_LENGTH_CHARS;
        dhtMetaData.setReadLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += SENSOR_ID_LENGTH_CHARS;
        dhtMetaData.setSensorIdLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TIMESTAMP_LENGTH_CHARS;
        dhtMetaData.setTimestampLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TEMPERATURE_LENGTH_CHARS;
        dhtMetaData.setTemperatureLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += HUMIDITY_LENGTH_CHARS;
        dhtMetaData.setHumidityLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));

        return dhtMetaData;
    }

    public PmMetaData parsePmMetaData(String strMetaData) {
        int bIndex = 0, eIndex = PM_NUMBER_OF_READS_CHARS;
        PmMetaData pmMetaData = new PmMetaData();
        pmMetaData.setNumberOfReads(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += PM_READ_LENGTH_CHARS;
        pmMetaData.setReadLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TIMESTAMP_LENGTH_CHARS;
        pmMetaData.setTimestampLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += PM_VALUE_LENGTH_CHARS;
        pmMetaData.setPmValueLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += SENSOR_ID_LENGTH_CHARS;
        pmMetaData.setSensorIdLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));

        return pmMetaData;
    }

    public int parseSensorIdTemperature(byte[] fixedDhtData, int sensorIdLength) {
        String strTempId = new String(fixedDhtData, 0, sensorIdLength, StandardCharsets.UTF_8);
        return Integer.parseInt(strTempId);
    }

    public int parseSensorIdHumidity(byte[] fixedDhtData, int sensorIdLength) {
        String strHumId = new String(fixedDhtData, sensorIdLength, sensorIdLength, StandardCharsets.UTF_8);
        return Integer.parseInt(strHumId);
    }

    public List<Measure> parseDhtData(int sensorIdTemp, int sensorIdHum, byte[] dhtData,
                                      int dhtReads, DhtMetaData dhtMetaData) {
        int dhtReadLength = dhtMetaData.getReadLength();
        int timestampLength = dhtMetaData.getTimestampLength();
        int temperatureLength = dhtMetaData.getTemperatureLength();
        int humidityLength = dhtMetaData.getHumidityLength();

        String strMessage;
        int offset;
        List<Measure> dhtMeasures = new ArrayList<>(dhtReads * 2);

        for(int readCount = 0; readCount < dhtReads; readCount++) {
            offset = readCount * dhtReadLength;
            strMessage = new String(dhtData, offset, dhtReadLength, StandardCharsets.UTF_8);
            int timestamp = Integer.parseInt(strMessage.substring(0, timestampLength));
            double temperature = Double.parseDouble(strMessage.substring(timestampLength,
                    timestampLength + temperatureLength));
            double humidity = Double.parseDouble(strMessage.substring(timestampLength + temperatureLength,
                    timestampLength + temperatureLength + humidityLength));

            Measure measureT = new Measure();
            measureT.sensorId = sensorIdTemp;
            measureT.timestamp = timestamp;
            measureT.data = temperature;
            Measure measureH = new Measure();
            measureH.sensorId = sensorIdHum;
            measureH.timestamp = timestamp;
            measureH.data = humidity;
            dhtMeasures.add(measureT);
            dhtMeasures.add(measureH);
        }

        return dhtMeasures;
    }

    public List<Measure> parsePmData(byte[] pmData, int pmReads, PmMetaData pmMetaData) {
        int pmReadLength = pmMetaData.getReadLength();
        int timestampLength = pmMetaData.getTimestampLength();
        int pmValueLength = pmMetaData.getPmValueLength();
        int sensorIdLength = pmMetaData.getSensorIdLength();

        String strMessage;
        int offset;
        List<Measure> pmMeasures = new ArrayList<>(pmReads * 2);

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

            Measure measurePm10 = new Measure();
            measurePm10.sensorId = sensorId10;
            measurePm10.timestamp = timestamp;
            measurePm10.data = pm10;
            Measure measurePm25 = new Measure();
            measurePm25.sensorId = sensorId25;
            measurePm25.timestamp = timestamp;
            measurePm25.data = pm25;
            pmMeasures.add(measurePm10);
            pmMeasures.add(measurePm25);
        }

        return pmMeasures;
    }
}
