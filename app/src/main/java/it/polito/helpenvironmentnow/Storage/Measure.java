package it.polito.helpenvironmentnow.Storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(primaryKeys = {"timestamp", "sensorId", "data", "geoHash" , "altitude"})
public class Measure {
    public int timestamp;
    public int sensorId;
    public double data;
    @NonNull
    public String geoHash;
    public double altitude;
}
