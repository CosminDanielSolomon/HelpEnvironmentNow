package it.polito.helpenvironmentnow.Storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeasuresDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMeasures(List<Measure> measures);

    @Delete
    void deleteMeasures(List<Measure> measures);

    @Query("SELECT COUNT(*) FROM Measure")
    long getTotalMeasures();

    // In this case I limit the number of records to 30000 because when data are extracted from
    // the database, internally Sqlite uses a Cursor Window which has a limited size of 2 MB. It
    // is important to not get more than 2MB with a single query. In this case a single record is
    // less than 50 bytes, so extracting 30000 records it's ok
    @Query("SELECT * FROM Measure LIMIT 30000")
    List<Measure> getSomeMeasures();
}
