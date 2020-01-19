package it.polito.helpenvironmentnow.Storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PositionsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPositions(List<Position> positions);

    @Query("SELECT * FROM Position WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    Position[] selectPositions(int startTimestamp, int endTimestamp);

    @Query("DELETE FROM Position WHERE timestamp <= :currentTimestamp")
    void deletePositions(int currentTimestamp);
}
