package it.polito.helpenvironmentnow.Helper;

public class MatchedPmMeasure extends PmMeasure {
    private String geohash;
    private double altitude;

    public MatchedPmMeasure() {}

    public MatchedPmMeasure(int timestamp, int sensorId25, int pm25, int sensorId10,
                            int pm10, String geohash, double altitude) {
        super(timestamp, sensorId25, pm25, sensorId10, pm10);
        this.geohash = geohash;
        this.altitude = altitude;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}
