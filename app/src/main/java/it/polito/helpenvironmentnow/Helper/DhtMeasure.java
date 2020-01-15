package it.polito.helpenvironmentnow.Helper;

public class DhtMeasure implements Comparable<DhtMeasure> {
    private int timestamp;
    private double temperature;
    private double humidity;

    public DhtMeasure() {}

    public DhtMeasure(int timestamp, double temperature, double humidity) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    @Override
    public int compareTo(DhtMeasure o) {
        if(timestamp < o.timestamp)
            return -1;
        if(timestamp > o.timestamp)
            return 1;
        return 0;
    }
}
