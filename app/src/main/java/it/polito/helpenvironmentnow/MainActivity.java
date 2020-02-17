package it.polito.helpenvironmentnow;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import it.polito.helpenvironmentnow.Helper.BtDevice;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity"; // This string is used as tag for debug
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private RelativeLayout movementRelativeLayout;
    private ProgressBar pbTop;
    private Switch switchMovementMode;
    private TextView tvMovementModeContent;
    private Button btnConfig;

    private boolean scanning = false;
    private List<BtDevice> scanningResult = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private SharedPreferences sharedPref;
    private LocationRequest locationRequest; // This field is used for the MOVEMENT mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movementRelativeLayout = findViewById(R.id.movementRelativeLayout);
        pbTop = findViewById(R.id.progressBarMovement);
        tvMovementModeContent = findViewById(R.id.tvMovementModeContent);
        sharedPref = getSharedPreferences(getString(R.string.config_file), Context.MODE_PRIVATE);
        boolean movementMode = sharedPref.getBoolean(getString(R.string.MODE), false);
        switchMovementMode = findViewById(R.id.switchMovementMode);
        initMovementLayout(movementMode);
        switchMovementMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        switchMovementMode.setChecked(false);
                        showPermissionSnackbar();
                    } else {
                        // Check if the device has enabled the needed geolocation settings and start
                        // LocationService if the user has the necessary settings
                        checkDeviceSettings();
                    }
                } else {
                    // Check if the device has enabled the needed geolocation settings and start
                    // LocationService if the user has the necessary settings
                    checkDeviceSettings();
                }


            } else {
                stopMovementService();
            }
            }
        });

        /* TODO remove this part(START) and the corresponding button from activity_main.xml */
        btnConfig = findViewById(R.id.buttonConfig);
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                startActivity(intent);
            }
        });
        /* TODO remove this part(END) */

        /* TODO remove this part(START) and the corresponding button from activity_main.xml */
        Button btnConnect = findViewById(R.id.buttonConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if(sharedPref.getBoolean(getString(R.string.MODE), false))
                    intent = new Intent(getApplicationContext(), MovementService.class);
                else
                    intent = new Intent(getApplicationContext(), StaticService.class);
                intent.putExtra("remoteMacAddress", "B8:27:EB:47:CF:BE");
                ContextCompat.startForegroundService(getApplicationContext(), intent);
                Log.d(TAG, "startService(...) performed");
            }
        });
        /* TODO remove this part(END) */

        // Check permissions
        requestFineLocationPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    private void requestFineLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons from Raspberry Pi around the city.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {

                            showPermissionSnackbar();

                        }

                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // This is invoked as a consequence of the call to "checkDeviceSettings()" if the necessary
        // geolocation settings are not enabled and onFailure is called
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_CANCELED) {
                switchMovementMode.setChecked(false);
                showSettingsSnackbar();
            } else if (resultCode == RESULT_OK) {
                checkBluetoothEnabled();
            }
        }
        if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startScanning();
            } else if(resultCode == RESULT_CANCELED) {
                switchMovementMode.setChecked(false);
                showBluetoothSnackbar();
            }
        }
    }

    private void checkBluetoothEnabled() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                startScanning();
            }
        }
    }

    private void startScanning() {
        tvMovementModeContent.setText(R.string.search_dev);
        pbTop.setVisibility(View.VISIBLE);
        scanning = true;
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        // start scanning Bluetooth devices
        boolean started = bluetoothAdapter.startDiscovery();
        if(!started) { // Bluetooth is not enabled
            switchMovementMode.setChecked(false);
            showBluetoothSnackbar();
        }
    }

    private void stopScanning() {
        if(scanning) {
            unregisterReceiver(receiver);
            if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            tvMovementModeContent.setText(R.string.movement_mode_body);
            pbTop.setVisibility(View.GONE);
            scanning = false;
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevic object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BtDevice btDevice = new BtDevice(deviceName, deviceHardwareAddress);
                if(!scanningResult.contains(btDevice))
                    scanningResult.add(btDevice);
                else
                    Log.d(TAG, "DUPLICATE");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopScanning();
                for (BtDevice b : scanningResult)
                    Log.d(TAG, "name: " + b.getName() + " address: " + b.getAddress());
                /*for(int i = 0; i<20; i++) {
                    scanningResult.add(new BtDevice("dv", "a"+i));
                }*/
                createDialog(scanningResult);
            }
        }
    };

    private void createDialog(final List<BtDevice> devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(devices.size() > 0) {
            final CharSequence[] items = new CharSequence[devices.size()];
            int index = 0;
            for (BtDevice btDevice : devices) {
                items[index] = btDevice.getName() + " (" + btDevice.getAddress() + ")";
                index++;
            }
            builder.setTitle("Select Raspberry Pi device")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        Log.d(TAG, "index: " + which + " Address: " + devices.get(which).getAddress());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopScanning();
                        switchMovementMode.setChecked(false);
                    }
                });
        } else {
            builder.setTitle("No device found!").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopScanning();
                    switchMovementMode.setChecked(false);
                }
            });
        }
        AlertDialog dialog = builder.setCancelable(false).create();
        ListView listView = dialog.getListView();
        listView.setDivider(new ColorDrawable(Color.GRAY));
        listView.setDividerHeight(1);
        dialog.show();
    }

    private void startMovementService() {
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        intent.putExtra("request", locationRequest);
        ContextCompat.startForegroundService(getApplicationContext(), intent);
        saveMovementStatus(true);
        setMovementRelativeLayoutBackground(true);
    }

    private void stopMovementService() {
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        stopService(intent);
        // TODO stop bluetooth discovery if running
        saveMovementStatus(false);
        setMovementRelativeLayoutBackground(false);

        stopScanning();
    }

    private void saveMovementStatus(boolean movementMode) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.MODE), movementMode);
        editor.commit();
    }

    private void initMovementLayout(boolean movementMode) {
        setMovementRelativeLayoutBackground(movementMode);
        switchMovementMode.setChecked(movementMode);
    }

    // Sets the background colour of the Movement mode layout
    private void setMovementRelativeLayoutBackground(boolean state) {
        if(state)
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#068DE5"));
        else
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
    }

    // Show a snackbar that inform the user that the location permission has not been granted
    private void showPermissionSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Location permission NOT granted!", Snackbar.LENGTH_INDEFINITE)
                .setAction("GRANT IT", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestFineLocationPermission();
                    }
                });
        permissionSnackbar.show();
    }

    // Show a snackbar that inform the user that the settings for continuous tracking are not set
    private void showSettingsSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Location settings NOT set!", Snackbar.LENGTH_LONG);
        permissionSnackbar.show();
    }

    // Show a snackbar that inform the user that the settings for continuous tracking are not set
    private void showBluetoothSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Bluetooth is not enabled!", Snackbar.LENGTH_LONG);
        permissionSnackbar.show();
    }

    // Check if the device has enabled the needed geolocation settings otherwise a dialog is
    // started asking the user to accept the necessary geolocation settings for continuous tracking
    private void checkDeviceSettings() {
        // Set the location request in order to receive continuous location updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(5000);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> task = client.checkLocationSettings(settingsRequest);

        // onFailure will be invoked if the device settings do not match the LocationRequest and
        // a dialog is presented to the user to accept the necessary settings
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // Location settings are not satisfied, but this can
                    // be fixed by showing the user a dialog
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the
                        // result in onActivityResult()
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                    }
                }
            }
        });

        // onSuccess is invoked if the device settings are ok
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                checkBluetoothEnabled();
            }
        });
    }
}
