package it.polito.helpenvironmentnow.Caching;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {CachedJson.class}, version = 1)
public abstract class LocalDatabase extends RoomDatabase {
    public abstract CachedJsonDao cachedJsonDao();
}
