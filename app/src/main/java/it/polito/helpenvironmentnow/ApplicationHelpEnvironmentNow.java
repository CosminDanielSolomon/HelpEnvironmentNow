package it.polito.helpenvironmentnow;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;

import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.concurrent.Executors;

// To better understand the callbacks of this class search for "Android beacon library background detection"
// Useful link: https://altbeacon.github.io/android-beacon-library/background_launching.html
public class ApplicationHelpEnvironmentNow extends Application implements BootstrapNotifier, RangeNotifier, Configuration.Provider {

    private static final String TAG = "AppHelpNow"; // This string is used as tag for debug
    private static final String namespaceId = "0xa8844da2d40a11e9bb65";
    private static final String instanceId = "0x2a2ae2dbcce4";
    private RegionBootstrap regionBootstrap;
    private BeaconManager beaconManager;
    private Region region;
    private Identifier myBeaconNamespaceId, myBeaconInstanceId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.setRegionStatePersistenceEnabled(false);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // wake up the app when any beacon is seen (you can specify specific id filters in the parameters below)
        myBeaconNamespaceId = Identifier.parse(namespaceId);
        myBeaconInstanceId = Identifier.parse(instanceId);
        region = new Region("it.polito.helpenvironmentnow.boostrapRegion", myBeaconNamespaceId, myBeaconInstanceId, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    @Override
    public void didDetermineStateForRegion(int state, Region arg1) {
        if(state == INSIDE) {
            try {
                beaconManager.addRangeNotifier(this);
                beaconManager.startRangingBeaconsInRegion(region);
                Log.d(TAG, "start RANGING beacons in region");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Got a didEnterRegion call");
        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.
        //regionBootstrap.disable();
    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.d(TAG, "didExitRegion(...) called");
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if(beacons.size() > 0) {
            for (Beacon beacon : beacons) {
                if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                    // This is a Eddystone-UID frame
                    Identifier detectedNamespaceId = beacon.getId1();
                    Identifier detectedInstanceId = beacon.getId2();
                    if(myBeaconNamespaceId.equals(detectedNamespaceId) && myBeaconInstanceId.equals(detectedInstanceId)) {
                        String remoteMacAddress = beacon.getBluetoothAddress();
                        if(remoteMacAddress != null) {
                            Intent intent = new Intent(getApplicationContext(), ClassicService.class);
                            intent.putExtra("remoteMacAddress", remoteMacAddress);
                            ContextCompat.startForegroundService(this, intent);
                            break;
                        }
                    }
                }
            }
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setExecutor(Executors.newSingleThreadExecutor()).build();
    }
}
