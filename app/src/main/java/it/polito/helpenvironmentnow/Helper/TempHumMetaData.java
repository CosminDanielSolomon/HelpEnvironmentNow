package it.polito.helpenvironmentnow.Helper;

public class TempHumMetaData {
    private int numberOfMessages;
    private int messageLength;
    private int timestampLength;
    private int temperatureLength;
    private int humidityLength;
    private int sensorIdLength;

    public int getNumberOfMessages() {
        return numberOfMessages;
    }

    public void setNumberOfMessages(int numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
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
