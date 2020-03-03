package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import java.util.List;

import it.polito.helpenvironmentnow.Storage.Measure;

// This class is used to associate a geographical position to the measures received from the Raspberry Pi
public class Matcher {
    // This method receives the list of measures that need to be associated to a location.
    // The "location" parameter contains the position, so all the measures will be associated to that position.
    public void matchMeasuresAndPositions(Location location, List<Measure> parsedMeasures) {

        if(location != null) {
            String geoHash = LocationInfo.encodeLocation(location);
            double altitude = location.getAltitude();
            for(Measure measure : parsedMeasures) {
                measure.geoHash = geoHash;
                measure.altitude = altitude;
            }
        }

    }
}
