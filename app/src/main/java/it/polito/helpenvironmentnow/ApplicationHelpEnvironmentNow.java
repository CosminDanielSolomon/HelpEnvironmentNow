package it.polito.helpenvironmentnow;

import android.app.Application;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
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

public class ApplicationHelpEnvironmentNow extends Application implements BootstrapNotifier, RangeNotifier {
    private static final String TAG = "AppHelpNow";
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
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.setRegionStatePersistenceEnabled(false);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
        myBeaconNamespaceId = Identifier.parse(namespaceId);
        myBeaconInstanceId = Identifier.parse(instanceId);
        region = new Region("com.example.myapp.boostrapRegion", myBeaconNamespaceId, myBeaconInstanceId, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    @Override
    public void didDetermineStateForRegion(int state, Region arg1) {
        Log.d(TAG, "didDetermineStateForRegion(...) called");
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.addRangeNotifier(this);
            Log.d(TAG, "didDetermineStateForRegion try executed");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if(state == INSIDE) {
            Log.d(TAG, "didDetermineStateForRegion(...) INSIDE");

        } else {
            Log.d(TAG, "didDetermineStateForRegion(...) OUTSIDE");
        }
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Got a didEnterRegion call");
        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.
        //regionBootstrap.disable();

        /*Intent intent = new Intent(this, RfcommSendService.class);
        Log.d(TAG, "remoteMacAddress:" + remoteMacAddress);
        intent.putExtra("remoteMacAddress", remoteMacAddress);
        ContextCompat.startForegroundService(this, intent);
        Log.d(TAG, "startService(...) performed");*/
        Intent intent = new Intent(this, MainActivity.class);
        // IMPORTANT: in the AndroidManifest.xml definition of this activity, you must set android:launchMode="singleInstance" or you will get two instances
        // created when a user launches the activity manually and it gets launched from here.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.d(TAG, "didEnterRegion Launching activity");
        this.startActivity(intent);
    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.d(TAG, "didExitRegion(...) called");
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "didRangeBeaconsInRegion(...) called");
        if(beacons.size() > 0) {
            Log.d(TAG, "didRangeBeaconsInRegion(...) enter in for loop size >0");
            for (Beacon beacon : beacons) {
                if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                    // This is a Eddystone-UID frame
                    Identifier detectedNamespaceId = beacon.getId1();
                    Identifier detectedInstanceId = beacon.getId2();
                    if(myBeaconNamespaceId.equals(detectedNamespaceId) && myBeaconInstanceId.equals(detectedInstanceId)) {
                        String remoteMacAddress = beacon.getBluetoothAddress();
                        if(remoteMacAddress != null) {
                            Intent intent = new Intent(this, RfcommSendService.class);
                            Log.d(TAG, "remoteMacAddress:" + remoteMacAddress + " " + detectedNamespaceId.toString() + detectedInstanceId.toString());
                            intent.putExtra("remoteMacAddress", remoteMacAddress);
                            ContextCompat.startForegroundService(this, intent);
                            Log.d(TAG, "startService(...) performed");
                            break;
                        }
                    }
                }
            }
        }
    }
}