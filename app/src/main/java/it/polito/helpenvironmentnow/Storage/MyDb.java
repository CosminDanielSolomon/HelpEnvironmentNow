package it.polito.helpenvironmentnow.Storage;

import android.content.Context;

import androidx.room.Room;

import java.util.List;

public class MyDb {
    private static final String LOCAL_DB_NAME = "DB_HELP_ENVIRONMENT";
    private LocalStore db;
    private MeasuresDao measuresDao; // used to access the Measure table of the database

    public MyDb(Context context) {
        db = Room.databaseBuilder(context, LocalStore.class, LOCAL_DB_NAME).build();
        measuresDao = db.measuresDao();
    }

    public void insertMeasures(List<Measure> measures) {
        measuresDao.insertMeasures(measures);
    }

    public void deleteMeasures(List<Measure> measures) {
        measuresDao.deleteMeasures(measures);
    }

    public long getTotalMeasures() {
        return measuresDao.getTotalMeasures();
    }

    public List<Measure> getSomeMeasures() {
        return measuresDao.getSomeMeasures();
    }

    public void closeDb() {
        db.close();
    }


}
