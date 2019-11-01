package it.polito.helpenvironmentnow.Helper;

public class DhtMetaData {
    private int numberOfReads;
    private int readLength;
    private int timestampLength;
    private int temperatureLength;
    private int humidityLength;
    private int sensorIdLength;

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(int numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public int getReadLength() {
        return readLength;
    }

    public void setReadLength(int readLength) {
        this.readLength = readLength;
    }

    public int getTimestampLength() {
        return timestampLength;
    }

    public void setTimestampLength(int timestampLength) {
        this.timestampLength = timestampLength;
    }

    public int getTemperatureLength() {
        return temperatureLength;
    }

    public void setTemperatureLength(int temperatureLength) {
        this.temperatureLength = temperatureLength;
    }

    public int getHumidityLength() {
        return humidityLength;
    }

    public void setHumidityLength(int humidityLength) {
        this.humidityLength = humidityLength;
    }

    public int getSensorIdLength() {
        return sensorIdLength;
    }

    public void setSensorIdLength(int sensorIdLength) {
        this.sensorIdLength = sensorIdLength;
    }
}
