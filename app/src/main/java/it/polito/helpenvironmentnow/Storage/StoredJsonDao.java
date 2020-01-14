package it.polito.helpenvironmentnow.Storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface StoredJsonDao {
    @Insert
    void insertJson(StoredJson storedJson);

    @Delete
    void deleteJson(StoredJson storedJson);

    @Query("SELECT * FROM StoredJson")
    StoredJson[] getAllStoredJson();
}
