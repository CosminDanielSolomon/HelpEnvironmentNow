package it.polito.helpenvironmentnow.Helper;

public class PmMetaData {
    private int numberOfReads;
    private int readLength;
    private int timestampLength;
    private int pmValueLength;
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

    public int getPmValueLength() {
        return pmValueLength;
    }

    public void setPmValueLength(int pmValueLength) {
        this.pmValueLength = pmValueLength;
    }

    public int getSensorIdLength() {
        return sensorIdLength;
    }

    public void setSensorIdLength(int sensorIdLength) {
        this.sensorIdLength = sensorIdLength;
    }
}
