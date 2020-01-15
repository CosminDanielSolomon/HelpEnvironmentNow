package it.polito.helpenvironmentnow.Helper;

import java.util.List;

public class ParsedData {
    private int sensorIdTemperature;
    private int sensorIdHumidity;
    private List<DhtMeasure> dhtMeasures;
    private List<PmMeasure> pmMeasures;

    public ParsedData() {}

    public ParsedData(int sensorIdTemperature, int sensorIdHumidity, List<DhtMeasure> dhtMeasures,
                      List<PmMeasure> pmMeasures) {
        this.sensorIdTemperature = sensorIdTemperature;
        this.sensorIdHumidity = sensorIdHumidity;
        this.dhtMeasures = dhtMeasures;
        this.pmMeasures = pmMeasures;
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

    public List<DhtMeasure> getDhtMeasures() {
        return dhtMeasures;
    }

    public void setDhtMeasures(List<DhtMeasure> dhtMeasures) {
        this.dhtMeasures = dhtMeasures;
    }

    public List<PmMeasure> getPmMeasures() {
        return pmMeasures;
    }

    public void setPmMeasures(List<PmMeasure> pmMeasures) {
        this.pmMeasures = pmMeasures;
    }
}
