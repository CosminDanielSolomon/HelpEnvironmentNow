package it.polito.helpenvironmentnow.Storage;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Position {
    @PrimaryKey
    public int timestamp;

    public double latitude;
    public double longitude;
    public double altitude;
}
