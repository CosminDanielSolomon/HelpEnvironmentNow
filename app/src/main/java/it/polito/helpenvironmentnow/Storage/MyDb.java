package it.polito.helpenvironmentnow.Storage;

import android.content.Context;

import androidx.room.Room;

import org.json.JSONObject;

import java.util.List;

public class MyDb {
    private static final String LOCAL_DB_NAME = "DB_HELP_ENVIRONMENT";
    private LocalStore db;
    private MeasuresDao measuresDao; // used to access the Measure table of the database
    private PositionsDao positionsDao; // used to access the Position table saved into database

    public MyDb(Context context) {
        db = Room.databaseBuilder(context, LocalStore.class, LOCAL_DB_NAME).build();
        measuresDao = db.measuresDao();
        positionsDao = db.positionsDao();
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

    public void insertPosition(int t, double lat, double lon, double alt) {
        Position currentPosition = new Position();
        currentPosition.timestamp = t;
        currentPosition.latitude = lat;
        currentPosition.longitude = lon;
        currentPosition.altitude = alt;
        positionsDao.insertPosition(currentPosition);
    }

    public Position[] selectPositions(int startT, int endT) {
        return positionsDao.selectPositions(startT, endT);
    }

    public void deletePositions(int endT) {
        positionsDao.deletePositions(endT);
    }

    public void closeDb() {
        db.close();
    }


}
