package it.polito.helpenvironmentnow.Helper;

import java.util.List;

public class MatchedData {
    private int sensorIdTemperature;
    private int sensorIdHumidity;
    private List<MatchedDhtMeasure> matchedDhtMeasures;
    private List<MatchedPmMeasure> matchedPmMeasures;

    public MatchedData() {}

    public MatchedData(int sensorIdTemperature, int sensorIdHumidity,
                       List<MatchedDhtMeasure> matchedDhtMeasures,
                       List<MatchedPmMeasure> matchedPmMeasures) {
        this.sensorIdTemperature = sensorIdTemperature;
        this.sensorIdHumidity = sensorIdHumidity;
        this.matchedDhtMeasures = matchedDhtMeasures;
        this.matchedPmMeasures = matchedPmMeasures;
    }

    public int getSensorIdTemperature() {
        return sensorIdTemperature;
    }

    public void setSensorIdTemperature(int sensorIdTemperature) {
        this.sensorIdTemperature = sensorIdTemperature;
    }

    public int getSensorIdHumidity() {
        return sensorIdHumidity;
    }

    public void setSensorIdHumidity(int sensorIdHumidity) {
        this.sensorIdHumidity = sensorIdHumidity;
    }

    public List<MatchedDhtMeasure> getMatchedDhtMeasures() {
        return matchedDhtMeasures;
    }

    public void setMatchedDhtMeasures(List<MatchedDhtMeasure> matchedDhtMeasures) {
        this.matchedDhtMeasures = matchedDhtMeasures;
    }

    public List<MatchedPmMeasure> getMatchedPmMeasures() {
        return matchedPmMeasures;
    }

    public void setMatchedPmMeasures(List<MatchedPmMeasure> matchedPmMeasures) {
        this.matchedPmMeasures = matchedPmMeasures;
    }
}
