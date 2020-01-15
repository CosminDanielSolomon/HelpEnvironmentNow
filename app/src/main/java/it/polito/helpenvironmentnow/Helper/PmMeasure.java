package it.polito.helpenvironmentnow.Helper;

public class PmMeasure {
    private int timestamp;
    private int sensorId25;
    private int pm25;
    private int sensorId10;
    private int pm10;

    public PmMeasure() {}

    public PmMeasure(int timestamp, int sensorId25, int pm25, int sensorId10, int pm10) {
        this.timestamp = timestamp;
        this.sensorId25 = sensorId25;
        this.pm25 = pm25;
        this.sensorId10 = sensorId10;
        this.pm10 = pm10;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getSensorId25() {
        return sensorId25;
    }

    public void setSensorId25(int sensorId25) {
        this.sensorId25 = sensorId25;
    }

    public int getPm25() {
        return pm25;
    }

    public void setPm25(int pm25) {
        this.pm25 = pm25;
    }

    public int getSensorId10() {
        return sensorId10;
    }

    public void setSensorId10(int sensorId10) {
        this.sensorId10 = sensorId10;
    }

    public int getPm10() {
        return pm10;
    }

    public void setPm10(int pm10) {
        this.pm10 = pm10;
    }
}
