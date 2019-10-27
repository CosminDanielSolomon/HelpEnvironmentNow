package it.polito.helpenvironmentnow.Caching;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface CachedJsonDao {
    @Insert
    public void insertJson(CachedJson cachedJson);

    @Delete
    public void deleteJson(CachedJson cachedJson);

    @Query("SELECT * FROM CachedJson")
    public CachedJson[] getAllCachedJson();
}
