package it.polito.helpenvironmentnow.Storage;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Measure.class}, version = 1)
public abstract class LocalStore extends RoomDatabase {
    public abstract MeasuresDao measuresDao();
}
