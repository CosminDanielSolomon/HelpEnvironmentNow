package it.polito.helpenvironmentnow.Storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface StoredJsonDao {
    @Insert
    public void insertJson(StoredJson storedJson);

    @Delete
    public void deleteJson(StoredJson storedJson);

    @Query("SELECT * FROM StoredJson")
    public StoredJson[] getAllStoredJson();
}
