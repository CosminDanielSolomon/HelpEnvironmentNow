package it.polito.helpenvironmentnow.Helper;

import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

// This class is used to associate a geographical position to the measures received from the Raspberry Pi
public class Matcher {
    private final double defaultAltitude = 0.0;
    private final double defaultLatitude = 0.0;
    private final double defaultLongitude = 0.0;

    // This method receives the list of measures that need to be associated to a location.
    // If this method is called by the CLASSIC service the first parameter is NOT null, so all the
    // measures will be associated to that position.
    public void matchMeasuresAndPositions(Location location, List<Measure> parsedMeasures) {

        if(location != null) {
            // CLASSIC MODE. I put the received location for all the measures received
            // from the Raspberry Pi

            String geoHash = LocationInfo.encodeLocation(location);
            double altitude = location.getAltitude();
            for(Measure measure : parsedMeasures) {
                measure.geoHash = geoHash;
                measure.altitude = altitude;
            }
        }

    }
}
