package it.polito.helpenvironmentnow.Helper;

public class MatchedDhtMeasure extends DhtMeasure {

    private String geohash;
    private double altitude;

    public MatchedDhtMeasure() {}

    public MatchedDhtMeasure(int timestamp, double temperature, double humidity, String geohash, double altitude) {
        super(timestamp, temperature, humidity);
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
