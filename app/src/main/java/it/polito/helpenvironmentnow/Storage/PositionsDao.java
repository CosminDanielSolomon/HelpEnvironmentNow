package it.polito.helpenvironmentnow.Storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PositionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPosition(Position position);

    @Query("SELECT * FROM Position WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    Position[] selectPositions(int startTimestamp, int endTimestamp);

    @Query("DELETE FROM Position WHERE timestamp <= :currentTimestamp")
    void deletePositions(int currentTimestamp);
}
